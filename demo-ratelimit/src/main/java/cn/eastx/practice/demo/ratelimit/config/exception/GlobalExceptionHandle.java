package cn.eastx.practice.demo.ratelimit.config.exception;

import cn.eastx.practice.demo.ratelimit.constants.ResponseEnum;
import cn.eastx.practice.demo.ratelimit.pojo.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * @author EastX
 * @date 2022/11/5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandle {

    /**
     * handle other exception.
     *
     * @param exception exception
     * @return ResponseResult
     */
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseResult processException(Exception exception) {
        log.error(exception.getLocalizedMessage(), exception);
        return ResponseResult.fail(null, ResponseEnum.HTTP_STATUS_500.getDescription());
    }

    /**
     * 自定义返回异常处理
     *
     * @param exception business exception
     * @return ResponseResult
     */
    @ResponseBody
    @ExceptionHandler(ResponseException.class)
    public ResponseResult processResponseException(ResponseException exception) {
        return ResponseResult.builder()
                .code(exception.getCode())
                .message(exception.getMsg())
                .data(exception.getData())
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
