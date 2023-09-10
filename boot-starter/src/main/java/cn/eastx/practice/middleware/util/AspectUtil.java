package cn.eastx.practice.middleware.util;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * AOP 切面工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
public class AspectUtil {

    /** SpEL 解析器 */
    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();
    /** SpEL解析模板 */
    private static final TemplateParserContext SPEL_TEMPLATE =
            new TemplateParserContext("${", "}");

    private AspectUtil() {}

    /**
     * 构建SpEL上下文变量
     *
     * @param method 方法
     * @param args 方法参数
     * @return SpEL上下文变量Map
     */
    public static Map<String, Object> buildSpelVars(Method method, Object[] args) {
        Parameter[] methodParameters = method.getParameters();

        Map<String, Object> resultMap = Maps.newHashMapWithExpectedSize(methodParameters.length);
        for (int i = 0; i < methodParameters.length; i++) {
            resultMap.put(methodParameters[i].getName(), args[i]);
        }

        return resultMap;
    }

    /**
     * 转换 SpEL 解析表达式
     * 示例：${1==1} => true
     *
     * @param spelStr           SpEL字符串
     * @param variables         上下文变量 名称与值 对应Map
     * @param desiredResultType 期望结果类型
     * @return 转换 SpEL 表达式后的值
     */
    @Nullable
    public static <T> T parseSpel(String spelStr, Map<String, Object> variables,
                                  @Nullable Class<T> desiredResultType) {
        if (StringUtils.isBlank(spelStr)) {
            return null;
        }

        // 1. 创建SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);

        // 2. 执行解析转换
        return SPEL_PARSER.parseExpression(spelStr, SPEL_TEMPLATE)
                .getValue(context, desiredResultType);
    }

    /**
     * 查找方法或类上的注解（优先方法，没有则方法所在类）
     *
     * @param method 方法对象
     * @param annotationType 注解类型
     * @param <A> 注解类型泛型
     * @return 注解对象，查询不到为null
     */
    @Nullable
    public static <A extends Annotation> A findMethodOrClassAnnotation(Method method,
                                                                       @Nullable Class<A> annotationType) {
        // 优先从方法上获取注解
        A annotation = AnnotationUtils.findAnnotation(method, annotationType);
        if (annotation != null) {
            return annotation;
        }

        // 从类上获取注解
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), annotationType);
    }

}
