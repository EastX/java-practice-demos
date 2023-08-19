package cn.eastx.practice.common.util;

import cn.eastx.practice.common.exception.BaseException;

/**
 * 断言接口
 *  default 方法用于定义默认断言
 *
 * @author EastX
 * @date 2023/08/19
 */
public interface IAssert {

    /**
     * 创建异常
     *
     * @param msgArgs 消息参数列表
     * @return 异常
     */
    BaseException newException(Object... msgArgs);

    /**
     * 断言对象不为null
     *
     * @param object  断言
     * @param msgArgs 断言成立消息参数
     */
    default void assertNotNull(Object object, Object... msgArgs) {
        assertTrue(null != object);
    }

    /**
     * 断言为真
     *
     * @param flag 标识
     */
    default void assertTrue(Boolean flag, Object... msgArgs) {
        if (!Boolean.TRUE.equals(flag)) {
            throw newException(msgArgs);
        }
    }

}
