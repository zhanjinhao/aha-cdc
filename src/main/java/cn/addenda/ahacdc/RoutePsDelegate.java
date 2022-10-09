package cn.addenda.ahacdc;

import cn.addenda.ro.utils.SqlUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author addenda
 * @datetime 2022/8/24 22:35
 */
public class RoutePsDelegate implements PsDelegate {

    private final PsDelegate realPsDelegate;

    public RoutePsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        if (SqlUtils.isInsertSql(parameterizedSql)) {
            realPsDelegate = new InsertPsDelegate(cdcConnection, ps, tableConfig, parameterizedSql);
        } else if (SqlUtils.isUpdateSql(parameterizedSql)) {
            realPsDelegate = new UpdatePsDelegate(cdcConnection, ps, tableConfig, parameterizedSql);
        } else if (SqlUtils.isDeleteSql(parameterizedSql)) {
            realPsDelegate = new DeletePsDelegate(cdcConnection, ps, tableConfig, parameterizedSql);
        } else {
            throw new CdcException("Only support delete, insert, update sql. ");
        }
    }

    @Override
    public <T> T execute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        return realPsDelegate.execute(executableSqlList, pi);
    }

}
