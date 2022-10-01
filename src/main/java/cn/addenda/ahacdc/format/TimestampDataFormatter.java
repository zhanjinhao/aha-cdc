package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class TimestampDataFormatter implements DataFormatter<Timestamp> {
    private static final ThreadLocal<SimpleDateFormat> DATETIME_FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    @Override
    public Class<Timestamp> getType() {
        return Timestamp.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + DATETIME_FORMAT_THREAD_LOCAL.get().format((Timestamp) obj) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, DATETIME_FORMAT_THREAD_LOCAL.get().format((Timestamp) obj));
    }

}
