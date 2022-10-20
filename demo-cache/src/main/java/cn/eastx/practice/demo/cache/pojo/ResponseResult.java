package cn.eastx.practice.demo.cache.pojo;

import cn.eastx.practice.demo.cache.constants.ResponseEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回对象
 *
 * @author EastX
 * @date 2022/10/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseResult<T> {

    /**
     * response code, 200 -> OK.
     */
    private String code;

    /**
     * response message.
     */
    private String message;

    /**
     * response data.
     */
    private T data;

    /**
     * response timestamp.
     */
    private Long timestamp;

    /**
     * response success result wrapper.
     *
     * @param <T> keyType of data class
     * @return response result
     */
    public static <T> ResponseResult<T> success() {
        return success(null);
    }

    /**
     * response success result wrapper.
     *
     * @param data response data
     * @param <T>  keyType of data class
     * @return response result
     */
    public static <T> ResponseResult<T> success(T data) {
        return ResponseResult.<T>builder()
                .code(ResponseEnum.SUCCESS.getCode())
                .message(ResponseEnum.SUCCESS.getDescription())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    /**
     * response error result wrapper.
     *
     * @param message error message
     * @param <T>     keyType of data class
     * @return response result
     */
    public static <T extends Serializable> ResponseResult<T> fail(String message) {
        return fail(message, null);
    }

    /**
     * response error result wrapper.
     *
     * @param data    response data
     * @param message error message
     * @param <T>     keyType of data class
     * @return response result
     */
    public static <T> ResponseResult<T> fail(String message, T data) {
        return ResponseResult.<T>builder()
                .code(ResponseEnum.FAIL.getCode())
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
