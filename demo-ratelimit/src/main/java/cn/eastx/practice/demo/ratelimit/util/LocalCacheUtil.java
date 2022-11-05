package cn.eastx.practice.demo.ratelimit.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
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
                            // 缓存最大长度
                            .maximumSize(100)
                            // 全局最大缓存5分钟
                            .expireAfterWrite(5, TimeUnit.MINUTES)
                            .build();
                }
            }
        }

        return cache;
    }

}
