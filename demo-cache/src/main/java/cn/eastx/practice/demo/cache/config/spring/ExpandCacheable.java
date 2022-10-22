package cn.eastx.practice.demo.cache.config.spring;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 自定义缓存注解
 *  扩展 @Cacheable ，支持 Spring Cache 处理
 *  默认自定义缓存管理器
 *      {@link ExpandRedisConfig#expandRedisCacheManager(RedisConnectionFactory)}
 *      {@link ExpandRedisCacheManager}
 *      指定缓存时长 {@link cn.eastx.practice.demo.cache.util.TimeUtil#parseDuration(String)} ，注意会增加随机值防止缓存雪崩问题
 *  默认自定义 key 生成器
 *      {@link ExpandKeyGenerator}
 *
 * @author EastX
 * @date 2022/10/22
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Cacheable
public @interface ExpandCacheable {

    /**
     * 指定缓存空间的名称，不同缓存空间的数据是彼此隔离的
     */
    @AliasFor(annotation = Cacheable.class, attribute = "value")
    String[] value() default {};
    /**
     * 指定缓存空间的名称，不同缓存空间的数据是彼此隔离的
     */
    @AliasFor(annotation = Cacheable.class, attribute = "cacheNames")
    String[] cacheNames() default {};
    /**
     * 同一个cacheNames中通过key区别不同的缓存。
     *  指定要按照 SpEL 表达式编写，如果不指定，则缺省按照方法的所有参数进行组合
     *  示例：@CachePut(value = “demo”, key = “‘user’+#user.id”)，字符串中spring表达式意外的字符串部分需要用单引号
     */
    @AliasFor(annotation = Cacheable.class, attribute = "key")
    String key() default "";
    /**
     * key 的生成器
     *  SpringCache 默认使用 SimpleKeyGenerator，默认情况下将参数值作为键，但是可能会导致key重复出现，因此一般需要自定义key的生成策略
     *  此处指定默认生成策略为自定义 {@link ExpandRedisConfig#expandKeyGenerator()}
     */
    @AliasFor(annotation = Cacheable.class, attribute = "keyGenerator")
    String keyGenerator() default ExpandRedisConfig.BEAN_KEY_GENERATOR;
    /**
     * 缓存管理器
     *  此处指定默认为自定义 {@link ExpandRedisConfig#expandRedisCacheManager(RedisConnectionFactory)}
     */
    @AliasFor(annotation = Cacheable.class, attribute = "cacheManager")
    String cacheManager() default ExpandRedisConfig.BEAN_REDIS_CACHE_MANAGER;
    /**
     * 缓存解析器
     */
    @AliasFor(annotation = Cacheable.class, attribute = "cacheResolver")
    String cacheResolver() default "";
    /**
     * 调用方法之前判断条件，满足条件才缓存
     */
    @AliasFor(annotation = Cacheable.class, attribute = "condition")
    String condition() default "";
    /**
     * 调用方法之后判断条件，如果SpEL条件成立，则不缓存
     */
    @AliasFor(annotation = Cacheable.class, attribute = "unless")
    String unless() default "";

    /**
     * 缓存过期之后，如果多个线程同时请求对某个数据的访问，会同时去到数据库，导致数据库瞬间负荷增高。
     *  Spring4.3 为 @Cacheable 注解提供了一个新的参数 “sync”（boolean 类型，默认为 false）。
     *  当设置它为 true时，只有一个线程的请求会去到数据库，其他线程都会等待直到缓存可用。这个设置可以减少对数据库的瞬间并发访问。
     */
    @AliasFor(annotation = Cacheable.class, attribute = "sync")
    boolean sync() default false;

    /**
     * 自定义 SpEL key
     *
     * @see ExpandRedisConfig#expandKeyGenerator() 注入自定义缓存 key 生成器
     * @see ExpandKeyGenerator 自定义缓存 key 生成器
     */
    String spelKey() default "";

    /**
     * 自定义缓存过期时间数值
     *
     * @see ExpandRedisConfig#expandCacheExpireConfig() 注入注解初始化 Bean
     * @see ExpandCacheExpireConfig 自定义缓存注解过期时间配置初始化
     */
    long timeout() default 5;

    /**
     * 自定义缓存过期时间单位
     *
     * @see ExpandRedisConfig#expandCacheExpireConfig() 注入注解初始化 Bean
     * @see ExpandCacheExpireConfig 自定义缓存注解过期时间配置初始化
     */
    TimeUnit unit() default TimeUnit.MINUTES;

}
