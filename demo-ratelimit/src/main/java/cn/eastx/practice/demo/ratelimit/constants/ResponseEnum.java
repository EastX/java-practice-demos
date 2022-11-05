package cn.eastx.practice.demo.ratelimit.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回枚举
 *
 * @author EastX
 * @date 2022/10/20
 */
@Getter
@AllArgsConstructor
public enum ResponseEnum {
    /** 默认状态，成功失败 */
    SUCCESS("200", "success"),
    FAIL("500", "failed"),

    /** HTTP状态码 */
    HTTP_STATUS_200("200", "ok"),
    HTTP_STATUS_400("400", "request error"),
    HTTP_STATUS_401("401", "no authentication"),
    HTTP_STATUS_403("403", "no authorities"),
    HTTP_STATUS_500("500", "server error"),

    /** 业务定义错误码 */
    SERVER_ERROR("B0001", "服务异常"),
    RATE_LIMITED("B0002", "请求太多了，请稍后重试"),
    ;

    /**
     * response code
     */
    private final String code;

    /**
     * description.
     */
    private final String description;

}
