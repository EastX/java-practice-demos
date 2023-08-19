package cn.eastx.practice.demo.cache.util;

import cn.eastx.practice.common.exception.BaseException;
import cn.eastx.practice.common.exception.BusinessException;
import cn.eastx.practice.common.response.ResponseEnum;
import cn.eastx.practice.common.util.CompressUtil;
import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.common.util.IEnum;
import cn.eastx.practice.common.util.JsonUtil;
import cn.eastx.practice.demo.cache.config.RedisSubscriber;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 两级缓存工具类
 *
 * 一级：本地缓存 {@link LocalCacheUtil}
 * 二级：redis 缓存 {@link RedisUtil}
 *
 * @see RedisSubscriber Redis Pub/Sub ，需要被 Spring IOC 管理方生效
 *
 * @author EastX
 * @date 2023/08/19
 */
public class L2CacheUtil implements RedisSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(L2CacheUtil.class);

    private static final Config DEFAULT_CONFIG = Config.builder().build();

    /** 缓存数据变更发布订阅主题 */
    private static final String DATA_CHANGE_TOPIC = "topic:l2:cache";
    /** 建议最低缓存时长 */
    private static final int MIN_TTL = 30;

    public L2CacheUtil() {}

    /**
     * 根据 key 获取值，不存在返回null
     *
     * @param key 缓存Key
     * @return 缓存数据
     */
    @Nullable
    public static Object get(String key) {
        return get(key, DEFAULT_CONFIG);
    }

    /**
     * 根据 key 获取值，不存在返回null
     *
     * @param key 缓存Key
     * @param config 其它配置
     * @return 缓存数据
     */
    @Nullable
    public static Object get(String key, Config config) {
        Object val = getStoreVal(key, config);
        return convertCacheData(val, config);
    }

    /**
     * 根据 key 获取存储值
     *
     * @param key 缓存Key
     * @param config 其它配置
     * @return 缓存数据
     */
    @Nullable
    public static Object getStoreVal(String key, Config config) {
        if (!config.isUseL1()) {
            // 不使用L1本地缓存
            return redisTemplate().opsForValue().get(key);
        }

        // 使用 L1 本地缓存 + L2 Redis 缓存
        Object val = LocalCacheUtil.get(key);
        if (val != null) {
            return val;
        }

        val = redisTemplate().opsForValue().get(key);
        if (val != null) {
            Long ttl = redisTemplate().getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > MIN_TTL) {
                LocalCacheUtil.set(key, val, ttl - MIN_TTL);
            }
        }

        return val;
    }

    /**
     * 设置缓存数据
     *
     * @param key 缓存Key
     * @param value 缓存数据
     * @param duration 缓存时长，单位秒

     */
    public static void set(String key, Object value, long duration) {
        set(key, value, duration, DEFAULT_CONFIG);
    }

    /**
     * 设置缓存数据
     *
     * @param key 缓存Key
     * @param value 缓存数据
     * @param duration 缓存时长，单位秒
     * @param config 是否压缩
     */
    public static void set(String key, Object value, long duration, Config config) {
        Object cacheVal = null;

        // 缓存特殊值处理
        long durationL1 = config.isUseL1() ? config.getDurationL1() : 0L;
        for (SpecialVal valEnum : SpecialVal.values()) {
            if (valEnum.canStore(value)) {
                cacheVal = valEnum.storeVal(value);
                duration = valEnum.storeDuration();
                durationL1 = duration;
                break;
            }
        }

        // 非特殊值处理
        if (cacheVal == null) {
            // 是否压缩判断
            cacheVal = config.isCompress() ? CompressUtil.compressObj(value) : value;
        }

        if (config.isUseL1()) {
            LocalCacheUtil.set(key, cacheVal, durationL1);
            // 通过发布订阅通知数据变更清除本地缓存
            redisTemplate().convertAndSend(DATA_CHANGE_TOPIC, key);
        }

        redisTemplate().opsForValue().set(key, cacheVal, Duration.ofSeconds(duration));
    }

    /**
     * 修改缓存失效
     *
     * @param key 缓存key
     */
    public static void delete(String key) {
        LocalCacheUtil.delete(key);
        redisTemplate().delete(key);
        // 通过发布订阅通知数据变更清除本地缓存
        redisTemplate().convertAndSend(DATA_CHANGE_TOPIC, key);
    }

    @Override
    public String getTopic() {
        return DATA_CHANGE_TOPIC;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 使用 redis convertAndSend发布消息，订阅者获取字符串字节必须要反序列
        String cacheKey = (String) redisTemplate().getValueSerializer().deserialize(message.getBody());
        // 清除本地缓存
        logger.debug("[L2CacheUtil]清除本地缓存 START, cacheKey={}, value={}",
                cacheKey, JsonUtil.toSimpleStr(LocalCacheUtil.get(cacheKey)));

        LocalCacheUtil.delete(cacheKey);

        logger.debug("[L2CacheUtil]清除本地缓存 END, cacheKey={}, value IS NULL={}",
                cacheKey, LocalCacheUtil.get(cacheKey) == null);
    }

    /**
     * 转换缓存数据
     *
     * @param data 缓存数据
     * @return 实际返回数据
     */
    public static Object convertCacheData(Object data, Config config) {
        if (!(data instanceof String)) {
            return data;
        }

        String dataStr = (String) data;

        // 缓存特殊值处理
        for (SpecialVal valEnum : SpecialVal.values()) {
            if (valEnum.canConvert(dataStr)) {
                return valEnum.convertVal(dataStr);
            }
        }

        // 非特殊值处理
        if (!config.isCompress()) {
            // 不开启对象压缩
            return data;
        }

        return CompressUtil.uncompressObj(dataStr, Object.class);
    }

    /**
     * Redis 模板
     */
    private static RedisTemplate<String, Object> redisTemplate() {
        return RedisUtil.defTemplate();
    }

    /**
     * 两级缓存配置
     * 默认：使用本地缓存60s、开启对象压缩
     */
    @Getter
    @Builder
    public static class Config {
        /**
         * 是否使用L1缓存（本地缓存）
         */
        @Builder.Default
        private boolean useL1 = true;

        /**
         * L1缓存时长（本地缓存），单位秒
         */
        @Builder.Default
        private long durationL1 = 60L;

        /**
         * 是否开启对象压缩
         */
        @Builder.Default
        private boolean compress = true;
    }

    @Getter
    @AllArgsConstructor
    enum SpecialVal implements IEnum<String> {
        /** NULL 对象 */
        NULL("NULL_VALUE", "null对象", "") {
            @Override
            public boolean canStore(Object originVal) {
                return originVal == null;
            }

            @Override
            public boolean canConvert(String storeVal) {
                return Objects.equals(storeVal, this.getCode());
            }

            @Override
            public Object convertVal(String storeVal) {
                return null;
            }
        },
        /** 异常对象 */
        THROWABLE("BASE_EXCEPTION_", "异常对象", "{CODE}_{MSG}") {
            @Override
            public boolean canStore(Object originVal) {
                return originVal instanceof Throwable;
            }

            @Override
            public String storeVal(Object originVal) {
                String code = ResponseEnum.SERVER_ERROR.getCode();
                String msg = GeneralUtil.formatMsg(ResponseEnum.SERVER_ERROR.getMessage(), "L2Cache");
                if (originVal instanceof BaseException) {
                    BaseException be = (BaseException) originVal;
                    code = be.getCode();
                    msg = be.getMsg();
                }

                return this.getCode() + this.getTemplate().replace("{CODE}", code).replace("{MSG}", msg);
            }

            @Override
            public Object convertVal(String storeVal) {
                String[] tmpArr = storeVal.replace(this.getCode(), "").split("_");
                throw new BusinessException(tmpArr[0], tmpArr[1]);
            }
        },
        ;

        private String code;
        private String info;
        private String template;

        /**
         * 转换存储数据
         */
        public String storeVal(Object originVal) {
            return this.getCode();
        }

        /**
         * 缓存时长，单位秒
         */
        public int storeDuration() {
            return MIN_TTL;
        }

        public boolean canConvert(String storeVal) {
            return storeVal != null && storeVal.startsWith(this.getCode());
        }

        public abstract boolean canStore(Object originVal);
        public abstract Object convertVal(String storeVal);
    }

}