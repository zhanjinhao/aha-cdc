package cn.addenda.ahacdc.sql;

import cn.addenda.ahacdc.CdcException;
import cn.addenda.ahacdc.format.DataFormatterRegistry;
import cn.addenda.businesseasy.asynctask.BinaryResult;
import cn.addenda.ec.calculator.CalculatorFactory;
import cn.addenda.ec.function.calculator.FunctionCalculator;
import cn.addenda.ro.grammar.ast.CurdUtils;
import cn.addenda.ro.grammar.ast.create.Insert;
import cn.addenda.ro.grammar.ast.create.InsertSelectRep;
import cn.addenda.ro.grammar.ast.create.InsertSetRep;
import cn.addenda.ro.grammar.ast.create.InsertValuesRep;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.expression.AssignmentList.Entry;
import cn.addenda.ro.grammar.ast.update.Update;
import cn.addenda.ro.grammar.lexical.scan.DefaultScanner;
import cn.addenda.ro.grammar.lexical.scan.TokenSequence;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;
import sun.security.krb5.internal.PAData;

import java.math.BigInteger;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author addenda
 * @datetime 2022/9/29 8:44
 */
public class SqlHelper {

    private final FunctionCalculator functionCalculator;

    public SqlHelper(FunctionCalculator functionCalculator) {
        this.functionCalculator = functionCalculator;
    }

    /**
     * 从 Insert SQL 里面提取出来字段名。
     * 其他语句抛出异常。
     */
    public <T> T extractColumnValueFromInsertSql(String sql, String keyColumn, Class<T> clazz) {
        if (!SqlUtils.isInsertSql(sql)) {
            throw new CdcException("only support insert sql. ");
        }
        Insert insert = CurdUtils.parseInsert(sql, functionCalculator, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) insertRep;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                if (keyColumn.equals(entry.getColumn().getLiteral())) {
                    Curd value = entry.getValue();
                    if (!(value instanceof Literal)) {
                        throw new CdcException("keyColumn must be Long or long data type");
                    }
                    Literal literal = (Literal) value;
                    Token result = literal.getValue();
                    if (clazz.isAssignableFrom(result.getLiteral().getClass())) {
                        return (T) result.getLiteral();
                    }
                }
            }
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            int index = -1;
            for (int i = 0; i < columnList.size(); i++) {
                Token token = columnList.get(i);
                if (keyColumn.equals(token.getLiteral())) {
                    index = i;
                }
            }
            if (index != -1) {
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                Literal literal = (Literal) curdListList.get(0).get(index);
                Token result = literal.getValue();
                if (clazz.isAssignableFrom(result.getLiteral().getClass())) {
                    return (T) result.getLiteral();
                }
            }
        } else {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }
        return null;
    }


    public boolean checkStableUpdateSql(String sql, String keyColumn) {
        Update update = CurdUtils.parseUpdate(sql, functionCalculator, false);

        // where 里的条件列
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        Set<Token> conditionColumnList = new HashSet<>();
        if (whereSeg != null) {
            conditionColumnList = whereSeg.accept(WhereSegColumnVisitor.getInstance());
        }
        Set<String> conditionColumnNameList = conditionColumnList
                .stream().map(item -> String.valueOf(item.getLiteral())).collect(Collectors.toSet());

        // where 里的条件列 和 key列 不能在更新列里面出现
        AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
        List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
        for (AssignmentList.Entry entry : entryList) {
            String columnName = String.valueOf(entry.getColumn().getLiteral());
            if (keyColumn.equals(columnName) || conditionColumnNameList.contains(columnName)) {
                return false;
            }
        }
        return true;
    }


    /**
     * @return first dependentColumnNameList; second calculableColumnNameList
     */
    public BinaryResult<List<String>, List<BinaryResult<String, Curd>>> divideColumnFromUpdateOrInsertSql(String sql) {
        if (!SqlUtils.isInsertSql(sql) && !SqlUtils.isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        List<String> dependentColumnNameList = new ArrayList<>();
        List<BinaryResult<String, Curd>> calculableColumnNameList = new ArrayList<>();

        Curd parse = CurdUtils.parse(sql, functionCalculator, false);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    Curd value = entry.getValue();
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (value instanceof Literal) {
                        // no-op
                    } else if (Boolean.TRUE.equals(value.accept(InsertOrUpdateCalculableColumnVisitor.getInstance()))) {
                        calculableColumnNameList.add(new BinaryResult<>(columnName, value));
                    } else {
                        dependentColumnNameList.add(columnName);
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < curdList.size(); i++) {
                    Curd value = curdList.get(i);
                    String columnName = String.valueOf(columnList.get(i).getLiteral());
                    if (value instanceof Literal) {
                        // no-op
                    } else if (Boolean.TRUE.equals(value.accept(InsertOrUpdateCalculableColumnVisitor.getInstance()))) {
                        calculableColumnNameList.add(new BinaryResult<>(columnName, value));
                    } else {
                        dependentColumnNameList.add(columnName);
                    }
                }
            } else {
                throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
            }
        } else if (parse instanceof Update) {
            Update update = (Update) parse;
            AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                Curd value = entry.getValue();
                String columnName = String.valueOf(entry.getColumn().getLiteral());
                if (value instanceof Literal) {
                    // no-op
                } else if (Boolean.TRUE.equals(value.accept(InsertOrUpdateCalculableColumnVisitor.getInstance()))) {
                    calculableColumnNameList.add(new BinaryResult<>(columnName, value));
                } else {
                    dependentColumnNameList.add(columnName);
                }
            }
        }

        return new BinaryResult<>(dependentColumnNameList, calculableColumnNameList);
    }

    public boolean checkInsertMultipleRows(String insertSql) {
        Insert insert = CurdUtils.parseInsert(insertSql, functionCalculator, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            return false;
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            return curdListList.size() > 1;
        } else {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }
    }

    public List<String> splitInsertMultipleRows(String multipleRowsInsertSql) {
        List<String> singleRowList = new ArrayList<>();
        Insert insert = CurdUtils.parseInsert(multipleRowsInsertSql, functionCalculator, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            singleRowList.add(multipleRowsInsertSql);
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            String prefix = "insert into "
                    + insert.getTableName().getLiteral() + "(" + columnList.stream().map(item -> String.valueOf(item.getLiteral())).collect(Collectors.joining(","))
                    + ") values ";
            List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
            for (List<Curd> curdList : curdListList) {
                String sql = prefix + curdList.stream().map(Curd::toString).collect(Collectors.joining(",", "(", ")"));
                singleRowList.add(sql);
            }
        } else {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }
        return singleRowList;
    }

    public String insertInjectColumnValue(String sql, String keyColumn, Token token) {
        Insert insert = CurdUtils.parseInsert(sql, functionCalculator, false);
        Curd insertRep = insert.getInsertRep();
        if (insertRep instanceof InsertSetRep) {
            InsertSetRep insertSetRep = (InsertSetRep) insertRep;
            AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            entryList.add(new AssignmentList.Entry(new Token(TokenType.IDENTIFIER, keyColumn), new Literal(token)));
        } else if (insertRep instanceof InsertValuesRep) {
            InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
            List<Token> columnList = insertValuesRep.getColumnList();
            columnList.add(new Token(TokenType.IDENTIFIER, keyColumn));
            List<Curd> curdList = insertValuesRep.getCurdListList().get(0);
            curdList.add(new Literal(token));
        } else if (insertRep instanceof InsertSelectRep) {
            throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
        }

        return insert.toString();
    }

    public String updateOrInsertUpdateColumnValue(String sql, Map<String, Token> columnTokenMap) {
        if (!SqlUtils.isInsertSql(sql) && !SqlUtils.isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        Curd parse = CurdUtils.parse(sql, functionCalculator, false);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (columnTokenMap.containsKey(columnName)) {
                        entry.setValue(new Literal(columnTokenMap.get(columnName)));
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < columnList.size(); i++) {
                    String columnName = String.valueOf(columnList.get(i).getLiteral());
                    if (columnTokenMap.containsKey(columnName)) {
                        curdList.set(i, new Literal(columnTokenMap.get(columnName)));
                    }
                }
            } else {
                throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
            }
        } else if (parse instanceof Update) {
            Update update = (Update) parse;
            AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                String columnName = String.valueOf(entry.getColumn().getLiteral());
                if (columnTokenMap.containsKey(columnName)) {
                    entry.setValue(new Literal(columnTokenMap.get(columnName)));
                }
            }
        }

        return parse.toString();
    }

    public String updateOrInsertCalculateColumnValue(String sql, List<String> calculableColumnList, DataFormatterRegistry dataFormatterRegistry) {
        if (!SqlUtils.isInsertSql(sql) && !SqlUtils.isUpdateSql(sql)) {
            throw new CdcException("only support insert and update sql. ");
        }

        Curd parse = CurdUtils.parse(sql, functionCalculator, false);
        if (parse instanceof Insert) {
            Insert insert = (Insert) parse;
            Curd insertRep = insert.getInsertRep();
            if (insertRep instanceof InsertSetRep) {
                InsertSetRep insertSetRep = (InsertSetRep) insertRep;
                AssignmentList assignmentList = (AssignmentList) insertSetRep.getAssignmentList();
                List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
                for (AssignmentList.Entry entry : entryList) {
                    String columnName = String.valueOf(entry.getColumn().getLiteral());
                    if (calculableColumnList.contains(columnName)) {
                        Object result = CalculatorFactory.createExpressionCalculator(entry.getValue(), functionCalculator).calculate();
                        entry.setValue(new Literal(dataFormatterRegistry.parse(result)));
                    }
                }
            } else if (insertRep instanceof InsertValuesRep) {
                InsertValuesRep insertValuesRep = (InsertValuesRep) insertRep;
                List<List<Curd>> curdListList = insertValuesRep.getCurdListList();
                List<Curd> curdList = curdListList.get(0);
                List<Token> columnList = insertValuesRep.getColumnList();
                for (int i = 0; i < columnList.size(); i++) {
                    String columnName = String.valueOf(columnList.get(i).getLiteral());
                    if (calculableColumnList.contains(columnName)) {
                        Object result = CalculatorFactory.createExpressionCalculator(curdList.get(i), functionCalculator).calculate();
                        curdList.set(i, new Literal(dataFormatterRegistry.parse(result)));
                    }
                }
            } else {
                throw new CdcException("不支持的Insert语法，仅支持：insert into T() values() 和 insert into T set c = '1' 两种语法");
            }
        } else if (parse instanceof Update) {
            Update update = (Update) parse;
            AssignmentList assignmentList = (AssignmentList) update.getAssignmentList();
            List<AssignmentList.Entry> entryList = assignmentList.getEntryList();
            for (AssignmentList.Entry entry : entryList) {
                String columnName = String.valueOf(entry.getColumn().getLiteral());
                if (calculableColumnList.contains(columnName)) {
                    Object result = CalculatorFactory.createExpressionCalculator(entry.getValue(), functionCalculator).calculate();
                    entry.setValue(new Literal(dataFormatterRegistry.parse(result)));
                }
            }
        }

        return parse.toString();
    }

    public BinaryResult<String, List<Long>> separateUpdateSegAndKeyValues(String keyInOrKeyEqualConditionUpdateSql) {
        Update update = CurdUtils.parseUpdate(keyInOrKeyEqualConditionUpdateSql, functionCalculator, false);

        List<Long> keyValueList = new ArrayList<>();
        WhereSeg whereSeg = (WhereSeg) update.getWhereSeg();
        Curd logic = whereSeg.getLogic();
        if (logic instanceof InCondition) {
            InCondition inCondition = (InCondition) logic;
            List<Curd> range = inCondition.getRange();
            for (Curd curd : range) {
                Token value = ((Literal) curd).getValue();
                keyValueList.add(((BigInteger) value.getLiteral()).longValue());
            }
        } else if (logic instanceof Comparison) {
            Comparison comparison = (Comparison) logic;
            Token value = ((Literal) comparison.getRightCurd()).getValue();
            keyValueList.add(((BigInteger) value.getLiteral()).longValue());
        } else {
            throw new CdcException("仅支持 key in 和 key equal 语法！");
        }

        int whereIndex = keyInOrKeyEqualConditionUpdateSql.indexOf("where");
        String updateSeg = keyInOrKeyEqualConditionUpdateSql.substring(0, whereIndex);
        return new BinaryResult<>(updateSeg, keyValueList);
    }

    public String toStorableSql(String sql) {
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        StringBuilder sb = new StringBuilder("'");
        for (Token token : source) {
            TokenType type = token.getType();
            // 遇到 group_concat 的时候使用SQL解析器。
            if (TokenType.GROUP_CONCAT.equals(type)) {
                return CurdUtils.parse(sql, functionCalculator, false)
                        .toString(" ", true);
            }
            String literal = String.valueOf(token.getLiteral());
            if (TokenType.STRING.equals(type)) {
                sb.append(" \\'");
                int length = literal.length();
                for (int i = 0; i < length; i++) {
                    char c = literal.charAt(i);
                    if (c == '\'') {
                        sb.append("\\'");
                    }
                    sb.append(c);
                }
                sb.append("\\'");
            } else if (TokenType.EOF.equals(type)) {
                // no-op
            } else {
                // mysql 的函数 支持 now(), now( )写法，但是不支持 now (), now ( ) 写法。所以(前不留空格
                if (!("(".equals(literal))) {
                    sb.append(" ");
                }
                sb.append(literal);
            }
        }
        sb.append("'");
        return sb.toString();
    }


    private static final Map<String, KeyInOrKeyEqualConditionVisitor> keyInOrKeyEqualConditionVisitorMap = new ConcurrentHashMap<>();

    public int analysisLeastTxIsolation(String parameterizedSql, String keyColumn) {
        String whereCondition = SqlUtils.extractWhereConditionFromUpdateOrDeleteSql(parameterizedSql, false);
        if (whereCondition.length() == 0) {
            return Connection.TRANSACTION_REPEATABLE_READ;
        }
        KeyInOrKeyEqualConditionVisitor visitor = keyInOrKeyEqualConditionVisitorMap.computeIfAbsent(keyColumn, s -> new KeyInOrKeyEqualConditionVisitor(keyColumn));
        if (Boolean.TRUE.equals(CurdUtils.parseExpression(whereCondition, functionCalculator, false).accept(visitor))) {
            return Connection.TRANSACTION_READ_COMMITTED;
        } else {
            return Connection.TRANSACTION_REPEATABLE_READ;
        }
    }

}
