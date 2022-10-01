package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author addenda
 * @datetime 2022/9/11 17:49
 */
public class ZonedDateTimeDataFormatter implements DataFormatter<ZonedDateTime> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ZoneId zoneId;

    public ZonedDateTimeDataFormatter() {
        zoneId = ZoneId.systemDefault();
    }

    public ZonedDateTimeDataFormatter(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public Class<ZonedDateTime> getType() {
        return ZonedDateTime.class;
    }

    @Override
    public String format(Object obj) {
        ZonedDateTime zonedDateTime = (ZonedDateTime) obj;
        LocalDateTime localDateTime = LocalDateTime.ofInstant(zonedDateTime.toInstant(), zoneId);
        return SINGLE_QUOTATION + DATE_TIME_FORMATTER.format(localDateTime) + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        OffsetDateTime offsetDateTime = (OffsetDateTime) obj;
        LocalDateTime localDateTime = LocalDateTime.ofInstant(offsetDateTime.toInstant(), zoneId);
        return new Token(TokenType.STRING, DATE_TIME_FORMATTER.format(localDateTime));
    }

}
