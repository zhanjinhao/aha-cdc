package cn.addenda.ahacdc.format;

import cn.addenda.ahacdc.CdcException;
import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author addenda
 * @datetime 2022/9/10 20:10
 */
public abstract class AbstractDataFormatterRegistry implements DataFormatterRegistry {

    protected final Map<Class<?>, DataFormatter<?>> dataFormatterMap = new ConcurrentHashMap<>();

    @Override
    public String format(Object obj) {
        if (obj == null) {
            return "null";
        }
        DataFormatter<?> dataFormatter = dataFormatterMap.get(obj.getClass());
        return dataFormatter.format(dataFormatter.getType().cast(obj));
    }

    @Override
    public Token parse(Object obj) {
        if (obj == null) {
            return new Token(TokenType.NULL, "null");
        }
        DataFormatter<?> dataFormatter = dataFormatterMap.get(obj.getClass());
        return dataFormatter.parse(dataFormatter.getType().cast(obj));
    }

    @Override
    public boolean typeAvailable(Class<?> clazz) {
        return dataFormatterMap.containsKey(clazz);
    }

    protected void addDataFormatter(DataFormatter<?> dataFormatter) {
        if (dataFormatterMap.containsKey(dataFormatter.getType())) {
            throw new CdcException(dataFormatter.getType().getSimpleName() + ", dataFormatter has exists! ");
        }
        dataFormatterMap.put(dataFormatter.getType(), dataFormatter);
    }

}
