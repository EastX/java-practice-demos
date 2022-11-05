package cn.eastx.practice.demo.ratelimit.config.custom;

import cn.eastx.practice.demo.ratelimit.constants.AspectKeyTypeEnum;
import cn.eastx.practice.demo.ratelimit.constants.RateLimiterEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 *
 * @see RateLimitInterceptor 限流拦截器
 * @author EastX
 * @date 2022/11/5
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 缓存key
     *  支持SpEL语法，示例：${#param}
     *  默认为空使用对应key为 类名+方法名+参数
     */
    String key() default "";

    /**
     * Key类型
     */
    AspectKeyTypeEnum keyType() default AspectKeyTypeEnum.METHOD;

    /**
     * 限流器
     */
    RateLimiterEnum rateLimiter() default RateLimiterEnum.TOKEN_BUCKET;

    /**
     * 阈值，一定时间内最多访问数
     */
    long threshold();

    /**
     * 限流时长，默认1分钟
     */
    long time() default 1;

    /**
     * 限流时长单位，默认分钟
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

}
