package cn.eastx.practice.demo.cache.config.custom;

import cn.eastx.practice.demo.cache.util.LocalCacheUtil;
import cn.eastx.practice.demo.cache.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Objects;

/**
 * 方法缓存 AOP 处理
 *
 * @see MethodCacheable 方法缓存注解
 * @see MethodCacheableOperation 方法缓存操作类
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
@Aspect
@Component
public class MethodCacheAspect {

    /** 空对象 */
    private static final String NULL_VALUE = "NULL-VALUE";
    /**
     * 特殊值缓存时长
     *  特殊值包括 null
     */
    private static final Duration SPECIAL_VALUE_DURATION = Duration.ofSeconds(30);

    @Resource
    private RedisUtil redisUtil;

    /**
     * 缓存处理
     *
     * @param joinPoint AOP 连接点
     * @return 返回结果
     */
    @Around("@annotation(cn.eastx.practice.demo.cache.config.custom.MethodCacheable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodCacheableOperation operation = MethodCacheableOperation.convert(joinPoint);
        if (Objects.isNull(operation)) {
            return joinPoint.proceed();
        }

        Object result = getCacheData(operation);
        if (Objects.nonNull(result)) {
            return convertCacheData(result);
        }

        // 加锁减少重复查库
        synchronized (operation.getKey().intern()) {
            result = getCacheData(operation);
            if (Objects.nonNull(result)) {
                return convertCacheData(result);
            }

            result = joinPoint.proceed();
            setDataCache(operation, result);
        }

        return result;
    }

    /**
     * 获取缓存中的数据
     *
     * @param operation 操作数据
     * @return 缓存中的数据
     */
    private Object getCacheData(MethodCacheableOperation operation) {
        String key = operation.getKey();
        if (!Boolean.TRUE.equals(operation.getUseLocal())) {
            // 不使用本地缓存
            return redisUtil.get(key);
        }

        // 优先从本地缓存获取
        Object data = LocalCacheUtil.getIfPresent(key);
        if (Objects.nonNull(data)) {
            return data;
        }

        // 本地缓存没有从redis缓存获取并设置到本地缓存
        data = redisUtil.get(key);
        if (Objects.nonNull(data)) {
            LocalCacheUtil.put(key, data, operation.getLocalDuration());
        }

        return data;
    }

    /**
     * 设置数据缓存
     *  特殊值缓存需要转换，特殊值包括 null
     *
     * @param operation 操作数据
     * @param data 数据
     * @see MethodCacheAspect#convertCacheData(java.lang.Object) 转换缓存中的特殊值
     */
    private void setDataCache(MethodCacheableOperation operation, Object data) {
        // null缓存处理，固定存储时长，防止缓存穿透
        if (Objects.isNull(data)) {
            redisUtil.setEx(operation.getKey(), NULL_VALUE, SPECIAL_VALUE_DURATION);
            return;
        }

        // 存在实际数据缓存处理
        redisUtil.setEx(operation.getKey(), data, operation.getDuration());
        if (Boolean.TRUE.equals(operation.getUseLocal())) {
            LocalCacheUtil.put(operation.getKey(), data, operation.getLocalDuration());
        }
    }

    /**
     * 转换缓存中数据
     *  特殊值缓存需要转换，如 null
     *
     * @param data 缓存中数据
     * @return 转换后实际返回的数据
     * @see MethodCacheAspect#setDataCache(MethodCacheableOperation, Object) 设置数据缓存
     */
    private Object convertCacheData(Object data) {
        if (!(data instanceof String)) {
            return data;
        }

        String dataStr = (String) data;
        if (NULL_VALUE.equals(dataStr)) {
            // null存储
            return null;
        }

        return dataStr;
    }

}
