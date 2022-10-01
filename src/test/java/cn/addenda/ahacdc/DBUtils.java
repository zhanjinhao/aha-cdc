package cn.addenda.ahacdc;

import cn.addenda.ahacdc.format.DefaultDataFormatterRegistry;
import cn.addenda.ec.function.calculator.DefaultFunctionCalculator;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * @author addenda
 * @datetime 2022/9/4 18:32
 */
public class DBUtils {

    public static Connection getConnection() {
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            return DriverManager.getConnection(
                DBPropertiesReader.read("db.url"),
                DBPropertiesReader.read("db.username"),
                DBPropertiesReader.read("db.password"));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static DataSource getDataSource() {
        DataSource dataSource = new DataSource() {
            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {

            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {

            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }

            @Override
            public Connection getConnection() throws SQLException {
                return DBUtils.getConnection();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DBUtils.getConnection();
            }
        };

        CdcDataSource cdcDataSource = new CdcDataSource(dataSource, new DefaultDataFormatterRegistry(), DefaultFunctionCalculator.getInstance());
        cdcDataSource.setTableMetaData("t_cdc_test[id]s,r");
        return cdcDataSource;
    }

}
