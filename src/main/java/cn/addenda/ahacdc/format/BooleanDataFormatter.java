package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class BooleanDataFormatter implements DataFormatter<Boolean> {

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public String format(Object obj) {
        return Boolean.FALSE.equals(obj) ? "false" : "true";
    }

    @Override
    public Token parse(Object obj) {
        return Boolean.FALSE.equals(obj) ? new Token(TokenType.FALSE, "false") : new Token(TokenType.TRUE, "true");
    }

}
