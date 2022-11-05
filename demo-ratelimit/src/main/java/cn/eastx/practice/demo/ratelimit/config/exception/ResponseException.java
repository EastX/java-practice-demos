package cn.eastx.practice.demo.ratelimit.config.exception;

import cn.eastx.practice.demo.ratelimit.constants.ResponseEnum;

/**
 * 返回数据异常
 *
 * @see GlobalExceptionHandle 全局异常处理
 * @author EastX
 * @date 2022/11/5
 */
public class ResponseException extends BaseException {

    public ResponseException(String code, String msg) {
        super(code, msg);
    }

    public ResponseException(ResponseEnum responseEnum) {
        super(responseEnum.getCode(), responseEnum.getDescription());
    }
}
