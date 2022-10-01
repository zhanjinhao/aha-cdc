package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;
import cn.addenda.ro.grammar.lexical.token.TokenType;

/**
 * @author addenda
 * @datetime 2022/9/10 16:57
 */
public class CharacterDataFormatter implements DataFormatter<Character> {

    @Override
    public Class<Character> getType() {
        return Character.class;
    }

    @Override
    public String format(Object obj) {
        return SINGLE_QUOTATION + obj + SINGLE_QUOTATION;
    }

    @Override
    public Token parse(Object obj) {
        return new Token(TokenType.STRING, obj);
    }
}
