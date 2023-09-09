package cn.eastx.practice.demo.cache.util;

import cn.eastx.practice.common.util.GeneralUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
    private static volatile Cache<String, Object> cache;
    /** 最大缓存数量 */
    private static final int MAX_NUM = 100;
    /** 最大过期时长 */
    private static final long MAX_TTL = TimeUnit.MINUTES.toMillis(5);

    private LocalCacheUtil() {}

    /**
     * 获取缓存对象
     *  DCL获取缓存实例，注意存在时长限制
     *
     * @return 缓存对象
     */
    public static Cache<String, Object> getCache() {
        if (cache == null) {
            synchronized (LocalCacheUtil.class) {
                if (cache == null) {
                    cache = Caffeine.newBuilder()
                            // 缓存最大长度
                            .maximumSize(MAX_NUM)
                            // 自定义缓存过期策略
                            .expireAfter(new CacheExpiry())
                            .build();
                }
            }
        }

        return cache;
    }

    /**
     * 根据 key 获取值，不存在返回null
     *
     * @param key 缓存Key
     * @return 缓存数据
     * @see LocalCacheUtil#getCache() DCL获取缓存实例，注意存在时长限制
     */
    @Nullable
    public static Object get(String key) {
        return getCache().getIfPresent(key);
    }

    /**
     * 获取 key 与 值 对应Map
     *
     * @param keys 缓存Key集合
     * @return key 与 值 对应Map
     */
    public static Map<String, Object> get(Collection<String> keys) {
        if (GeneralUtil.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        return getCache().getAllPresent(keys);
    }

    /**
     * 设置缓存数据
     *
     * @param key 缓存Key
     * @param value 缓存数据
     * @param duration 缓存时长，单位秒
     */
    public static void set(String key, Object value, long duration) {
        if (GeneralUtil.isEmpty(key)) {
            return;
        }

        Duration ttl = Duration.ofSeconds(Math.min(duration, MAX_TTL));
        getCache().policy().expireVariably().ifPresent(e ->
                e.put(key, value, ttl));
    }

    /**
     * 设置缓存数据
     *
     * @param keys 缓存Key集合
     * @param value 缓存数据
     * @param duration 缓存时长，单位秒
     */
    public static void set(Collection<String> keys, Object value, long duration) {
        if (GeneralUtil.isEmpty(keys)) {
            return;
        }

        Duration ttl = Duration.ofSeconds(Math.min(duration, MAX_TTL));
        getCache().policy().expireVariably().ifPresent(e -> {
            for (String key : keys) {
                e.put(key, value, ttl);
            }
        });
    }

    /**
     * 删除缓存
     *
     * @param key 缓存key
     * @see LocalCacheUtil#getCache() DCL获取缓存实例，注意存在时长限制
     */
    public static void delete(String key) {
        getCache().invalidate(key);
    }

    /**
     * 缓存过期策略
     *
     * https://blog.csdn.net/AdobePeng/article/details/127773031
     * https://www.cnblogs.com/victorbu/p/17495469.html
     */
    static class CacheExpiry implements Expiry<String, Object> {

        /**
         * 创建后过期策略
         *
         * @param currentTime 当前时间往后算，多长时间过期，单位：纳秒
         * @return 剩余多长时间过期，单位：纳秒
         */
        @Override
        public long expireAfterCreate(@NonNull String key, @NonNull Object value, long currentTime) {
            log.debug("[CacheExpiry]expireAfterCreate, key={}, value={}, time={}",
                    key, value, currentTime);
            return MAX_TTL;
        }

        /**
         * 更新后过期策略
         *
         * @param currentTime     当前时间往后算，多长时间过期，单位：纳秒
         * @param currentDuration 剩余多长时间过期，单位：纳秒
         * @return 剩余多长时间过期，单位：纳秒
         */
        @Override
        public long expireAfterUpdate(@NonNull String key, @NonNull Object value,
                                      long currentTime, @NonNegative long currentDuration) {
            log.debug("[CacheExpiry]expireAfterUpdate, key={}, value={}, time={}, duration={}",
                    key, value, currentTime, currentDuration);
            return currentDuration;
        }

        /**
         * 读取后过期策略
         *
         * @param currentTime     当前时间往后算，多长时间过期，单位：纳秒
         * @param currentDuration 剩余多长时间过期，单位：纳秒
         * @return 剩余多长时间过期，单位：纳秒
         */
        @Override
        public long expireAfterRead(@NonNull String key, @NonNull Object value, long currentTime,
                                    @NonNegative long currentDuration) {
            log.debug("[CacheExpiry]expireAfterRead, key={}, value={}, time={}, duration={}",
                    key, value, currentTime, currentDuration);
            return currentDuration;
        }

    }

}
