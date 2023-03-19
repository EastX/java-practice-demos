package cn.eastx.practice.middleware.util;

import cn.eastx.practice.middleware.config.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 本地缓存工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
public class LocalCacheUtil {

    /**
     * 本地缓存单例
     */
    private volatile static Cache<String, Cache<String, Object>> cache;

    private LocalCacheUtil() {}

    /**
     * 根据 key 获取值，不存在返回null
     *
     * @param key 缓存Key
     * @return 缓存数据
     * @see LocalCacheUtil#getCache() DCL获取缓存实例，注意存在时长限制
     */
    @Nullable
    public static Object getIfPresent(String key) {
        return Optional.ofNullable(getCache().getIfPresent(key))
                .map(dataCache -> dataCache.getIfPresent(key))
                .orElse(null);
    }

    /**
     * 设置缓存数据
     *
     * @param key 缓存Key
     * @param value 缓存数据
     * @param duration 缓存时长，单位秒
     * @see LocalCacheUtil#getCache() DCL获取缓存实例，注意存在时长限制
     */
    public static void put(String key, Object value, long duration) {
        Cache<String, Object> dataCache = Caffeine.newBuilder()
                .maximumSize(1).expireAfterWrite(duration, TimeUnit.SECONDS).build();
        dataCache.put(key, value);
        getCache().put(key, dataCache);
    }

    /**
     * 修改缓存失效
     *
     * @param key 缓存key
     * @see LocalCacheUtil#getCache() DCL获取缓存实例，注意存在时长限制
     */
    public static void invalidate(String key) {
        getCache().invalidate(key);
    }

    /**
     * 获取本地缓存所有对象
     *
     * @return 本地缓存所有对象
     */
    public static Map<String, Object> asMap() {
        return getCache().asMap().values().stream()
                .flatMap(item -> item.asMap().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 获取缓存对象
     *  DCL获取缓存实例，注意存在时长限制
     *
     * @return 缓存对象
     */
    private static Cache<String, Cache<String, Object>> getCache() {
        if (cache == null) {
            synchronized (LocalCacheUtil.class) {
                if (cache == null) {
                    cache = Caffeine.newBuilder()
                            // 缓存最大大小
                            .maximumSize(CacheProperties.getLocalCache().getMaximumSize())
                            // 全局最大缓存时长
                            .expireAfterWrite(
                                    CacheProperties.getLocalCache().getExpireAfterWrite(),
                                    TimeUnit.MINUTES)
                            .build();
                }
            }
        }

        return cache;
    }

}
