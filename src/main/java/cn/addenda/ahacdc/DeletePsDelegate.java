package cn.addenda.ahacdc;

import cn.addenda.businesseasy.util.BEListUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author addenda
 * @datetime 2022/9/24 9:52
 */
public class DeletePsDelegate extends AbstractPsDelegate {

    public DeletePsDelegate(CdcConnection cdcConnection, PreparedStatement ps, TableConfig tableConfig, String parameterizedSql) {
        super(cdcConnection, ps, tableConfig, parameterizedSql);
    }

    @Override
    protected <T> void doAssert(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        assertTxIsolationNotLessThan(Connection.TRANSACTION_REPEATABLE_READ);
    }

    @Override
    public <T> T doExecute(List<String> executableSqlList, PsInvocation<T> pi) throws SQLException {
        // -------------------------------------
        //  对于Statement模式来说，记录下来SQL就行了
        // -------------------------------------
        if (checkTableMode(TableConfig.CM_STATEMENT)) {
            List<String> statementCdcSqlList = new ArrayList<>(executableSqlList);
            executeCdcSql(TableConfig.CM_STATEMENT, statementCdcSqlList);
        }

        // ----------------------------------
        //  对于ROW模式，需要记录下来具体删除的行。
        // ----------------------------------
        if (checkTableMode(TableConfig.CM_ROW)) {
            List<String> rowCdcSqlList = new ArrayList<>();
            // 多余delete语句来说，在batch模式下，如果sqlX和sqlY同时命中了KeyN，则只应该记录一次。
            Set<Long> keyValueSet = new LinkedHashSet<>();
            try (Statement statement = cdcConnection.getDelegate().createStatement()) {
                for (String executableSql : executableSqlList) {
                    // select 获取主键值。
                    keyValueSet.addAll(lockKey(statement, executableSql));
                }
            }
            // 对于simple模式 ： 1:n -> 1:1 优化；
            // 对于batch模式，也进行了： 1:1 -> n:1 优化。
            if (!keyValueSet.isEmpty()) {
                List<List<Long>> listList = BEListUtil.splitList(new ArrayList<>(keyValueSet), IN_SIZE);
                for (List<Long> item : listList) {
                    rowCdcSqlList.add("delete from " + tableName + " where " + keyColumn + " in (" + longListToString(item) + ")");
                }
            }
            executeCdcSql(TableConfig.CM_ROW, rowCdcSqlList);
        }

        return pi.invoke();
    }

}
