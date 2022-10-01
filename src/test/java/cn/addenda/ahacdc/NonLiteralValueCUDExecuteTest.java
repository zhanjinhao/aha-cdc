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
public class NonLiteralValueCUDExecuteTest {

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
                "insert into t_cdc_test(long_d, int_d, string_d, date_d, time_d, datetime_d, float_d, double_d) " +
                        "values (? + 1,?,?,?,?,now(),?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, 1L);
        ps.setInt(2, 2);
        ps.setString(3, "3");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setFloat(6, 1.1f);
        ps.setDouble(7, 2.2d);

        ps.executeUpdate();

        connection.commit();
    }


    @Test
    public void test02_update() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "update t_cdc_test set date_d = ?, time_d = ?, datetime_d =now() where long_d = ? + 1");
        ps.setDate(1, new Date(System.currentTimeMillis()));
        ps.setTime(2, new Time(System.currentTimeMillis()));
        ps.setLong(3, 1L);

        ps.executeUpdate();

        connection.commit();
    }

    @Test
    public void test03_delete() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
                "delete from t_cdc_test where long_d = ? + 1");
        ps.setLong(1, 1L);

        ps.executeUpdate();

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
