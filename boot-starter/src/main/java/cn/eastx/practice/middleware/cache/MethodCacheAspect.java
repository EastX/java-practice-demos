package cn.eastx.practice.middleware.cache;

import cn.eastx.practice.common.util.GeneralUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;

/**
 * 方法缓存 AOP 处理
 *
 * @see MethodCacheable 方法缓存注解
 * @see MethodCacheableOperation 方法缓存操作类
 *
 * @author EastX
 * @date 2022/10/20
 */
@Aspect
public class MethodCacheAspect {

    @Pointcut("@annotation(cn.eastx.practice.middleware.cache.MethodCacheable)")
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
        if (operation == null) {
            return joinPoint.proceed();
        }

        Object result = getCacheData(operation);
        if (result != null) {
            return parseCacheData(result, operation);
        }

        try {
            // 加锁处理同步执行
            synchronized (operation.getLockObj()) {
                result = getCacheData(operation);
                if (result != null) {
                    return parseCacheData(result, operation);
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
        Map<String, Object> kvMap = L2CacheUtil.getStoreVal(operation.getKeys(), operation.getL2Config(), false);
        if (GeneralUtil.isEmpty(kvMap)) {
            return null;
        }

        return kvMap.values().iterator().next();
    }

    /**
     * 转换缓存中数据
     *  特殊值缓存需要转换，如 null
     *
     * @param data 缓存中数据
     * @return 转换后实际返回的数据
     * @see MethodCacheAspect#setDataCache(MethodCacheableOperation, Object) 设置数据缓存
     */
    private Object parseCacheData(Object data, MethodCacheableOperation operation) {
        return L2CacheUtil.parseCacheData(data, operation.getL2Config());
    }

    /**
     * 设置数据缓存
     *  特殊值缓存需要转换，特殊值包括 null
     *
     * @param operation 操作数据
     * @param data 数据
     * @see MethodCacheAspect#parseCacheData(Object, MethodCacheableOperation)  转换缓存中的特殊值
     */
    private void setDataCache(MethodCacheableOperation operation, Object data) {
        L2CacheUtil.set(operation.getKeys(), data, operation.getDuration().getSeconds(),
                operation.getL2Config());
    }

}
