package cn.eastx.practice.demo.ratelimit.config;

import cn.eastx.practice.demo.ratelimit.util.JsonUtil;
import cn.eastx.practice.demo.ratelimit.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 *
 * @author EastX
 * @date 2022/10/20
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 模板实例
     *
     * @param factory Redis 连接工厂
     * @return Redis 模板实例
     */
    @Bean
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
     *
     * @param redisTemplate Redis 模板实例
     * @return Redis 工具类实例
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisUtil redisUtil(RedisTemplate<String, Object> redisTemplate) {
        return new RedisUtil(redisTemplate);
    }

}
