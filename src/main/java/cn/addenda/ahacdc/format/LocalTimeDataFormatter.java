package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class LocalTimeDataFormatter implements DataFormatter<LocalTime> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public Class<LocalTime> getType() {
        return LocalTime.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + TIME_FORMATTER.format((LocalTime) obj) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, TIME_FORMATTER.format((LocalTime) obj));
    }

}
