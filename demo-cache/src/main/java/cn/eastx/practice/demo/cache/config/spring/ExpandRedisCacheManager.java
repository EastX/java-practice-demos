package cn.eastx.practice.demo.cache.config.spring;

import cn.eastx.practice.demo.cache.util.TimeUtil;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.Map;

/**
 * 自定义 Redis 缓存管理器
 *  支持指定不同的缓存时长
 *  参考：
 *      随机失效时间 https://blog.csdn.net/yang_wen_wu/article/details/120348727
 *      支持设置缓存时长 https://juejin.cn/post/7062155187200196644
 *
 * @author EastX
 * @date 2022/10/22
 */
public class ExpandRedisCacheManager extends RedisCacheManager {

    /**
     * 缓存空间名称中 实际名称与缓存时长分隔符
     */
    private static final String NAME_SPLIT_SYMBOL = "#";

    /**
     * 最小随机失效时间，单位秒
     */
    private final int minRandomSecond;
    /**
     * 最大随机失效时间，单位秒
     */
    private final int maxRandomSecond;

    private RedisCacheWriter cacheWriter;

    public ExpandRedisCacheManager(RedisCacheWriter cacheWriter,
                                   RedisCacheConfiguration defaultCacheConfiguration,
                                   Map<String, RedisCacheConfiguration> initialCacheConfigurations,
                                   int minRandomSecond, int maxRandomSecond) {
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations, true);
        this.cacheWriter = cacheWriter;
        this.minRandomSecond = minRandomSecond;
        this.maxRandomSecond = maxRandomSecond;
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        String theName = name;
        if (name.contains(NAME_SPLIT_SYMBOL)) {
            // 名称中存在#标记，修改实际名称，替换默认配置的缓存时长为指定缓存时长
            String[] nameArr = name.split(NAME_SPLIT_SYMBOL);
            theName = nameArr[0];
            Duration duration = TimeUtil.parseDuration(nameArr[1]);
            if (duration != null) {
                cacheConfig = cacheConfig.entryTtl(duration);
            }
        }

        return new ExpandRedisCache(theName, cacheWriter, cacheConfig, minRandomSecond, maxRandomSecond);
    }

}
