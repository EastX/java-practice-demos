package cn.eastx.practice.demo.cache.config.custom;

import cn.eastx.practice.demo.cache.util.L2CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

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

    @Pointcut("@annotation(cn.eastx.practice.demo.cache.config.custom.MethodCacheable)")
    public void pointcut() {}

    /**
     * 缓存处理
     *
     * @param joinPoint AOP 连接点
     * @return 返回结果
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodCacheableOperation operation = MethodCacheableOperation.convert(joinPoint);
        if (Objects.isNull(operation)) {
            return joinPoint.proceed();
        }

        Object result = getCacheData(operation);
        if (Objects.nonNull(result)) {
            return convertCacheData(result, operation);
        }

        try {
            // 加锁处理同步执行
            synchronized (operation.getKey().intern()) {
                result = getCacheData(operation);
                if (Objects.nonNull(result)) {
                    return convertCacheData(result, operation);
                }

                result = joinPoint.proceed();
                setDataCache(operation, result);
            }

            return result;
        } catch (Throwable e) {
            setDataCache(operation, e);
            throw e;
        }
    }

    /**
     * 获取缓存中的数据
     *
     * @param operation 操作数据
     * @return 缓存中的数据
     */
    private Object getCacheData(MethodCacheableOperation operation) {
        String key = operation.getKey();
        return L2CacheUtil.getStoreVal(key, operation.getL2Config());
    }

    /**
     * 转换缓存中数据
     *  特殊值缓存需要转换，如 null
     *
     * @param data 缓存中数据
     * @return 转换后实际返回的数据
     * @see MethodCacheAspect#setDataCache(MethodCacheableOperation, Object) 设置数据缓存
     */
    private Object convertCacheData(Object data, MethodCacheableOperation operation) {
        return L2CacheUtil.convertCacheData(data, operation.getL2Config());
    }

    /**
     * 设置数据缓存
     *  特殊值缓存需要转换，特殊值包括 null
     *
     * @param operation 操作数据
     * @param data 数据
     * @see MethodCacheAspect#convertCacheData(Object, MethodCacheableOperation)  转换缓存中的特殊值
     */
    private void setDataCache(MethodCacheableOperation operation, Object data) {
        L2CacheUtil.set(operation.getKey(), data, operation.getDuration().getSeconds(),
                operation.getL2Config());
    }

}
