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
public class NonLiteralValueCUDExecuteBatchTest {

    private Connection connection;

    @Before
    public void before() {
        DataSource dataSource = DBUtils.getDataSource();
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test01_insert() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "insert into t_cdc_test(long_d, int_d, string_d, date_d, time_d, datetime_d, float_d, double_d) " +
                        "values (? + 1, ?, replace(?,'a','\\''), date_add(?, interval 1 day), ?, now(), ?, ?)" +
                        ",(1, 2, '3a', '1970-01-01', '01:00:00', '1970-01-01 01:00:00', 1.1, 2.2)", Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, 1L);
        ps.setInt(2, 2);
        ps.setString(3, "3a");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setFloat(6, 1.1f);
        ps.setDouble(7, 2.2d);
        ps.addBatch();
        ps.setLong(1, 2L);
        ps.setInt(2, 3);
        ps.setString(3, "4a");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setFloat(6, 2.2f);
        ps.setDouble(7, 4.3d);
        ps.addBatch();

        ps.executeBatch();

        connection.commit();
    }


    @Test
    public void test02_update() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "update t_cdc_test set date_d = date_add(?, interval 1 day), time_d = ?, datetime_d =now(), float_d = ? + 1 where long_d = ? + 1");
        ps.setDate(1, new Date(System.currentTimeMillis()));
        ps.setTime(2, new Time(System.currentTimeMillis()));
        ps.setFloat(3, 3.3f);
        ps.setLong(4, 1L);
        ps.addBatch();
        ps.setFloat(3, 3.3f);
        ps.setLong(4, 2L);
        ps.addBatch();

        ps.executeBatch();

        connection.commit();
    }

    @Test
    public void test03_delete() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "delete from t_cdc_test  where long_d = ?+1");
        ps.setLong(1, 1L);
        ps.addBatch();
        ps.setLong(1, 2L);
        ps.addBatch();
        ps.setLong(1, 0L);
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
