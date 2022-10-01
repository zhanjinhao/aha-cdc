package cn.addenda.ahacdc.dataformatter;

import cn.addenda.ahacdc.format.DefaultDataFormatterRegistry;

/**
 * @author addenda
 * @datetime 2022/9/10 20:25
 */
public class DefaultDataFormatterTest {

    public static void main(String[] args) {
        DefaultDataFormatterRegistry registry = new DefaultDataFormatterRegistry();
        System.out.println(registry.typeAvailable(Long.class));


    }

}
