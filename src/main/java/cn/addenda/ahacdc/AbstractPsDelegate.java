package cn.addenda.ahacdc;

import cn.addenda.ahacdc.format.DataFormatterRegistry;
import cn.addenda.ahacdc.sql.SqlHelper;
import cn.addenda.businesseasy.util.BEListUtil;
import cn.addenda.ec.function.calculator.FunctionCalculator;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.utils.SqlUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/9/24 9:50
 */
public abstract class AbstractPsDelegate implements PsDelegate {

    public static final int IN_SIZE = 2;
    public static final int EXECUTE_INSERT_SQL_BATCH = 10;
    public static final int EXECUTE_INSERT_STATEMENT_BATCH = 20;

    protected final CdcConnection cdcConnection;

    protected final PreparedStatement ps;

    protected final TableConfig tableConfig;

    protected final String parameterizedSql;

    protected final String tableName;

    protected final String keyColumn;

    protected final DataFormatterRegistry dataFormatterRegistry;

    protected final FunctionCalculator functionCalculator;

    protected final SqlHelper sqlHelper;

    protected AbstractPsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        this.cdcConnection = cdcConnection;
        this.ps = ps;
        this.tableConfig = tableConfig;
        this.parameterizedSql = parameterizedSql;
        this.tableName = tableConfig.getTableName();
        this.keyColumn = tableConfig.getKeyColumn();
        this.dataFormatterRegistry = cdcConnection.getCdcDataSource().getDataFormatterRegistry();
        functionCalculator = cdcConnection.getCdcDataSource().getFunctionCalculator();
        sqlHelper = new SqlHelper(functionCalculator);
    }

    @Override
    public <T> T execute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        doAssert(executableSqlList, pi);
        return doExecute(executableSqlList, pi);
    }

    protected abstract <T> void doAssert(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException;

    protected abstract <T> T doExecute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException;

    private static final Map<Integer, String> txIsolationStrMap = new HashMap<>();

    static {
        txIsolationStrMap.put(1, "READ_UNCOMMITTED");
        txIsolationStrMap.put(2, "READ_COMMITTED");
        txIsolationStrMap.put(4, "REPEATABLE_READ");
        txIsolationStrMap.put(8, "SERIALIZABLE");
    }

    protected void assertTxIsolationNotLessThan(int txIsolation) throws SQLException {
        if (cdcConnection.getDelegate().getTransactionIsolation() < txIsolation) {
            throw new CdcException("tx isolation must not less than " + txIsolationStrMap.get(txIsolation) + ". sql: " + parameterizedSql + ". ");
        }
    }

    protected Map<Long, Map<String, Token>> queryKeyColumnTokenMap(
            Statement statement, List<Long> keyValueList, List<String> columnList) throws SQLException {
        Map<Long, Map<String, Token>> map = new HashMap<>();
        if (columnList.isEmpty()) {
            return map;
        }
        List<String> resultColumnList = new ArrayList<>(columnList);
        resultColumnList.add(keyColumn);
        List<List<Long>> listList = BEListUtil.splitList(keyValueList, IN_SIZE);
        for (List<Long> item : listList) {
            int size = item.size();
            String keyInList = longListToString(item);
            String sql = "select "
                    + String.join(",", resultColumnList) + " "
                    + "from " + tableName + " "
                    + "where " + keyColumn + " "
                    + "in (" + keyInList + ")";
            ResultSet resultSet = statement.executeQuery(sql);
            int i = 0;
            while (resultSet.next()) {
                Map<String, Token> columnTokenMap = new HashMap<>();
                Long keyValue = null;
                for (String column : resultColumnList) {
                    if (keyColumn.equals(column)) {
                        keyValue = resultSet.getLong(column);
                        columnTokenMap.put(column, dataFormatterRegistry.parse(keyValue));
                    } else {
                        columnTokenMap.put(column, dataFormatterRegistry.parse(resultSet.getObject(column)));
                    }
                }
                map.put(keyValue, columnTokenMap);
                i++;
            }
            resultSet.close();
            if (size != i) {
                throw new CdcException("Cannot get enough key value from resultSet. keyValueList: " + keyInList + ".");
            }
        }
        return map;
    }

    /**
     * @return executableSql 执行时锁住的key
     */
    protected List<Long> lockKey(Statement statement, String executableSql) throws SQLException {
        List<Long> keyValueList = new ArrayList<>();
        ResultSet resultSet = statement.executeQuery(assembleSelectKeyValueSql(executableSql));
        while (resultSet.next()) {
            keyValueList.add(resultSet.getLong(keyColumn));
        }
        resultSet.close();
        return keyValueList;
    }

    private String assembleSelectKeyValueSql(String executableSql) {
        return "select "
                + keyColumn + " "
                + "from "
                + tableName + " "
                + SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(executableSql, true) + " "
                + "for update";
    }

    protected void executeCdcSql(String cdcMode, List<String> cdcSqlList) throws SQLException {
        if (cdcSqlList.isEmpty()) {
            return;
        }

        List<String> sqlList = new ArrayList<>();
        String sqlPreSegment = "insert into " + tableName + "_cdc_" + cdcMode + "(executable_sql) values";
        List<List<String>> listList = BEListUtil.splitList(cdcSqlList, EXECUTE_INSERT_SQL_BATCH);
        String aloneSql = null;
        for (List<String> item : listList) {
            StringBuilder sql = new StringBuilder(sqlPreSegment);
            for (String string : item) {
                sql.append("(");
                sql.append(sqlHelper.toStorableSql(string));
                sql.append("),");
            }
            if (item.size() == EXECUTE_INSERT_SQL_BATCH) {
                sqlList.add(sql.substring(0, sql.length() - 1));
            } else {
                aloneSql = sql.substring(0, sql.length() - 1);
            }
        }

        Connection connection = cdcConnection.getDelegate();
        try (Statement statement = connection.createStatement()) {
            for (int i = 0; i < sqlList.size(); i++) {
                statement.addBatch(sqlList.get(i));
                if (i != 0 && i % EXECUTE_INSERT_STATEMENT_BATCH == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
        }
        if (aloneSql != null) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(aloneSql);
            }
        }
    }

    protected boolean checkTableMode(String mode) {
        if (tableConfig == null) {
            return false;
        }
        List<String> cdcModeList = tableConfig.getCdcModeList();
        return cdcModeList.contains(mode);
    }

    protected String longListToString(List<Long> longList) {
        return longList.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

}
