package cn.eastx.practice.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * AOP 切面工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
public class AspectUtil {

    private static final Logger logger = LoggerFactory.getLogger(AspectUtil.class);

    private AspectUtil() {}

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
        if (GeneralUtil.isEmpty(spelStr)) {
            return null;
        }

        // 1. 创建SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);

        // 2. 创建解析器
        SpelExpressionParser parser = new SpelExpressionParser();

        // 3. 创建解析模板
        TemplateParserContext template = new TemplateParserContext("${", "}");

        // 4. 执行解析转换
        return parser.parseExpression(spelStr, template).getValue(context, desiredResultType);
    }

    /**
     * 获取方法 key（类名 + # + 方法名 + (param)）
     *
     * @param method 方法对象
     * @param param  参数
     * @return 方法 key
     */
    public static String getMethodKey(Method method, String param) {
        StringBuilder sb = new StringBuilder()
                .append(method.getDeclaringClass().getSimpleName())
                .append("#")
                .append(method.getName());
        if (GeneralUtil.isNotEmpty(param)) {
            sb.append("(").append(param).append(")");
        }

        return sb.toString();
    }

    /**
     * 查找方法或类上的注解（优先方法，没有则方法所在类）
     *
     * @param method         方法对象
     * @param annotationType 注解类型
     * @param <A>            注解类型泛型
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
