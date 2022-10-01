package cn.addenda.ahacdc.jdbc;


import cn.addenda.ahacdc.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @author 01395265
 * @date 2020/7/27
 */
public class TestBatchInsertParameter {

    public static void main(String[] args) throws Exception {
        Connection connection = DBUtils.getConnection();
        insertBatch(connection);
        DBUtils.closeConnection(connection);
    }

    // 73435
    public static void insertBatch(Connection connection) throws Exception {
        connection.setAutoCommit(false);
        String sql = "insert into t_course(course_id, course_name, creator) values (?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, "1");
        statement.setString(2, "1");
        statement.setString(3, "1");
        statement.addBatch();
        statement.setString(1, "2");
        statement.setString(2, "2");
        statement.addBatch();

        statement.executeBatch();
        connection.commit();
    }

}
