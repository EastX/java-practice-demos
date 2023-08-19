package cn.eastx.practice.common.exception;


import cn.eastx.practice.common.response.IResult;

/**
 * 自定义返回数据异常
 *
 * @author EastX
 * @date 2023/08/19
 */
public class ResponseException extends BaseException {

    public ResponseException(String code, String msg) {
        super(code, msg);
    }

    public ResponseException(IResult result) {
        super(result.getCode(), result.getMessage());
    }

}
