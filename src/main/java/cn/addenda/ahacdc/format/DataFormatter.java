package cn.addenda.ahacdc.format;

import cn.addenda.ro.grammar.lexical.token.Token;

/**
 * @author addenda
 * @datetime 2022/9/10 16:51
 */
public interface DataFormatter<T> {

    String SINGLE_QUOTATION = "'";

    Class<T> getType();

    String format(Object obj);

    Token parse(Object obj);

}
