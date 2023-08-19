package cn.eastx.practice.demo.cache.config;

import cn.eastx.practice.common.response.ResponseEnum;
import cn.eastx.practice.common.util.GeneralUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存相关配置
 */
@ConfigurationProperties(prefix = "mc")
@Configuration
public class MemcacheProperties {

    private static String host;
    private static Integer port;

    public static void check() {
        ResponseEnum.SERVER_ERROR.assertTrue(!GeneralUtil.hasEmpty(host, port), "未配置mc缓存");
    }

    public static String getHost() {
        return host;
    }

    public void setHost(String host) {
        MemcacheProperties.host = host;
    }

    public static Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        MemcacheProperties.port = port;
    }

}
