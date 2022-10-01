package cn.addenda.ahacdc.jdbc;

import cn.addenda.ahacdc.CdcConnection;
import cn.addenda.ahacdc.DBUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/4 18:23
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataTypeTest {

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

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    public void test01_insert_OffsetDateTime() throws Exception {
        PreparedStatement ps = ((CdcConnection) connection).getDelegate().prepareStatement(
                "insert into t_cdc_test(long_d, int_d, string_d, date_d, time_d, datetime_d, float_d, double_d) values (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, 1L);
        ps.setInt(2, 2);
        ps.setString(3, "3");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setObject(6, OffsetDateTime.now(ZoneId.of("America/New_York")));
        ps.setFloat(7, 1.1f);
        ps.setDouble(8, 2.2d);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("America/New_York"));
        System.out.println(now);
        System.out.println(DATE_TIME_FORMATTER.format(now));
        System.out.println(DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault())));

        System.out.println(LocalDateTime.now(ZoneId.of("America/New_York")));
        System.out.println(DATE_TIME_FORMATTER.format(LocalDateTime.now(ZoneId.of("America/New_York"))));

        ps.executeUpdate();

        connection.commit();
    }

    @Test
    public void test01_insert_ZonedDateTime() throws Exception {
        PreparedStatement ps = ((CdcConnection) connection).getDelegate().prepareStatement(
                "insert into t_cdc_test(long_d, int_d, string_d, date_d, time_d, datetime_d, float_d, double_d) values (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, 1L);
        ps.setInt(2, 2);
        ps.setString(3, "3");
        ps.setDate(4, new Date(System.currentTimeMillis()));
        ps.setTime(5, new Time(System.currentTimeMillis()));
        ps.setObject(6, ZonedDateTime.now(ZoneId.of("America/New_York")));
        ps.setFloat(7, 1.1f);
        ps.setDouble(8, 2.2d);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        System.out.println(now);
        System.out.println(DATE_TIME_FORMATTER.format(now));
        System.out.println(DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault())));

        System.out.println(LocalDateTime.now(ZoneId.of("America/New_York")));
        System.out.println(DATE_TIME_FORMATTER.format(LocalDateTime.now(ZoneId.of("America/New_York"))));

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
