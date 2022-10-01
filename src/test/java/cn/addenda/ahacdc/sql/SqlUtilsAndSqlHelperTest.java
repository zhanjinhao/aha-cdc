package cn.addenda.ahacdc.sql;

import cn.addenda.ec.function.calculator.DefaultFunctionCalculator;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

/**
 * @author addenda
 * @datetime 2022/9/4 14:59
 */
public class SqlUtilsAndSqlHelperTest {

    private SqlHelper sqlHelper;

    @Before
    public void before() {
        sqlHelper = new SqlHelper(DefaultFunctionCalculator.getInstance());
    }

    @Test
    public void test1() {

        System.out.println(SqlUtils.extractTableNameFromDmlSql("delete from score where CREATE_TM  < date_add( now(),  interval 1 day )   and DEGREE  + 1  < 60 - 1"));
        System.out.println(SqlUtils.extractTableNameFromDmlSql("insert into score ( SNO, CNO, DEGREE ) values ( 109, '3-105', 76 )"));
        System.out.println(SqlUtils.extractTableNameFromDmlSql("update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' )  where runoob_id  = 3"));

    }

    @Test
    public void test2() {
        System.out.println(sqlHelper.extractColumnValueFromInsertSql(
                "insert into score ( SNO, CNO, DEGREE ) values ( 109, '3-105', 76 )",
                "SNO", BigInteger.class));

        System.out.println(sqlHelper.extractColumnValueFromInsertSql(
                "insert ignore into score set SNO='1387398', CNO=#{cno}, DEGREE=?",
                "SNO", String.class));
    }

    @Test
    public void test3() {
        System.out.println(SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(
                "delete from score where CREATE_TM  < date_add( now(),  interval 1 day )   and DEGREE  + 1  < 60 - 1"));
        System.out.println(SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(
                "update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' ) , a=?  + 1 , b=?"));
        System.out.println(SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(
                "update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' )  where runoob_id  = 3"));
    }

    @Test
    public void test4() {
        System.out.println(sqlHelper.checkStableUpdateSql(
                "update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' )  where runoob_title  = '1'", "id"));
        System.out.println(sqlHelper.checkStableUpdateSql(
                "update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' ) , id=?  + 1 , b=?", "id"));
        System.out.println(sqlHelper.checkStableUpdateSql(
                "update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' )  where runoob_id  = 3", "id"));
    }

    @Test
    public void test5() {
        System.out.println(sqlHelper.divideColumnFromUpdateOrInsertSql(
                "update runoob_tbl set runoob_title=replace( runoob_title , 'c++', 'python' )  where runoob_title  = '1'"));

        System.out.println(sqlHelper.divideColumnFromUpdateOrInsertSql(
                "update runoob_tbl set runoob_title=replace( '1234567' , 'c++', 'python' )  where runoob_title  = '1'"));

        System.out.println(sqlHelper.divideColumnFromUpdateOrInsertSql(
                "update runoob_tbl set id=?  + 1 , b=1"));

        System.out.println(sqlHelper.divideColumnFromUpdateOrInsertSql(
                "update runoob_tbl set id=1  + 1 , b=?"));

    }

    @Test
    public void test6() {
        String sql = "insert into A(a, b) values (1,'2'), (3,'4'), (3+1,concat('2','3'))";
        List<String> list = sqlHelper.splitInsertMultipleRows(sql);
        list.forEach(System.out::println);
    }

    @Test
    public void test7() {
        String sql1 = "insert into auto_filled_test(name) values ('1\\'2')";
        System.out.println(sqlHelper.toStorableSql(sql1));
        String sql2 = "select  group_concat (  DELETE_FLAG ,  CREATOR  + '1'  separator '\\'' ) as a from t_fp_day_navigation   group by DELETE_FLAG, CREATOR having  group_concat (  DELETE_FLAG  + '1'  order by CREATOR  asc separator ';' ) != ''  order by  ( '1' + DELETE_FLAG   )  desc";
        System.out.println(sqlHelper.toStorableSql(sql2));
    }

}
