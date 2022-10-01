package cn.addenda.ahacdc;

import java.sql.SQLException;

/**
 * @author addenda
 * @datetime 2022/8/27 17:01
 */
@FunctionalInterface
public interface PsInvocation<R> {

    R invoke() throws SQLException;

}
