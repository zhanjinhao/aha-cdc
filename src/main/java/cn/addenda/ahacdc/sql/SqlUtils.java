package cn.addenda.ahacdc.sql;

import cn.addenda.ahacdc.CdcException;
import cn.addenda.ro.grammar.lexical.scan.DefaultScanner;
import cn.addenda.ro.grammar.lexical.scan.TokenSequence;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.List;
import java.util.Locale;

/**
 * @author addenda
 * @datetime 2022/8/27 10:50
 */
public class SqlUtils {

    private SqlUtils() {
    }

    public static boolean isInsertSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("insert");
    }

    public static boolean isDeleteSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("delete");
    }

    public static boolean isUpdateSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("update");
    }

    public static boolean isSelectSql(String sql) {
        return sql.trim().toLowerCase(Locale.ROOT).startsWith("select");
    }

    /**
     * 从 DML SQL 里面提取出来表名。
     * DQL 语句抛出异常。
     */
    public static String extractTableNameFromDmlSql(String sql) {
        if (isSelectSql(sql)) {
            throw new CdcException("only support dml sql. ");
        }
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        boolean tableNamePreTokenFg = false;
        for (Token token : source) {
            if (TokenType.UPDATE.equals(token.getType()) ||
                    TokenType.INTO.equals(token.getType()) ||
                    TokenType.FROM.equals(token.getType())) {
                tableNamePreTokenFg = true;
                continue;
            }
            if (tableNamePreTokenFg) {
                return String.valueOf(token.getLiteral());
            }
        }
        return null;
    }


    /**
     * 从 Update or Delete SQL 里面提取出来 where 条件。
     * 其他语句抛出异常。
     */
    public static String extractWhereConditionFromUpdateOrDeleteSql(String sql, boolean remainWherePrefix) {
        if (!isUpdateSql(sql) && !isDeleteSql(sql)) {
            throw new CdcException("only support update or delete sql. ");
        }
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        StringBuilder sb = new StringBuilder();
        boolean wherePreTokenFg = false;
        for (Token token : source) {
            TokenType type = token.getType();
            if (TokenType.WHERE.equals(type)) {
                wherePreTokenFg = true;
                if (!remainWherePrefix) {
                    continue;
                }
            }
            Object literal = token.getLiteral();
            if (wherePreTokenFg && !TokenType.EOF.equals(type)) {
                if (TokenType.STRING.equals(type)) {
                    sb.append(" '").append(literal).append("'");
                } else {
                    // mysql 的函数 支持 now(), now( )写法，但是不支持 now (), now ( ) 写法。所以(前不留空格
                    if (!("(".equals(literal))) {
                        sb.append(" ");
                    }
                    sb.append(literal);
                }
            }
        }
        return !wherePreTokenFg ? "" : sb.toString();
    }


    public static String replaceDmlWhereSeg(String sql, String whereSeg) {
        if (!isUpdateSql(sql) && !isDeleteSql(sql)) {
            throw new CdcException("only support update or delete sql. ");
        }
        TokenSequence tokenSequence = new DefaultScanner(sql).scanTokens();
        List<Token> source = tokenSequence.getSource();
        StringBuilder sb = new StringBuilder();
        for (Token token : source) {
            TokenType type = token.getType();
            if (TokenType.WHERE.equals(type)) {
                break;
            }
            Object literal = token.getLiteral();
            if (TokenType.STRING.equals(type)) {
                sb.append(" '").append(literal).append("'");
            } else {
                // mysql 的函数 支持 now(), now( )写法，但是不支持 now (), now ( ) 写法。所以(前不留空格
                if (!("(".equals(literal))) {
                    sb.append(" ");
                }
                sb.append(literal);
            }
        }
        return sb + " " + whereSeg;
    }

}
