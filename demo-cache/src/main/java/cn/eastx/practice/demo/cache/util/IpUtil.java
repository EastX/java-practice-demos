package cn.eastx.practice.demo.cache.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

/**
 * IP 工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
public class IpUtil {

    /**
     * 涉及 IP 的请求头
     */
    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };
    /**
     * 本地请求 IP
     */
    private static final String[] LOCAL_IP = {"127.0.0.1", "0:0:0:0:0:0:0:1"};
    /**
     * 多个 IP 分隔符号
     */
    private static final String IP_SPLIT_SYMBOL = ",";

    private IpUtil() {}

    /**
     * 获取请求的 IP 地址
     *
     * @return 请求的 IP 地址，未获取到正常的 IP 地址返回空字符串
     */
    public static String getIpAddr() {
        HttpServletRequest request = RequestUtil.getRequest();
        if (request == null) {
            return "";
        }

        return Optional.ofNullable(getIpAddr(request)).orElse("");
    }

    /**
     * 获取请求的 IP 地址
     *  出现异常返回 null
     *
     * @param request HTTP请求对象
     * @return 请求的 IP 地址，未获取到正常的IP地址返回null
     */
    @Nullable
    public static String getIpAddr(HttpServletRequest request) {
        Assert.notNull(request, "请求对象为null");

        String ip;
        for (String header : HEADERS_TO_TRY) {
            ip = checkConvertIp(request.getHeader(header));
            if (ip != null) {
                return ip;
            }
        }

        ip = request.getRemoteAddr();
        if (ip != null && Arrays.asList(LOCAL_IP).contains(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                log.error("获取本机Host出现异常, message={}", e.getMessage());
            }
        }

        return checkConvertIp(ip);
    }

    /**
     * 校验并转换 IP 地址
     *
     * @param ip IP 地址
     * @return IP 地址，校验失败返回null
     */
    @Nullable
    private static String checkConvertIp(String ip) {
        if (checkIpIncorrect(ip)) {
            return null;
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        int firstEndIdx;
        if ((firstEndIdx = ip.indexOf(IP_SPLIT_SYMBOL)) > 0) {
            ip = ip.substring(0, firstEndIdx);
        }

        //  对分割后的IP进行判断
        return checkIpIncorrect(ip) ? null : ip.trim();
    }

    /**
     * 校验 IP 地址是否不正确
     *
     * @return IP 地址是否不正确
     */
    private static boolean checkIpIncorrect(String ip) {
        return StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim());
    }

}
