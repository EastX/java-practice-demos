package cn.eastx.practice.demo.cache.config.custom;

import cn.eastx.practice.demo.cache.util.AspectUtil;
import cn.eastx.practice.demo.cache.constants.AspectKeyTypeEnum;
import cn.eastx.practice.demo.cache.util.L2CacheUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
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
     * 缓存 key
     */
    private String key;

    /**
     * 缓存时长
     */
    private Duration duration;

    /**
     * 二级缓存配置
     */
    private L2CacheUtil.Config l2Config;

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

        AspectKeyTypeEnum.KeyTypeData data = AspectKeyTypeEnum.KeyTypeData.builder()
                .prefix("method:cache").key(annotation.key()).build();
        String key = annotation.keyType().obtainTypeKey(method, joinPoint.getArgs(), data);
        if (StrUtil.isBlank(key)) {
            return null;
        }

        Duration duration = Duration.ofSeconds(annotation.unit().toSeconds(annotation.timeout()));
        if (annotation.addRandTtl()) {
            // 增加随机时长 5 - 30 秒
            duration = duration.plusSeconds(ThreadLocalRandom.current().nextInt(5, 30));
        }

        MethodCacheableOperation operation = new MethodCacheableOperation();
        operation.setKey(key);
        operation.setDuration(duration);
        operation.setL2Config(L2CacheUtil.Config.builder()
                .useL1(annotation.useLocal())
                .durationL1(annotation.localTimeout())
                .compress(annotation.compress())
                .build());
        return operation;
    }

}
