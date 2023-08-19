package cn.eastx.practice.demo.cache.config.custom;

import cn.eastx.practice.demo.cache.constants.AspectKeyTypeEnum;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法缓存注解
 *
 * @see MethodCacheableOperation 方法缓存操作类
 * @see MethodCacheAspect 方法缓存 AOP 处理
 *
 * @author EastX
 * @date 2022/10/20
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodCacheable {

    /**
     * 缓存 key
     *  支持 SpEL 语法，示例：${#param}
     *  默认为空使用对应key为 类名+方法名+参数
     */
    String key() default "";

    /**
     * AOP 切面 key 类型
     */
    AspectKeyTypeEnum keyType() default AspectKeyTypeEnum.DEFAULT;

    /**
     * 缓存排除条件，指定条件不缓存处理
     *  支持 SpEL 语法，示例：${#param==1}
     *  默认为空表示无条件支持缓存
     */
    String unless() default "";

    /**
     * 缓存时长数值
     */
    long timeout() default 5;

    /**
     * 缓存时长单位
     */
    TimeUnit unit() default TimeUnit.MINUTES;

    /**
     * 是否增加随机时长（防止缓存雪崩）
     *  注意：固定了随机时长，依据操作类的转换设定
     * 
     * @see MethodCacheableOperation#convert(ProceedingJoinPoint) 注解转换为操作对象
     */
    boolean addRandTtl() default true;

    /**
     * 是否使用本地缓存
     *  如设置使用本地缓存建议缓存时长大于本地缓存时长
     *
     * @see cn.eastx.practice.demo.cache.util.LocalCacheUtil 本地缓存
     */
    boolean useLocal() default false;

    /**
     * 本地缓存时长，单位秒
     *  注意：本地缓存存在全局最大时长限制
     *
     * @see cn.eastx.practice.demo.cache.util.LocalCacheUtil 本地缓存
     */
    long localTimeout() default 30;

    /**
     * 是否开启对象压缩
     * 对象过大将大量占用 Redis 内存及带宽，非必要进行压缩处理
     */
    boolean compress() default true;

}
