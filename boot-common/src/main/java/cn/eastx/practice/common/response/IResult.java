package cn.eastx.practice.common.response;

import java.util.Objects;

/**
 * 返回结果接口
 *
 * @author EastX
 * @date 2023/08/19
 */
public interface IResult {

    /**
     * 返回码
     */
    String getCode();

    /**
     * 返回信息
     */
    String getMessage();

    /**
     * 校验返回码
     */
    default boolean verifyCode(String code) {
        return Objects.equals(getCode(), code);
    }

}
