package cn.addenda.ahacdc;

import java.util.List;

/**
 * @author addenda
 * @datetime 2022/9/5 20:42
 */
public class TableConfig {
    public static final String CM_ROW = "r";

    /**
     * 无法指出那条数据出现了变化，但是可以重放得到与原表一样的数据
     */
    public static final String CM_STATEMENT = "s";

    private final String tableName;
    private final String keyColumn;
    private final List<String> cdcModeList;

    public TableConfig(String tableName, String keyColumn, List<String> cdcModeList) {
        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.cdcModeList = cdcModeList;
    }

    public String getTableName() {
        return tableName;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

    public List<String> getCdcModeList() {
        return cdcModeList;
    }
}
