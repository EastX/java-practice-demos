package cn.eastx.practice.middleware.aspect;

import cn.eastx.practice.middleware.annotation.MethodCacheable;
import cn.eastx.practice.middleware.constant.AspectKeyTypeEnum;
import cn.eastx.practice.middleware.util.AspectUtil;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
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
    private String key;

    /**
     * 缓存时长
     */
    private Duration duration;

    /**
     * 是否使用本地缓存
     */
    private Boolean useLocal;

    /**
     * 本地缓存时长，单位秒
     */
    private Long localDuration;

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

        Boolean isUnless = AspectUtil.convertSpelValue(annotation.unless(), method,
                joinPoint.getArgs(), Boolean.class);
        if (Boolean.TRUE.equals(isUnless)) {
            // 匹配条件不满足
            return null;
        }

        AspectKeyTypeEnum.KeyTypeData data =
                new AspectKeyTypeEnum.KeyTypeData("method:cache", annotation.key());
        String key = annotation.keyType().obtainTypeKey(method, joinPoint.getArgs(), data);
        if (StringUtils.isBlank(key)) {
            return null;
        }

        Duration duration = Duration.ofSeconds(annotation.unit().toSeconds(annotation.timeout()));
        if (annotation.addRandomDuration()) {
            // 增加随机时长 5 - 30 秒
            duration = duration.plusSeconds(ThreadLocalRandom.current().nextInt(5, 30));
        }

        MethodCacheableOperation operation = new MethodCacheableOperation();
        operation.setKey(key);
        operation.setDuration(duration);
        operation.setUseLocal(annotation.useLocal());
        operation.setLocalDuration(annotation.localTimeout());
        return operation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Boolean getUseLocal() {
        return useLocal;
    }

    public void setUseLocal(Boolean useLocal) {
        this.useLocal = useLocal;
    }

    public Long getLocalDuration() {
        return localDuration;
    }

    public void setLocalDuration(Long localDuration) {
        this.localDuration = localDuration;
    }

    @Override
    public String toString() {
        return "MethodCacheableOperation{" +
                "key='" + key + '\'' +
                ", duration=" + duration +
                ", useLocal=" + useLocal +
                ", localDuration=" + localDuration +
                '}';
    }
}
