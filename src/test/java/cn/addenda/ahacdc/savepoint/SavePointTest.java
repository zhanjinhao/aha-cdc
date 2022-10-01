package cn.addenda.ahacdc.savepoint;

import cn.addenda.ahacdc.DBUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.sql.DataSource;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author addenda
 * @datetime 2022/9/4 18:23
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SavePointTest {

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
        Savepoint savepoint = null;

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into t_cdc_test(long_d, int_d, string_d, date_d, time_d, datetime_d, float_d, double_d) values (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, 1L);
            ps.setInt(2, 2);
            ps.setString(3, "3");
            ps.setDate(4, new Date(System.currentTimeMillis()));
            ps.setTime(5, new Time(System.currentTimeMillis()));
            ps.setObject(6, ZonedDateTime.now(ZoneId.of("America/New_York")));
            ps.setFloat(7, 1.1f);
            ps.setDouble(8, 2.2d);

            ps.executeUpdate();
            savepoint = connection.setSavepoint("123");
            ps.setLong(1, 1L);
            ps.setInt(2, 2);
            ps.setString(3, "012345678901234567890");
            ps.setDate(4, new Date(System.currentTimeMillis()));
            ps.setTime(5, new Time(System.currentTimeMillis()));
            ps.setObject(6, ZonedDateTime.now(ZoneId.of("America/New_York")));
            ps.setFloat(7, 1.1f);
            ps.setDouble(8, 2.2d);
            ps.executeUpdate();

            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (savepoint == null) {
                connection.rollback();
            } else {
                connection.rollback(savepoint);
                connection.commit();
            }
        }

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
