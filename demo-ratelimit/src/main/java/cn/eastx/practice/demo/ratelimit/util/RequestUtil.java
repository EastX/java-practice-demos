package cn.eastx.practice.demo.ratelimit.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * 请求处理工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
public class RequestUtil {

    private RequestUtil() {}

    /**
     * 获取 HTTP 请求对象
     *
     * @return HTTP 请求对象
     */
    @Nullable
    public static HttpServletRequest getRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);
    }

}
