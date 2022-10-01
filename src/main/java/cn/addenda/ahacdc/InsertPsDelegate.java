package cn.addenda.ahacdc;

import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.ec.calculator.CalculatorFactory;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/9/24 9:51
 */
public class InsertPsDelegate extends AbstractPsDelegate {

    /**
     * 这些列必须从数据库里面取。
     */
    private List<String> dependentColumnList;

    /**
     * 这些列需要用表达式计算器计算
     */
    private List<String> calculableColumnList;

    private boolean multipleRows = false;

    public InsertPsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        super(cdcConnection, ps, tableConfig, parameterizedSql);
        if (sqlHelper.checkInsertMultipleRows(parameterizedSql)) {
            multipleRows = true;
        } else {
            BinaryResult<List<String>, List<BinaryResult<String, Curd>>> binaryResult = sqlHelper.divideColumnFromUpdateOrInsertSql(parameterizedSql);
            dependentColumnList = binaryResult.getFirstResult();
            calculableColumnList = binaryResult.getSecondResult().stream().map(BinaryResult::getFirstResult).collect(Collectors.toList());
        }
    }

    @Override
    protected <T> void doAssert(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        assertTxIsolationNotLessThan(Connection.TRANSACTION_READ_COMMITTED);
    }

    @Override
    public <T> T doExecute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        T invoke = pi.invoke();
        if (multipleRows) {
            // 将多行insert语句处理为单行insert语句
            executableSqlList = toSingleRow(executableSqlList);
        }

        BinaryResult<List<String>, List<Long>> sqlWithKeyValueBr = fillKeyValueToInsertSql(executableSqlList);
        executableSqlList = sqlWithKeyValueBr.getFirstResult();
        List<Long> keyValueList = sqlWithKeyValueBr.getSecondResult();

        // -------------------------------------
        //  对于Statement模式来说，记录下来SQL就行了
        // -------------------------------------
        if (checkTableMode(TableConfig.CM_STATEMENT)) {
            List<String> statementCdcSqlList = new ArrayList<>(executableSqlList);
            executeCdcSql(TableConfig.CM_STATEMENT, statementCdcSqlList);
        }

        // ----------------------------------
        //  对于ROW模式，需要记录下来具体插入的值。
        // ----------------------------------
        if (checkTableMode(TableConfig.CM_ROW)) {
            List<String> rowCdcSqlList = new ArrayList<>();
            if (multipleRows) {
                try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                    // insert into A(a, b) values (1,'2'), (3+1,concat('2','3'))
                    // 拆分后：
                    // insert into a(a,b) values (1,'2')
                    // insert into a(a,b) values (3 + 1 ,concat('2','3'))
                    // 所以不可以按相同的 dependentColumnList 和 calculableColumnList 处理
                    List<String> rowDependentColumnList;
                    List<BinaryResult<String, Curd>> rowCalculableColumnBrList;
                    for (int i = 0; i < executableSqlList.size(); i++) {
                        String executableSql = executableSqlList.get(i);
                        Long keyValue = keyValueList.get(i);
                        BinaryResult<List<String>, List<BinaryResult<String, Curd>>> binaryResult = sqlHelper.divideColumnFromUpdateOrInsertSql(executableSql);
                        rowDependentColumnList = binaryResult.getFirstResult();
                        rowCalculableColumnBrList = binaryResult.getSecondResult();
                        Map<String, Token> columnTokenMap = new HashMap<>(8);
                        // 从数据库中获取字段
                        if (!rowDependentColumnList.isEmpty()) {
                            columnTokenMap.putAll(queryColumnTokenMap(statement, keyValue, rowDependentColumnList));
                        }
                        // 表达式计算器计算字段
                        if (!rowCalculableColumnBrList.isEmpty()) {
                            for (BinaryResult<String, Curd> binary : rowCalculableColumnBrList) {
                                Curd value = binary.getSecondResult();
                                Object result = CalculatorFactory.createExpressionCalculator(value, functionCalculator).calculate();
                                columnTokenMap.put(binary.getFirstResult(), dataFormatterRegistry.parse(result));
                            }
                        }
                        if (!columnTokenMap.isEmpty()) {
                            rowCdcSqlList.add(sqlHelper.updateOrInsertUpdateColumnValue(executableSql, columnTokenMap));
                        } else {
                            rowCdcSqlList.add(executableSql);
                        }
                    }
                }
            } else {
                rowCdcSqlList.addAll(executableSqlList);

                // 从数据库里面取数据
                if (!dependentColumnList.isEmpty()) {
                    List<String> tmpSqlList = new ArrayList<>(rowCdcSqlList);
                    rowCdcSqlList.clear();
                    try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                        Map<Long, Map<String, Token>> keyColumnTokenMap = queryKeyColumnTokenMap(statement, keyValueList, dependentColumnList);
                        for (int i = 0; i < keyValueList.size(); i++) {
                            Map<String, Token> columnTokenMap = keyColumnTokenMap.get(keyValueList.get(i));
                            rowCdcSqlList.add(sqlHelper.updateOrInsertUpdateColumnValue(tmpSqlList.get(i), columnTokenMap));
                        }
                    }
                }

                // 表达式计算器计算字段
                if (!calculableColumnList.isEmpty()) {
                    List<String> tmpSqlList = new ArrayList<>(rowCdcSqlList);
                    rowCdcSqlList.clear();
                    for (String sql : tmpSqlList) {
                        rowCdcSqlList.add(sqlHelper.updateOrInsertCalculateColumnValue(sql, calculableColumnList, dataFormatterRegistry));
                    }
                }

            }
            executeCdcSql(TableConfig.CM_ROW, rowCdcSqlList);
        }

        return invoke;
    }

    private List<String> toSingleRow(List<String> executableSqlList) {
        List<String> singleRowList = new ArrayList<>();
        executableSqlList.forEach(item -> singleRowList.addAll(sqlHelper.splitInsertMultipleRows(item)));
        return singleRowList;
    }

    private Map<String, Token> queryColumnTokenMap(Statement statement, Long keyValue, List<String> columnList) throws SQLException {
        return queryKeyColumnTokenMap(statement, Collections.singletonList(keyValue), columnList).get(keyValue);
    }

    /**
     * 处理SQL语句 和 主键值
     *
     * @return firstValue时填充了主键值的sql；secondValue主键集合。
     */
    private BinaryResult<List<String>, List<Long>> fillKeyValueToInsertSql(List<String> executableSqlList) throws SQLException {
        ResultSet generatedKeys = ps.getGeneratedKeys();
        List<Long> keyValueList = new ArrayList<>();
        List<String> executableSqlWithKeyValueList = new ArrayList<>();
        for (String executableSql : executableSqlList) {
            // 如果SQL中存在主键值，取SQL中的值。
            // 如果SQL中不存在主键值，取自增主键的值。
            // 不允许表没有主键。不支持联合主键。
            BigInteger value = sqlHelper.extractColumnValueFromInsertSql(executableSql, keyColumn, BigInteger.class);
            long keyValue;
            if (value == null) {
                if (generatedKeys.next()) {
                    keyValue = generatedKeys.getLong(1);
                    keyValueList.add(keyValue);
                    executableSqlWithKeyValueList.add(sqlHelper.insertInjectColumnValue(executableSql, keyColumn, new Token(TokenType.INTEGER, new BigInteger(String.valueOf(keyValue)))));
                } else {
                    throw new CdcException("Cannot get key column value from sql or generatedKey, key column: " + keyColumn + ". ");
                }
            } else {
                keyValue = value.longValue();
                keyValueList.add(keyValue);
                executableSqlWithKeyValueList.add(executableSql);
            }
        }
        generatedKeys.close();
        return new BinaryResult<>(executableSqlWithKeyValueList, keyValueList);
    }

}
