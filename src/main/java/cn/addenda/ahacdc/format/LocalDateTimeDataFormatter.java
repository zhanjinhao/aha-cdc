package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class LocalDateTimeDataFormatter implements DataFormatter<LocalDateTime> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Class<LocalDateTime> getType() {
        return LocalDateTime.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + DATE_TIME_FORMATTER.format((LocalDateTime) obj) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, DATE_TIME_FORMATTER.format((LocalDateTime) obj));
    }

}
