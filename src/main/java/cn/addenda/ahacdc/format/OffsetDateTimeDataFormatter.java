package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/11 17:49
 */
public class OffsetDateTimeDataFormatter implements DataFormatter<OffsetDateTime> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ZoneId zoneId;

    public OffsetDateTimeDataFormatter() {
        zoneId = ZoneId.systemDefault();
    }

    public OffsetDateTimeDataFormatter(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public Class<OffsetDateTime> getType() {
        return OffsetDateTime.class;
    }

    @Override
    public String format(Object obj) {
        OffsetDateTime offsetDateTime = (OffsetDateTime) obj;
        LocalDateTime localDateTime = LocalDateTime.ofInstant(offsetDateTime.toInstant(), zoneId);
        return SINGLE_QUOTATION + DATE_TIME_FORMATTER.format(localDateTime) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        OffsetDateTime offsetDateTime = (OffsetDateTime) obj;
        LocalDateTime localDateTime = LocalDateTime.ofInstant(offsetDateTime.toInstant(), zoneId);
        return new Token(TokenType.STRING, DATE_TIME_FORMATTER.format(localDateTime));
    }

}
