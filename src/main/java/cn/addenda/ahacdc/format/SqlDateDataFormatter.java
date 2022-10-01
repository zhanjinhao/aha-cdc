package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class SqlDateDataFormatter implements DataFormatter<Date> {
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    @Override
    public Class<Date> getType() {
        return Date.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + DATE_FORMAT_THREAD_LOCAL.get().format((Date) obj) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, DATE_FORMAT_THREAD_LOCAL.get().format((Date) obj));
    }

}
