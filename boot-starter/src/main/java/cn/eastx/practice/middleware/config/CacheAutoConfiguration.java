package cn.eastx.practice.middleware.config;

import cn.eastx.practice.middleware.aspect.MethodCacheAspect;
import cn.eastx.practice.middleware.util.JsonUtil;
import cn.eastx.practice.middleware.util.RedisLockUtil;
import cn.eastx.practice.middleware.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 缓存相关自动配置
 *
 * @author EastX
 * @date 2023/3/19
 */
@Configuration
@EnableConfigurationProperties({CacheProperties.class})
public class CacheAutoConfiguration {

    /**
     * Redis 模板实例
     */
    @Bean("jacksonRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper om = JsonUtil.createJacksonObjectMapper();

        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(om);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis 工具类实例
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisUtil redisUtil(RedisTemplate<String, Object> jacksonRedisTemplate) {
        return new RedisUtil(jacksonRedisTemplate);
    }

    /**
     * Redis 锁工具类实例
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisLockUtil redisLockUtil(RedisTemplate<String, Object> jacksonRedisTemplate) {
        return new RedisLockUtil(jacksonRedisTemplate);
    }

    /**
     * 方法缓存 AOP 切面
     */
    @Bean
    public MethodCacheAspect methodCacheAspect() {
        return new MethodCacheAspect();
    }

}
