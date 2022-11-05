package cn.eastx.practice.demo.cache.config.spring;

import cn.eastx.practice.demo.cache.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Cache 实现 Redis 配置
 *  参考：https://blog.csdn.net/user2025/article/details/106595257
 *
 * @author EastX
 * @date 2022/10/22
 */
@Configuration
@EnableCaching
public class ExpandRedisConfig {

    /**
     * 自定义 Redis 缓存管理实例 BeanName
     */
    public static final String BEAN_REDIS_CACHE_MANAGER = "expandRedisCacheManager";
    /**
     * 自定义缓存的 key 生成器实例 BeanName
     */
    public static final String BEAN_KEY_GENERATOR = "expandKeyGenerator";

    /**
     * ${expand-cache-config.ttl-map} 获取配置文件信息
     * #{} 是 Spring 表达式，获取 Bean 对象属性
     */
    @Value("#{${expand-cache-config.ttl-map:null}}")
    private Map<String, Long> ttlMap;

    /**
     * 自定义 Redis 缓存管理实例
     *
     * @param factory redis 连接工厂
     */
    @Bean(BEAN_REDIS_CACHE_MANAGER)
    public RedisCacheManager expandRedisCacheManager(RedisConnectionFactory factory) {
        /*
            使用 Jackson 作为值序列化处理器
            FastJson 存在部分转换问题如：Set 存储后因为没有对应的类型保存无法转换为 JSONArray（实现 List ） 导致失败
         */
        ObjectMapper om = JsonUtil.createJacksonObjectMapper();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(om);

        // 配置key、value 序列化（解决乱码的问题）
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // key 使用 string 序列化方式
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
                // value 使用 jackson 序列化方式
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                // 配置缓存空间名称前缀
                .prefixCacheNameWith("spring:cache:")
                // 配置全局缓存过期时间
                .entryTtl(Duration.ofMinutes(30L));
        // 专门指定某些缓存空间的配置，如果过期时间，这里的 key 为缓存空间名称
        Set<Map.Entry<String, Long>> entrySet =
                Optional.ofNullable(ttlMap).map(Map::entrySet).orElse(Collections.emptySet());
        Map<String, RedisCacheConfiguration> configMap =
                Maps.newHashMapWithExpectedSize(entrySet.size());
        // 代码写死示例
        configMap.put("world", config.entryTtl(Duration.ofSeconds(60)));
        for (Map.Entry<String, Long> entry : entrySet) {
            // 指定特定缓存空间对应的过期时间
            configMap.put(entry.getKey(), config.entryTtl(Duration.ofSeconds(entry.getValue())));
        }

        RedisCacheWriter redisCacheWriter = RedisCacheWriter.lockingRedisCacheWriter(factory);
        // 使用自定义缓存管理器附带自定义参数随机时间，注意此处为全局设定，5-最小随机秒，30-最大随机秒
        return new ExpandRedisCacheManager(redisCacheWriter, config, configMap, 5, 30);
    }

    /**
     * 自定义缓存的 key 生成器实例
     *  注意: 该方法只是声明了 key 的生成策略，需在 @Cacheable 注解中通过 keyGenerator 属性指定具体的key生成策略
     *  示例: @Cacheable(value = "key", keyGenerator = "cacheKeyGenerator")
     * 可以根据业务情况，配置不同的生成策略
     */
    @Bean(BEAN_KEY_GENERATOR)
    public ExpandKeyGenerator expandKeyGenerator() {
        return new ExpandKeyGenerator();
    }

    /**
     * Spring Cache 过期时间配置初始化
     *  注意：反射执行获取 @Component 注解的 Bean 进行配置缓存时间的修改
     */
    @Bean("expandCacheExpireConfig")
    public ExpandCacheExpireConfig expandCacheExpireConfig() {
        return new ExpandCacheExpireConfig();
    }

}
