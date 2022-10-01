package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class LocalDateDataFormatter implements DataFormatter<LocalDate> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Class<LocalDate> getType() {
        return LocalDate.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + DATE_FORMATTER.format((LocalDate) obj) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, DATE_FORMATTER.format((LocalDate) obj));
    }

}
