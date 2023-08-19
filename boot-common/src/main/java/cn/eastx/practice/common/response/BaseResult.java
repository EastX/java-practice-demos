package cn.eastx.practice.common.response;

import cn.eastx.practice.common.util.GeneralUtil;

/**
 * 基础返回对象
 *
 * @author EastX
 * @date 2023/08/19
 */
public class BaseResult implements IResult, IResponseAssert {

    /**
     * response code.
     */
    private String code;

    /**
     * response message.
     */
    private String message;

    public BaseResult() {}

    public BaseResult(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public BaseResult(IResult res) {
        this.setCode(res.getCode());
        this.setMessage(res.getMessage());
    }

    /**
     * 构建返回结果
     *
     * @param res 返回
     * @param messageArgs 消息参数
     * @return 返回结果
     */
    public static BaseResult buildResult(IResult res, Object... messageArgs) {
        BaseResult result = new BaseResult(res);
        result.setMessage(GeneralUtil.formatMsg(result.getMessage(), messageArgs));
        return result;
    }

    /**
     * 替换状态码
     *
     * @param res 返回数据
     */
    public void replaceRes(IResult res) {
        this.setCode(res.getCode());
        this.setMessage(res.getMessage());
    }

    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "BaseResult{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
