package cn.eastx.practice.demo.cache.config.spring;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

/**
 * 自定义缓存注解过期时间初始化配置
 *  针对 {@link ExpandCacheable} 注解配置的时间进行处理
 *  参考：https://mp.weixin.qq.com/s/zzJH-enXlLZovV8h0RCR6Q
 *
 * @author EastX
 * @date 2022/10/22
 */
public class ExpandCacheExpireConfig {

    @Resource
    private DefaultListableBeanFactory beanFactory;
    @Resource
    private RedisCacheManager expandRedisCacheManager;

    /**
     * Spring Bean 加载后处理
     *  获取所有 @Component 注解的 Bean 判断是否存在 @SpringCacheable 进行过期时间修改
     */
    @PostConstruct
    public void init() {
        Map<String, Object> beanMap = beanFactory.getBeansWithAnnotation(Component.class);
        if (MapUtil.isEmpty(beanMap)) {
            return;
        }

        beanMap.values().forEach(item ->
            ReflectionUtils.doWithMethods(item.getClass(), method -> {
                ReflectionUtils.makeAccessible(method);
                putConfigTtl(method);
            })
        );

        expandRedisCacheManager.initializeCaches();
    }

    /**
     * 利用反射设置配置中的过期时间
     *
     * @param method 注解的方法
     */
    private void putConfigTtl(Method method) {
        ExpandCacheable annotation = method.getAnnotation(ExpandCacheable.class);
        if (annotation == null) {
            return;
        }

        String[] cacheNames = annotation.cacheNames();
        if (ArrayUtil.isEmpty(cacheNames)) {
            cacheNames = annotation.value();
        }

        Map<String, RedisCacheConfiguration> initialCacheConfiguration =
                (Map<String, RedisCacheConfiguration>)
                        ReflectUtil.getFieldValue(expandRedisCacheManager, "initialCacheConfiguration");
        RedisCacheConfiguration defaultCacheConfig =
                (RedisCacheConfiguration)
                        ReflectUtil.getFieldValue(expandRedisCacheManager, "defaultCacheConfig");
        Duration ttl = Duration.ofSeconds(annotation.unit().toSeconds(annotation.timeout()));
        for (String cacheName : cacheNames) {
            initialCacheConfiguration.put(cacheName, defaultCacheConfig.entryTtl(ttl));
        }
    }

}
