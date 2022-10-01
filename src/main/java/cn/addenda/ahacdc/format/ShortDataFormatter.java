package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigInteger;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class ShortDataFormatter implements DataFormatter<Short> {

    @Override
    public Class<Short> getType() {
        return Short.class;
    }

    @Override
    public String format(Object obj) {
        return obj.toString();
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.INTEGER, new BigInteger(obj.toString()));
    }
}
