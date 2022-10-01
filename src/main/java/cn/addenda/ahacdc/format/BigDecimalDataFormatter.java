package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

import java.math.BigDecimal;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class BigDecimalDataFormatter implements DataFormatter<BigDecimal> {

    @Override
    public Class<BigDecimal> getType() {
        return BigDecimal.class;
    }

    @Override
    public String format(Object obj) {
        return obj.toString();
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.DECIMAL, obj);
    }
}
