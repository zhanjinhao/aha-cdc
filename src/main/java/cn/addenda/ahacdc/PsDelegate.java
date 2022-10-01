package cn.addenda.ahacdc;

import java.sql.SQLException;
import java.util.List;

/**
 * @author addenda
 * @datetime 2022/9/24 10:03
 */
public interface PsDelegate {

    <T> T execute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException;

}
