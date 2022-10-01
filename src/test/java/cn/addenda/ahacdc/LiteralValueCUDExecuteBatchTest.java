package cn.addenda.ahacdc;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.sql.DataSource;
import java.sql.*;

/**
 * @author addenda
 * @datetime 2022/9/4 18:23
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LiteralValueCUDExecuteBatchTest {

    private Connection connection;

    @Before
    public void before() {
        DataSource dataSource = DBUtils.getDataSource();
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test01_insert() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "insert into t_cdc_test(long_d, int_d, string_d, date_d, time_d, datetime_d, float_d, double_d) values (?,?,replace(?, 'a', '\\''),?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, 1L);
        ps.setInt(2, 2);
        ps.setString(3, "3a");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
        ps.setFloat(7, 1.1f);
        ps.setDouble(8, 2.2d);
        ps.addBatch();
        ps.setLong(1, 2L);
        ps.setInt(2, 3);
        ps.setString(3, "4");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
        ps.setFloat(7, 2.2f);
        ps.setDouble(8, 4.3d);
        ps.addBatch();

        ps.executeBatch();

        connection.commit();
    }


    @Test
    public void test02_update() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "update t_cdc_test set date_d = ?, time_d = ?, datetime_d =date_add(?, interval 1 day) where long_d = ?");
        ps.setDate(1, new Date(System.currentTimeMillis()));
        ps.setTime(2, new Time(System.currentTimeMillis()));
        ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
        ps.setLong(4, 1L);
        ps.addBatch();
        ps.setLong(4, 2L);
        ps.addBatch();

        ps.executeBatch();

        connection.commit();
    }

    @Test
    public void test03_delete() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "delete from t_cdc_test where long_d = ?");
        ps.setLong(1, 1L);
        ps.addBatch();
        ps.setLong(1, 2L);
        ps.addBatch();

        ps.executeBatch();

        connection.commit();
    }

    @After
    public void after() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
