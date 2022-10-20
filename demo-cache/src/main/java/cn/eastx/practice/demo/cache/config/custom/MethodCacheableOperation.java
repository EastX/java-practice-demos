package cn.eastx.practice.demo.cache.config.custom;

import cn.eastx.practice.demo.cache.constants.AspectKeyTypeEnum;
import cn.eastx.practice.demo.cache.util.AspectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
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
@Data
public class MethodCacheableOperation {

    /**
     * 缓存Key
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
     * @param joinPoint aop连接点
     * @return 注解数据对应操作类
     */
    public static MethodCacheableOperation convert(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodCacheable annotation = method.getAnnotation(MethodCacheable.class);
        if (Objects.isNull(annotation)) {
            return null;
        }

        Boolean notMet = AspectUtil.convertSpelValue(annotation.condition(), joinPoint,
                Boolean.class);
        if (Boolean.TRUE.equals(notMet)) {
            // 匹配条件不满足
            return null;
        }

        AspectKeyTypeEnum.KeyTypeData data = AspectKeyTypeEnum.KeyTypeData.builder()
                .prefix("method:cache").key(annotation.key()).build();
        String key = annotation.keyType().obtainTypeKey(joinPoint, data);
        if (StrUtil.isBlank(key)) {
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

}
