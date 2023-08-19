package cn.eastx.practice.common.exception;

/**
 * 业务异常
 *
 * @author EastX
 * @date 2023/08/19
 */
public class BusinessException extends BaseException {

    public BusinessException(String code, String msg) {
        super(code, msg);
    }

}
