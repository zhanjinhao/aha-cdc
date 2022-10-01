package cn.addenda.ahacdc;

import cn.addenda.ahacdc.sql.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author addenda
 * @datetime 2022/8/24 17:03
 */
public class CdcConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(CdcConnection.class);

    private static final String CDC_CONNECTION_CALLABLE_WARN_REASON = "CdcConnection do not support CallableStatement. Original CallableStatement is created. ";
    private static final String CDC_CONNECTION_AUTO_COMMIT_EXCEPTION_REASON = "CdcConnection do not support auto commit! ";

    private final Connection delegate;

    private final CdcDataSource cdcDataSource;

    private boolean autoCommit;

    private boolean txActive = false;

    public CdcConnection(Connection delegate, CdcDataSource cdcDataSource) throws SQLException {
        this.delegate = delegate;
        this.cdcDataSource = cdcDataSource;
        this.autoCommit = getAutoCommit();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if ((autoCommit != this.autoCommit) && this.txActive) {
            throw new CdcException("当前连接存在未关闭的 CdcPrepareStatement，不可更换 autoCommit 状态。");
        }
        this.autoCommit = autoCommit;
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public void commit() throws SQLException {
        this.txActive = false;
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        this.txActive = false;
        delegate.rollback();
    }

    @Override
    public void close() throws SQLException {
        this.txActive = false;
        delegate.close();
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement statement = delegate.createStatement();
        return new CdcSimpleStatement(statement, this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency);
        return new CdcSimpleStatement(statement, this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return new CdcSimpleStatement(statement, this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement ps = delegate.prepareStatement(sql);
        return wrapPrepareStatementIsNecessary(sql, ps);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement ps = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return wrapPrepareStatementIsNecessary(sql, ps);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        PreparedStatement ps = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return wrapPrepareStatementIsNecessary(sql, ps);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement ps = delegate.prepareStatement(sql, autoGeneratedKeys);
        return wrapPrepareStatementIsNecessary(sql, ps);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement ps = delegate.prepareStatement(sql, columnIndexes);
        return wrapPrepareStatementIsNecessary(sql, ps);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement ps = delegate.prepareStatement(sql, columnNames);
        return wrapPrepareStatementIsNecessary(sql, ps);
    }

    private PreparedStatement wrapPrepareStatementIsNecessary(String sql, PreparedStatement delegate) {
        // 对于DML语句，如果表名被注册为需要进行CDC的表，使用CdcPreparedStatement
        if (!SqlUtils.isSelectSql(sql)) {
            String tableName = SqlUtils.extractTableNameFromDmlSql(sql);
            if (cdcDataSource.tableContains(tableName)) {
                assertAutoCommit();
                txActive = true;
                return new CdcPreparedStatement(tableName, sql, delegate, this);
            }
        }
        return delegate;
    }

    private void assertAutoCommit() {
        if (autoCommit) {
            throw new CdcException(CDC_CONNECTION_AUTO_COMMIT_EXCEPTION_REASON);
        }
    }

    public Connection getDelegate() {
        return delegate;
    }

    public CdcDataSource getCdcDataSource() {
        return cdcDataSource;
    }

    // --------------------------------------------------------------
    //  不支持存储过程，因为存储过程隐藏了具体的sql，无法解析出受影响的表和字段。
    // --------------------------------------------------------------

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        if (logger.isWarnEnabled()) {
            logger.warn(CDC_CONNECTION_CALLABLE_WARN_REASON);
        }
        return delegate.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (logger.isWarnEnabled()) {
            logger.warn(CDC_CONNECTION_CALLABLE_WARN_REASON);
        }
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (logger.isWarnEnabled()) {
            logger.warn(CDC_CONNECTION_CALLABLE_WARN_REASON);
        }
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        delegate.releaseSavepoint(savepoint);
    }

    // -------------------
    //  下面的方法与CDC无关
    // -------------------

    @Override
    public boolean getAutoCommit() throws SQLException {
        return delegate.getAutoCommit();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return delegate.nativeSQL(sql);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return delegate.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return delegate.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        delegate.clearWarnings();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        delegate.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return delegate.getHoldability();
    }

    @Override
    public Clob createClob() throws SQLException {
        return delegate.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return delegate.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return delegate.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return delegate.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return delegate.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return delegate.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return delegate.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return delegate.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

}