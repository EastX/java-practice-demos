package cn.eastx.practice.demo.cache.config;

import cn.eastx.practice.common.util.JsonUtil;
import cn.eastx.practice.demo.cache.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
public class RedisConfig implements BeanPostProcessor {

    /**
     * Redis 模板实例
     *
     * @param factory Redis 连接工厂
     * @return Redis 模板实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper om = JsonUtil.defFacade().getObjectMapper();

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

        return bean;
    }

}
