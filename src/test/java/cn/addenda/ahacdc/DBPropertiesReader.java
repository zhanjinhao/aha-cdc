package cn.addenda.ahacdc;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * @author addenda
 * @datetime 2022/9/4 18:24
 */
public class DBPropertiesReader {

    public static String read(String key){
        Properties properties = new Properties();
        try {
            properties.load(new FileReader("src/test/resources/db.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty(key);
    }


    public static void main(String[] args) throws Exception {
        System.out.println(read("db.driver"));
        System.out.println(read("db.url"));
        System.out.println(read("db.username"));
        System.out.println(read("db.password"));
    }

}
