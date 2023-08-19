package cn.eastx.practice.demo.cache.config;

import cn.eastx.practice.common.util.JsonUtil;
import cn.eastx.practice.demo.cache.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Redis 配置
 *
 * @author EastX
 * @date 2022/10/20
 */
@Configuration
public class RedisConfig implements BeanPostProcessor {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis 模板实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return createRedisTemplate(redisConnectionFactory);
    }

    /**
     * Redis 模板实例（String）
     */
    @Bean
    public StringRedisTemplate strRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    /**
     * 创建redis实例
     */
    public static RedisTemplate<String, Object> createRedisTemplate(RedisConnectionFactory connectionFactory) {
        if (connectionFactory instanceof LettuceConnectionFactory) {
            LettuceConnectionFactory lettuceConnectionFactory = (LettuceConnectionFactory) connectionFactory;
            // 使用前先校验连接，防止长时间未使用连接被服务端关闭导致 Connection reset by peer
            lettuceConnectionFactory.setValidateConnection(true);
        }

        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(JsonUtil.defFacade().getObjectMapper());

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setKeySerializer(template.getHashKeySerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Bean 初始化（ init-method ）之后处理
     *
     * @param bean Bean 对象
     * @param beanName Bean 名称
     * @return 要使用的 Bean 实例，返回 null 将不会继续调用
     * @throws BeansException 如果出现错误
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ObjectMapper) {
            // Json 工具类替换使用 Spring ObjectMapper
            ObjectMapper objectMapper = (ObjectMapper) bean;
            JsonUtil.initSpringOm(objectMapper);
        }
        if ("redisTemplate".equals(beanName)) {
            RedisUtil.initDefTemplate((RedisTemplate<String, Object>) bean);
        }
        if ("strRedisTemplate".equals(beanName)) {
            RedisUtil.initStrTemplate((StringRedisTemplate) bean);
        }

        return bean;
    }

}
