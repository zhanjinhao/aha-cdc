package cn.addenda.ahacdc;

import cn.addenda.ahacdc.format.DataFormatterRegistry;
import cn.addenda.ec.function.calculator.FunctionCalculator;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author addenda
 * @datetime 2022/8/24 17:03
 */
public class CdcDataSource implements DataSource {

    private final Map<String, TableConfig> tableMetaData = new HashMap<>(4);

    private final DataSource delegate;

    private DataFormatterRegistry dataFormatterRegistry;

    private FunctionCalculator functionCalculator;

    public CdcDataSource(DataSource delegate, DataFormatterRegistry dataFormatterRegistry, FunctionCalculator functionCalculator) {
        this.delegate = delegate;
        this.dataFormatterRegistry = dataFormatterRegistry;
        this.functionCalculator = functionCalculator;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        return new CdcConnection(connection, this);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        return new CdcConnection(connection, this);
    }

    /**
     * 当前对象强转为 iface。
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        }
        return null;
    }

    /**
     * 当前对象是否是 iface 的实例。<br/>
     * 是： true <br/>
     * 否： false
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    protected PrintWriter logWriter = new PrintWriter(System.out);

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setTableMetaData(String tableMetaDataStr) {
        String[] split = tableMetaDataStr.split(";");
        if (split.length == 0) {
            return;
        }
        for (String table : split) {
            StringBuilder tableName = new StringBuilder();
            StringBuilder keyColumn = new StringBuilder();
            List<String> cdcModeList = new ArrayList<>();
            boolean tableNameFg = true;
            for (int i = 0; i < table.length(); i++) {
                char c = table.charAt(i);
                if ('[' == c) {
                    tableNameFg = false;
                    continue;
                }

                if (tableNameFg) {
                    tableName.append(c);
                    continue;
                }

                if (']' != c) {
                    keyColumn.append(c);
                } else {
                    String modesStr = table.substring(i + 1);
                    if (modesStr.length() == 0) {
                        cdcModeList.add(TableConfig.CM_ROW);
                    } else {
                        String[] modes = modesStr.split(",");
                        cdcModeList.addAll(Arrays.asList(modes));
                        break;
                    }
                }
            }

            tableMetaData.put(tableName.toString(),
                new TableConfig(tableName.toString(), keyColumn.toString(), cdcModeList));
        }
    }

    public boolean tableContains(String tableName) {
        return tableMetaData.containsKey(tableName);
    }

    public TableConfig getTableConfig(String tableName) {
        return tableMetaData.get(tableName);
    }

    public void setDataFormatterRegistry(DataFormatterRegistry dataFormatterRegistry) {
        this.dataFormatterRegistry = dataFormatterRegistry;
    }

    public DataFormatterRegistry getDataFormatterRegistry() {
        return dataFormatterRegistry;
    }

    public FunctionCalculator getFunctionCalculator() {
        return functionCalculator;
    }

    public void setFunctionCalculator(FunctionCalculator functionCalculator) {
        this.functionCalculator = functionCalculator;
    }
}
