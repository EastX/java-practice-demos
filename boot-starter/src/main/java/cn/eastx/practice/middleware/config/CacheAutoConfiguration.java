package cn.eastx.practice.middleware.config;

import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.common.util.JsonUtil;
import cn.eastx.practice.middleware.cache.*;
import cn.eastx.practice.middleware.util.RedisLockUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * 缓存相关自动配置
 *
 * @author EastX
 * @date 2023/3/19
 */
@Configuration
@EnableConfigurationProperties({CacheProperties.class})
public class CacheAutoConfiguration {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis 模板实例
     */
    @Bean("redisTemplate")
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return createRedisTemplate(redisConnectionFactory);
    }

    /**
     * Redis 模板实例（String）
     */
    @Bean("strRedisTemplate")
    @Primary
    public StringRedisTemplate strRedisTemplate() {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    /**
     * 二级缓存工具类，由于使用了 Redis Pub/Sub 所以需要被 Spring IOC 管理
     */
    @Bean("l2CacheUtil")
    @DependsOn({"redisTemplate", "strRedisTemplate"})
    public L2CacheUtil l2CacheUtil() {
        RedisUtil.initDefTemplate(redisTemplate());
        RedisUtil.initStrTemplate(strRedisTemplate());
        return new L2CacheUtil();
    }

    /**
     * Redis 发布订阅监听
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        // 创建一个消息监听对象
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        // 将监听对象放入到容器中
        container.setConnectionFactory(redisConnectionFactory);

        Collection<RedisSubscriber> subscriberList =
                SpringUtil.getBeansOfType(RedisSubscriber.class).values();
        for (RedisSubscriber subscriber : subscriberList) {
            if (subscriber != null && GeneralUtil.isNotEmpty(subscriber.getTopic())) {
                // 一个订阅者对应一个主题通道信息
                container.addMessageListener(subscriber, new PatternTopic(subscriber.getTopic()));
            }
        }

        return container;
    }

    /**
     * 自定义缓存AOP
     */
    @Bean("methodCacheAspect")
    @DependsOn("l2CacheUtil")
    public MethodCacheAspect methodCacheAspect() {
        return new MethodCacheAspect();
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
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
