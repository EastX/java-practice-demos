package cn.eastx.practice.middleware.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存相关配置
 *
 * @author EastX
 * @date 2023/3/19
 */
@ConfigurationProperties(prefix = "practice.middleware.cache")
public class CacheProperties {

    private Logger logger = LoggerFactory.getLogger(CacheProperties.class);

    /** 本地缓存最大大小 */
    private static Integer localMaxSize = 500;
    /** 本地缓存最长过期时间，单位分钟 */
    private static Integer localMaxTtl = 5;

    public static Integer getLocalMaxSize() {
        return localMaxSize;
    }

    public void setLocalMaxSize(Integer localMaxSize) {
        CacheProperties.localMaxSize = localMaxSize;
    }

    public static Integer getLocalMaxTtl() {
        return localMaxTtl;
    }

    public void setLocalMaxTtl(Integer localMaxTtl) {
        CacheProperties.localMaxTtl = localMaxTtl;
    }
}
