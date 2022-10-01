package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.sql.Time;
import java.text.SimpleDateFormat;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class SqlTimeDataFormatter implements DataFormatter<Time> {
    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    @Override
    public Class<Time> getType() {
        return Time.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + TIME_FORMAT_THREAD_LOCAL.get().format((Time) obj) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, TIME_FORMAT_THREAD_LOCAL.get().format((Time) obj));
    }

}
