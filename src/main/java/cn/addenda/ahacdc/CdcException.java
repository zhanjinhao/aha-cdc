package cn.addenda.ahacdc;

import cn.addenda.businesseasy.BusinessEasyException;

/**
 * @author addenda
 * @datetime 2022/8/24 19:32
 */
public class CdcException extends BusinessEasyException {

    public CdcException(Throwable cause) {
        super(cause);
    }

    public CdcException(String message) {
        super(message);
    }

}
