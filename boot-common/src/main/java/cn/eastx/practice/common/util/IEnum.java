package cn.eastx.practice.common.util;

import java.util.Objects;

/**
 * 枚举接口
 *
 * @author EastX
 * @date 2023/08/19
 */
public interface IEnum<T> {

    /**
     * 获取编码
     */
    T getCode();

    /**
     * 获取信息
     */
    default String getInfo() {
        return null;
    }

    /**
     * 校验编码
     *
     * @param code 编码
     * @return 是否相等
     */
    default boolean verifyCode(T code) {
        return Objects.equals(code, this.getCode());
    }

}
