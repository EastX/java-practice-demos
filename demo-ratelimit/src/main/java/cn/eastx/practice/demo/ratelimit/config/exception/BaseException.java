package cn.eastx.practice.demo.ratelimit.config.exception;

import lombok.Getter;

/**
 * 业务基础异常
 *
 * @author EastX
 * @date 2022/11/5
 */
@Getter
public class BaseException extends RuntimeException {

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

}
