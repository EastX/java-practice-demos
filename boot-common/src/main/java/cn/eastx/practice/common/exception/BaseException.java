package cn.eastx.practice.common.exception;

/**
 * 基础异常
 *
 * @author EastX
 * @date 2023/08/19
 */
public class BaseException extends RuntimeException  {

    /**
     * 错误编码
     */
    private String code;

    /**
     * 错误信息
     */
    private String msg;

    /**
     * 数据
     */
    private Object data;

    public BaseException(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BaseException(String code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public Object getData() {
        return data;
    }
}
