package cn.eastx.practice.demo.cache.util;

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
public class RequestUtil {

    /**
     * 获取 HTTP 请求对象
     *
     * @return HTTP 请求对象
     */
    @Nullable
    public static HttpServletRequest getRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(attr -> attr instanceof ServletRequestAttributes)
                .map(attr -> (ServletRequestAttributes) attr)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);
    }

    private RequestUtil() {}

}
