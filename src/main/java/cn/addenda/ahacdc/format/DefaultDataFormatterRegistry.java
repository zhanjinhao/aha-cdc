package cn.addenda.ahacdc.format;

/**
 * @author addenda
 * @datetime 2022/9/10 20:09
 */
public class DefaultDataFormatterRegistry extends AbstractDataFormatterRegistry {


    public DefaultDataFormatterRegistry() {
        addDataFormatter(new BigDecimalDataFormatter());
        addDataFormatter(new BigIntegerDataFormatter());
        addDataFormatter(new BooleanDataFormatter());
        addDataFormatter(new ByteDataFormatter());
        addDataFormatter(new CharacterDataFormatter());
        addDataFormatter(new CharSequenceDataFormatter());
        addDataFormatter(new DateDataFormatter());
        addDataFormatter(new DoubleDataFormatter());
        addDataFormatter(new FloatDataFormatter());
        addDataFormatter(new IntegerDataFormatter());
        addDataFormatter(new LocalDateTimeDataFormatter());
        addDataFormatter(new LocalDateDataFormatter());
        addDataFormatter(new LocalTimeDataFormatter());
        addDataFormatter(new LongDataFormatter());
        addDataFormatter(new ShortDataFormatter());
        addDataFormatter(new SqlDateDataFormatter());
        addDataFormatter(new SqlTimeDataFormatter());
        addDataFormatter(new TimestampDataFormatter());
        addDataFormatter(new StringDataFormatter());
        addDataFormatter(new OffsetDateTimeDataFormatter());
        addDataFormatter(new ZonedDateTimeDataFormatter());
    }

}
