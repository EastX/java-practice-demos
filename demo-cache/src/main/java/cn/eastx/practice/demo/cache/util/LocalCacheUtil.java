package cn.eastx.practice.demo.cache.util;

import cn.eastx.practice.common.util.GeneralUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
    /**
     * 内部KEY
     */
    private static String INNER_KEY = "L1:INNER:KEY";

    private LocalCacheUtil() {}

    /**
     * 根据 key 获取值，不存在返回null
     *
     * @param key 缓存Key
     * @return 缓存数据
     * @see LocalCacheUtil#getCache() DCL获取缓存实例，注意存在时长限制
     */
    @Nullable
    public static Object get(String key) {
        return Optional.ofNullable(getCache().getIfPresent(key))
                .map(dataCache -> dataCache.getIfPresent(INNER_KEY))
                .orElse(null);
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

        Map<String, Cache<String, Object>> dcMap = getCache().getAllPresent(keys);

        Map<String, Object> resultMap = Maps.newHashMapWithExpectedSize(dcMap.size());
        for (Map.Entry<String, Cache<String, Object>> entry : dcMap.entrySet()) {
            Object val = Optional.ofNullable(entry.getValue())
                    .map(dc -> dc.getIfPresent(INNER_KEY)).orElse(null);
            if (val != null) {
                resultMap.put(entry.getKey(), val);
            }
        }

        return resultMap;
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

        getCache().put(key, buildInnerCache(value, duration));
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

        Cache<String, Object> dataCache = buildInnerCache(value, duration);

        Map<String, Cache<String, Object>> cacheMap = Maps.newHashMapWithExpectedSize(keys.size());
        for (String key : keys) {
            cacheMap.put(key, dataCache);
        }

        getCache().putAll(cacheMap);
    }

    /**
     * 构建内部缓存
     *
     * @param value 缓存值
     * @param duration 时长，单位：秒
     * @return 内部缓存对象
     */
    private static Cache<String, Object> buildInnerCache(Object value, long duration) {
        Cache<String, Object> dataCache = Caffeine.newBuilder()
                .maximumSize(1).expireAfterWrite(duration, TimeUnit.SECONDS).build();
        dataCache.put(INNER_KEY, value);
        return dataCache;
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
