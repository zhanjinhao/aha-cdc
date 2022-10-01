package cn.addenda.ahacdc.jdbc;

import cn.addenda.ahacdc.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author 01395265
 * @date 2020/7/27
 */
public class TestInsertBatchAutoInc {

    public static void main(String[] args) throws Exception {
        Connection connection = DBUtils.getConnection();
        batchInsert(connection);
        DBUtils.closeConnection(connection);
    }

    // 73435
    public static void batchInsert(Connection connection) throws Exception {
        connection.setAutoCommit(false);
        String sql = "insert into t_tx_test(name, remark) values (?, ?),(?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
        statement.setString(1, "1");
        statement.setString(2, "1");
        statement.setString(3, "2");
        statement.setString(4, "2");
        statement.addBatch();
        statement.setString(1, "3");
        statement.setString(2, "3");
        statement.setString(3, "4");
        statement.setString(4, "4");
        statement.addBatch();

        statement.executeBatch();

        ResultSet generatedKeys1 = statement.getGeneratedKeys();
        while (generatedKeys1.next()) {
            long id = generatedKeys1.getLong(1);
            System.out.println(id);
        }

        ResultSet generatedKeys2 = statement.getGeneratedKeys();
        while (generatedKeys2.next()) {
            long id = generatedKeys2.getLong(1);
            System.out.println(id);
        }

        connection.commit();
    }

}
