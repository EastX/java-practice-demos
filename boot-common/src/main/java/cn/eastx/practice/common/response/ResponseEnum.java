package cn.eastx.practice.common.response;

/**
 * 返回枚举
 *
 * @author EastX
 * @date 2022/10/20
 */
public enum ResponseEnum implements IResponseAssert{
    /** 默认状态，成功失败 */
    SUCCESS("200", "success"),
    FAIL("500", "failed"),

    /** HTTP状态码 */
    HTTP_STATUS_200("200", "ok"),
    HTTP_STATUS_400("400", "request error"),
    HTTP_STATUS_401("401", "no authentication"),
    HTTP_STATUS_403("403", "no authorities"),
    HTTP_STATUS_500("500", "server error"),

    /* ---
        错误码为字符串类型，共 5 位，分成两个部分：错误产生来源+四位数字编号。
        错误产生来源分为 A/B/C，
            A 表示错误来源于用户，比如参数错误，用户安装版本过低，用户支付超时等问题；
            B 表示错误来源于当前系统，往往是业务逻辑出错，或程序健壮性差等问题；
            C 表示错误来源于第三方服务，比如 CDN 服务出错，消息投递超时等问题；
            四位数字编号从 0001 到 9999，大类之间的步长间距预留 100
            00 -- 公共返回码
     --- */
    /** 用户错误码 */
    ILLEGAL_ARGUMENT("A0001", "非法参数"),
    FORBIDDEN("A0002", "没有相关权限"),

    /** 当前系统业务错误码 */
    SERVER_ERROR("B0001", "服务异常"),

    /** 第三方服务错误码 */
    REMOTE_ERROR("C0001", "远程服务调用异常:{}"),
    ;

    /**
     * response code，编码
     */
    private final String code;

    /**
     * message. 日志占位符 {}
     */
    private final String message;

    ResponseEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}