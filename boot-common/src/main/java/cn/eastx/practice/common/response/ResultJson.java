package cn.eastx.practice.common.response;

import java.io.Serializable;

/**
 * 统一返回对象（Json格式）
 *
 * @author EastX
 * @date 2023/08/19
 */
public class ResultJson<T>  extends BaseResult {

    /**
     * response data.
     */
    private T data;

    public ResultJson(String code, String message, T data) {
        super(code, message);
        this.data = data;
    }

    /**
     * response success result wrapper.
     *
     * @param <T> keyType of data class
     * @return response result
     */
    public static <T> ResultJson<T> success() {
        return success(null);
    }

    /**
     * response success result wrapper.
     *
     * @param data response data
     * @param <T>  keyType of data class
     * @return response result
     */
    public static <T> ResultJson<T> success(T data) {
        return build(ResponseEnum.SUCCESS, data);
    }

    /**
     * response error result wrapper.
     *
     * @param message error message
     * @param <T>     keyType of data class
     * @return response result
     */
    public static <T extends Serializable> ResultJson<T> fail(String message) {
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
    public static <T> ResultJson<T> fail(String message, T data) {
        return build(ResponseEnum.FAIL.getCode(), message, data);
    }

    /**
     * 通过返回结果接口构建返回对象
     */
    public static <T> ResultJson<T> build(IResult res, T data) {
        return build(res.getCode(), res.getMessage(), data);
    }

    /**
     * 构建返回对象
     */
    public static <T> ResultJson<T> build(String code, String message, T data) {
        return new ResultJson(code, message, data);
    }

}
