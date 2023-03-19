package cn.eastx.practice.middleware.config;

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

    /**
     * 本地缓存配置
     */
    private static LocalCache localCache = new LocalCache();

    public static LocalCache getLocalCache() {
        return localCache;
    }

    public void setLocalCache(LocalCache localCache) {
        CacheProperties.localCache = localCache;
        logger.debug("[middlewareProperties]localCache={}", localCache);
    }

    /**
     * 本地缓存
     */
    public static class LocalCache {
        /**
         * 全局最大大小
         */
        private Integer maximumSize;
        /**
         * 全局最大缓存时长（写后过期时长），单位分钟
         */
        private Integer expireAfterWrite;

        public LocalCache() {
            this.maximumSize = 100;
            this.expireAfterWrite = 5;
        }

        public Integer getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(Integer maximumSize) {
            this.maximumSize = maximumSize;
        }

        public Integer getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(Integer expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        @Override
        public String toString() {
            return "LocalCache{" +
                    "maximumSize=" + maximumSize +
                    ", expireAfterWrite=" + expireAfterWrite +
                    '}';
        }
    }

}
