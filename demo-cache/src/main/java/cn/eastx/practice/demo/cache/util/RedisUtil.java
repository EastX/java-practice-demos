package cn.eastx.practice.demo.cache.util;

import cn.eastx.practice.common.response.ResponseEnum;
import org.springframework.data.redis.core.*;

/**
 * Redis工具类
 *  参考：https://github.com/iyayu/RedisUtil
 *
 * @author EastX
 * @date 2022/10/20
 */
public class RedisUtil {

    private static RedisTemplate<String, Object> redisTemplate;
    private static StringRedisTemplate strRedisTemplate;

    private RedisUtil() {}

    /**
     * 初始化默认 RedisTemplate
     * <p>
     * 通过这样的方式，使用 Spring 创建的 Bean
     *
     * @param redisTemplate Redis模板
     */
    public static void initDefTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
    }

    /**
     * 初始化默认 StringRedisTemplate
     * <p>
     * 通过这样的方式，使用 Spring 创建的 Bean
     *
     * @param strRedisTemplate Redis模板
     */
    public static void initStrTemplate(StringRedisTemplate strRedisTemplate) {
        RedisUtil.strRedisTemplate = strRedisTemplate;
    }

    /**
     * 获取 默认 RedisTemplate
     */
    public static RedisTemplate<String, Object> defTemplate() {
        ResponseEnum.SERVER_ERROR.assetNotNull(redisTemplate, "[RedisUtil]未初始化 redisTemplate");
        return redisTemplate;
    }

    /**
     * 获取 StringRedisTemplate
     */
    public static StringRedisTemplate strTemplate() {
        ResponseEnum.SERVER_ERROR.assetNotNull(strRedisTemplate, "[RedisUtil]未初始化 strRedisTemplate");
        return strRedisTemplate;
    }

    /* ------------------------- ops 相关(默认) ------------------------- */

    /**
     * Returns the operations performed on simple values (or Strings in Redis terminology).
     *
     * @return value operations
     */
    public static ValueOperations<String, Object> opsValue() {
        return defTemplate().opsForValue();
    }

    /**
     * Returns the operations performed on list values.
     *
     * @return list operations
     */
    public static ListOperations<String, Object> opsList() {
        return defTemplate().opsForList();
    }

    /**
     * Returns the operations performed on set values.
     *
     * @return set operations
     */
    public static SetOperations<String, Object> opsSet() {
        return defTemplate().opsForSet();
    }

    /**
     * Returns the operations performed on hash values.
     *
     * @return hash operations
     */
    public static HashOperations<String, String, Object> opsHash() {
        return defTemplate().opsForHash();
    }

    /**
     * Returns the operations performed on zset values (also known as sorted sets).
     *
     * @return zset operations
     */
    public static ZSetOperations<String, Object> opsZSet() {
        return defTemplate().opsForZSet();
    }

    /* ------------------------- ops 相关(StringRedisTemplate) ------------------------- */

    /**
     * Returns the operations performed on simple values (or Strings in Redis terminology).
     *
     * @return value operations
     */
    public static ValueOperations<String, String> strOpsValue() {
        return strTemplate().opsForValue();
    }

    /**
     * Returns the operations performed on list values.
     *
     * @return list operations
     */
    public static ListOperations<String, String> strOpsList() {
        return strTemplate().opsForList();
    }

    /**
     * Returns the operations performed on set values.
     *
     * @return set operations
     */
    public static SetOperations<String, String> strOpsSet() {
        return strTemplate().opsForSet();
    }

    /**
     * Returns the operations performed on hash values.
     *
     * @return hash operations
     */
    public static HashOperations<String, String, String> strOpsHash() {
        return strTemplate().opsForHash();
    }

    /**
     * Returns the operations performed on zset values (also known as sorted sets).
     *
     * @return zset operations
     */
    public static ZSetOperations<String, String> strOpsZSet() {
        return strTemplate().opsForZSet();
    }

}
