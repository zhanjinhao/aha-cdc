package cn.addenda.ahacdc;

import cn.addenda.ahacdc.sql.SqlUtils;
import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.businesseasy.util.BEListUtil;
import cn.addenda.ro.grammar.ast.expression.Curd;
import cn.addenda.ro.grammar.lexical.token.Token;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/9/24 9:52
 */
public class UpdatePsDelegate extends AbstractPsDelegate {

    private final int leastTxIsolation;

    /**
     * 这些列必须从数据库里面取。
     */
    private final List<String> dependentColumnList;

    /**
     * 这些列需要用表达式计算器计算
     */
    private final List<String> calculableColumnList;

    public UpdatePsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        super(cdcConnection, ps, tableConfig, parameterizedSql);
        BinaryResult<List<String>, List<BinaryResult<String, Curd>>> binaryResult = sqlHelper.divideColumnFromUpdateOrInsertSql(parameterizedSql);
        dependentColumnList = binaryResult.getFirstResult();
        calculableColumnList = binaryResult.getSecondResult().stream().map(BinaryResult::getFirstResult).collect(Collectors.toList());
        leastTxIsolation = sqlHelper.analysisLeastTxIsolation(parameterizedSql, keyColumn);
    }

    @Override
    protected <T> void doAssert(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        assertTxIsolationNotLessThan(leastTxIsolation);
        // 对于batch模式，需要是 stable sql
        if (executableSqlList.size() > 1) {
            assertStableUpdateSql(parameterizedSql);
        }
    }

    private void assertStableUpdateSql(String sql) {
        if (!sqlHelper.checkStableUpdateSql(sql, keyColumn)) {
            throw new CdcException("update sql cannot update column which in where-condition and primary key column when sql in batch mode. ");
        }
    }

    @Override
    public <T> T doExecute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {

        // -------------------------------------
        //  对于Statement模式来说，记录下来SQL就行了
        // -------------------------------------
        if (checkTableMode(TableConfig.CM_STATEMENT)) {
            List<String> statementCdcSqlList = new ArrayList<>(executableSqlList);
            executeCdcSql(TableConfig.CM_STATEMENT, statementCdcSqlList);
        }

        // ----------------------------------
        //  对于ROW模式，需要记录下来具体更新的行。
        // ----------------------------------
        if (checkTableMode(TableConfig.CM_ROW)) {
            List<String> rowCdcSqlList = new ArrayList<>();
            try (Statement keyValueStatement = cdcConnection.getDelegate().createStatement()) {
                for (String executableSql : executableSqlList) {
                    // select 获取主键值。
                    List<Long> keyValueList = lockKey(keyValueStatement, executableSql);

                    // 进行 1:n -> 1:1 优化
                    if (dependentColumnList.isEmpty()) {
                        List<List<Long>> listList = BEListUtil.splitList(keyValueList, IN_SIZE);
                        for (List<Long> item : listList) {
                            String rowCdcSql = SqlUtils.replaceDmlWhereSeg(executableSql, "where " + keyColumn + " in (" + longListToString(item) + ")");
                            rowCdcSqlList.add(sqlHelper.updateOrInsertUpdateColumnValue(rowCdcSql, Collections.EMPTY_MAP));
                        }
                    }
                    // 无法进行 1:n -> 1:1 优化
                    else {
                        try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                            Map<Long, Map<String, Token>> keyColumnTokenMap = queryKeyColumnTokenMap(statement, keyValueList, dependentColumnList);
                            for (Long keyValue : keyValueList) {
                                String rowCdcSql = SqlUtils.replaceDmlWhereSeg(executableSql, "where " + keyColumn + " = " + keyValue);
                                Map<String, Token> columnTokenMap = keyColumnTokenMap.get(keyValue);
                                rowCdcSqlList.add(sqlHelper.updateOrInsertUpdateColumnValue(rowCdcSql, columnTokenMap));
                            }
                        }
                    }
                }
            }

            if (!calculableColumnList.isEmpty()) {
                List<String> tmpSqlList = new ArrayList<>(rowCdcSqlList);
                rowCdcSqlList.clear();
                for (String sql : tmpSqlList) {
                    rowCdcSqlList.add(sqlHelper.updateOrInsertCalculateColumnValue(sql, calculableColumnList, dataFormatterRegistry));
                }
            }

            // 1:1 -> n:1 优化
            List<String> tmpSqlList = new ArrayList<>(rowCdcSqlList);
            rowCdcSqlList.clear();
            List<Long> sameUpdateSegKeyValueList = new ArrayList<>();
            String preUpdateSeg = null;
            for (String rowCdcSql : tmpSqlList) {
                BinaryResult<String, List<Long>> binaryResult = sqlHelper.separateUpdateSegAndKeyValues(rowCdcSql);
                String curUpdateSeg = binaryResult.getFirstResult();
                if (preUpdateSeg == null || preUpdateSeg.equals(curUpdateSeg)) {
                    sameUpdateSegKeyValueList.addAll(binaryResult.getSecondResult());
                } else {
                    rowCdcSqlList.addAll(assembleRowUpdateSqlList(preUpdateSeg, sameUpdateSegKeyValueList));
                    sameUpdateSegKeyValueList.clear();
                    sameUpdateSegKeyValueList.addAll(binaryResult.getSecondResult());
                }
                preUpdateSeg = curUpdateSeg;
            }
            rowCdcSqlList.addAll(assembleRowUpdateSqlList(preUpdateSeg, sameUpdateSegKeyValueList));

            executeCdcSql(TableConfig.CM_ROW, rowCdcSqlList);
        }

        return pi.invoke();
    }

    private List<String> assembleRowUpdateSqlList(String updateSeg, List<Long> keyValueList) {
        List<String> rowCdcSqlList = new ArrayList<>();
        List<List<Long>> listList = BEListUtil.splitList(keyValueList, IN_SIZE);
        listList.forEach(item -> rowCdcSqlList.add(updateSeg + " where " + keyColumn + " in (" + longListToString(item) + ")"));
        return rowCdcSqlList;
    }

}
