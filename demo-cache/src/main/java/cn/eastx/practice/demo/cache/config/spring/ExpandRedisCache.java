package cn.eastx.practice.demo.cache.config.spring;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.NullValue;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * 自定义 Redis 缓存处理器
 *  在 put 方法中随机生成失效时间
 *  参考：https://blog.csdn.net/yang_wen_wu/article/details/120348727
 *
 * @author EastX
 * @date 2022/10/22
 */
@Slf4j
public class ExpandRedisCache extends RedisCache {

    /**
     * 最小随机失效时间，单位秒
     */
    private final int minRandomSecond;
    /**
     * 最大随机失效时间，单位秒
     */
    private final int maxRandomSecond;

    private String name;
    private RedisCacheWriter cacheWriter;
    private RedisCacheConfiguration cacheConfig;

    protected ExpandRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig,
                               int minSecond, int maxSecond) {
        super(name, cacheWriter, cacheConfig);
        if (minSecond <= 0) {
            throw new IllegalArgumentException("minSecond should be bigger than 0");
        }
        if (minSecond > maxSecond) {
            throw new IllegalArgumentException("maxSecond should be bigger than minSecond");
        }
        this.minRandomSecond = minSecond;
        this.maxRandomSecond = maxSecond;
        this.name = name;
        this.cacheWriter = cacheWriter;
        this.cacheConfig = cacheConfig;
    }

    @Override
    public void put(Object key, @Nullable Object value) {

        Object cacheValue = preProcessCacheValue(value);

        if (!isAllowNullValues() && cacheValue == null) {

            throw new IllegalArgumentException(String.format(
                    "Cache '%s' does not allow 'null' values. Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure RedisCache to allow 'null' via RedisCacheConfiguration.",
                    name));
        }

        // 替换父类设置缓存时长处理
        Duration duration = getDynamicDuration(cacheValue);
        cacheWriter.put(name, createAndConvertCacheKey(key),
                serializeCacheValue(cacheValue), duration);
        log.debug("redis put, name={}, key={}, value={}, duration={}", name, key, cacheValue, duration);
    }


    @Override
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {

        Object cacheValue = preProcessCacheValue(value);

        if (!isAllowNullValues() && cacheValue == null) {
            return get(key);
        }

        // 替换父类设置缓存时长处理
        Duration duration = getDynamicDuration(cacheValue);
        byte[] result = cacheWriter.putIfAbsent(name, createAndConvertCacheKey(key),
                serializeCacheValue(cacheValue), duration);
        log.debug("redis putIfAbsent, name={}, key={}, value={}, duration={}", name, key, cacheValue, duration);

        if (result == null) {
            return null;
        }

        return new SimpleValueWrapper(fromStoreValue(deserializeCacheValue(result)));
    }

    /**
     * 参照父类实现
     * @see RedisCache#createAndConvertCacheKey(Object)
     *
     * @param key 缓存 key
     * @return 序列化后的缓存 key 的字节数组
     */
    private byte[] createAndConvertCacheKey(Object key) {
        return super.serializeCacheKey(super.createCacheKey(key));
    }

    /**
     * 获取动态时长
     *
     * @param cacheValue 缓存值
     */
    private Duration getDynamicDuration(Object cacheValue) {
        // 如果缓存值为 null，固定返回时长为 30s 避免缓存穿透
        if (NullValue.INSTANCE.equals(cacheValue)) {
            return Duration.ofSeconds(30);
        }

        int randomInt = RandomUtil.randomInt(this.minRandomSecond, this.maxRandomSecond);
        return this.cacheConfig.getTtl().plus(Duration.ofSeconds(randomInt));
    }

}
