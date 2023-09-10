package cn.eastx.practice.middleware.cache;

import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.common.util.JsonUtil;
import cn.eastx.practice.middleware.util.AspectUtil;
import com.google.common.collect.Sets;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 方法缓存注解对应的操作类
 *
 * @see MethodCacheable 方法缓存注解
 * @see MethodCacheAspect 方法缓存 AOP 处理
 *
 * @author EastX
 * @date 2022/10/20
 */
public class MethodCacheableOperation {

    /**
     * 缓存 key
     */
    private Set<String> keys;

    /**
     * 缓存时长
     */
    private Duration duration;

    /**
     * 二级缓存配置
     */
    private L2CacheUtil.Config l2Config;

    /**
     * 锁定对象，缓存无数据需要执行调用加载数据是进行锁定，避免重复执行查库处理（缓存击穿）
     * 默认方法返回类型，如果缓存key只有一个就锁定字符串相应常量池对象
     */
    private Object lockObj;

    private MethodCacheableOperation() {}

    /**
     * 转换注解数据为对应操作类
     *
     * @param joinPoint AOP 连接点
     * @return 注解数据对应操作类
     */
    public static MethodCacheableOperation convert(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodCacheable annotation = method.getAnnotation(MethodCacheable.class);
        if (Objects.isNull(annotation)) {
            return null;
        }

        Map<String, Object> spelVars = AspectUtil.buildSpelVars(method, joinPoint.getArgs());
        Boolean isUnless = AspectUtil.parseSpel(annotation.unless(), spelVars, Boolean.class);
        if (Boolean.TRUE.equals(isUnless)) {
            // 匹配条件不满足
            return null;
        }

        Set<String> cacheKeys = generateCacheKey(joinPoint, method, annotation, spelVars);
        Duration duration = convertCacheDuration(annotation);
        L2CacheUtil.Config l2Config = convertL2Config(annotation);
        Object lockObj = buildLockObj(method, cacheKeys);

        MethodCacheableOperation operation = new MethodCacheableOperation();
        operation.setKeys(cacheKeys);
        operation.setDuration(duration);
        operation.setL2Config(l2Config);
        operation.setLockObj(lockObj);
        return operation;
    }

    /**
     * 生成缓存key集合
     *
     * @param joinPoint 连接点
     * @param method 方法
     * @param annotation 注解
     * @param spelVars SpEL参数
     * @return 缓存key集合
     */
    private static Set<String> generateCacheKey(ProceedingJoinPoint joinPoint, Method method,
                                                MethodCacheable annotation, Map<String, Object> spelVars) {
        String[] annKeys = annotation.key();
        Set<String> cacheKeys = Sets.newHashSetWithExpectedSize(annKeys.length);
        for (String annKey : annKeys) {
            String key = AspectUtil.parseSpel(annKey, spelVars, String.class);
            if (GeneralUtil.isNotEmpty(key)) {
                cacheKeys.add(key);
            }
        }

        if (GeneralUtil.isEmpty(cacheKeys)) {
            // 默认缓存key为 类名+方法名+参数值
            String paramStr = JsonUtil.toSimpleStr(joinPoint.getArgs());
            String defKey = new StringBuilder()
                    .append("method:cache:")
                    .append(joinPoint.getTarget().getClass().getSimpleName())
                    .append("#").append(method.getName())
                    .append("(").append(paramStr.replace("\"", "")).append(")")
                    .toString();
            cacheKeys.add(defKey);
        }

        return cacheKeys;
    }

    /**
     * 构建锁定对象
     *
     * @param method 方法
     * @param cacheKeys 缓存key集合
     * @return 锁定对象
     */
    private static Object buildLockObj(Method method, Set<String> cacheKeys) {
        // 加锁对象，默认方法返回类型
        Object lockObj = method.getReturnType();
        if (cacheKeys.size() == 1) {
            // 缓存key只有一个使用缓存key进行加锁
            lockObj = cacheKeys.iterator().next().intern();
        }

        return lockObj;
    }

    /**
     * 转换缓存时长
     *
     * @param annotation 注解
     * @return 缓存时长
     */
    private static Duration convertCacheDuration(MethodCacheable annotation) {
        Duration duration = Duration.ofSeconds(annotation.unit().toSeconds(annotation.timeout()));
        if (!annotation.addRandTtl()) {
            return duration;
        }

        // 增加随机时长 5 - 30 秒
        duration = duration.plusSeconds(ThreadLocalRandom.current().nextInt(5, 30));
        return duration;
    }

    /**
     * 转换二级缓存参数
     *
     * @param annotation 注解
     * @return 二级缓存参数
     */
    private static L2CacheUtil.Config convertL2Config(MethodCacheable annotation) {
        return L2CacheUtil.Config.builder()
                .useL1(annotation.useLocal())
                .durationL1(annotation.localTimeout())
                .compress(annotation.compress())
                .build();
    }

    public Set<String> getKeys() {
        return keys;
    }

    public void setKeys(Set<String> keys) {
        this.keys = keys;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public L2CacheUtil.Config getL2Config() {
        return l2Config;
    }

    public void setL2Config(L2CacheUtil.Config l2Config) {
        this.l2Config = l2Config;
    }

    public Object getLockObj() {
        return lockObj;
    }

    public void setLockObj(Object lockObj) {
        this.lockObj = lockObj;
    }
}
