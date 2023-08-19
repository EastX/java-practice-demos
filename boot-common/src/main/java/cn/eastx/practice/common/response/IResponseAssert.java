package cn.eastx.practice.common.response;

import cn.eastx.practice.common.exception.BaseException;
import cn.eastx.practice.common.exception.ResponseException;
import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.common.util.IAssert;

import java.util.Objects;

/**
 * 返回断言
 *  此类 default 方法用于定义有关 IResult 的断言
 *
 * @see IAssert 断言接口定义默认断言
 * @see IResult 返回结果接口定义状态码、提示信息
 *
 * @author EastX
 * @date 2023/08/19
 */
public interface IResponseAssert extends IAssert, IResult {

    @Override
    default BaseException newException(Object... msgArgs) {
        return new ResponseException(this.getCode(), GeneralUtil.formatMsg(this.getMessage(), msgArgs));
    }

    /**
     * 校验是否为正常返回
     */
    default boolean verifySuccess() {
        return Objects.equals(getCode(), ResponseEnum.SUCCESS.getCode());
    }

    /**
     * 断言是成功状态
     *
     * @param msgArgs 消息参数列表
     */
    default void assertSuccess(Object... msgArgs) {
        if (!this.verifySuccess()) {
            throw newException(msgArgs);
        }
    }

}
