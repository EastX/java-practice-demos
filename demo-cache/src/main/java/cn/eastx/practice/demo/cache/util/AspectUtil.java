package cn.eastx.practice.demo.cache.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * AOP 切面工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
public class AspectUtil {

    private AspectUtil() {}

    /**
     * 转换 SpEL 解析表达式
     * 示例：${1==1} => true
     *
     * @param spelStr SpEL字符串
     * @param joinPoint 连接点
     * @param desiredResultType 期望结果类型
     * @return 转换 SpEL 表达式后的值
     */
    @Nullable
    public static <T> T convertSpelValue(String spelStr, ProceedingJoinPoint joinPoint,
                                         @Nullable Class<T> desiredResultType) {
        if (StrUtil.isBlank(spelStr)) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return convertSpelValue(spelStr, signature.getMethod(), joinPoint.getArgs(), desiredResultType);
    }

    /**
     * 转换 SpEL 解析表达式
     * 示例：${1==1} => true
     *
     * @param spelStr SpEL字符串
     * @param method 方法对象
     * @param params 方法参数数组
     * @param desiredResultType 期望结果类型
     * @return 转换 SpEL 表达式后的值
     */
    @Nullable
    public static <T> T convertSpelValue(String spelStr, Method method, Object[] params,
                                         @Nullable Class<T> desiredResultType) {
        if (StrUtil.isBlank(spelStr)) {
            return null;
        }

        // 1. 创建SpEL上下文
        StandardEvaluationContext context = createSpelContext(method, params);

        // 2. 创建解析器
        SpelExpressionParser parser = new SpelExpressionParser();

        // 3. 创建解析模板
        TemplateParserContext template = new TemplateParserContext("${", "}");

        // 4. 执行解析转换
        return parser.parseExpression(spelStr, template).getValue(context, desiredResultType);
    }

    /**
     * 创建 SpEL 字符串上下文
     *
     * @param method 方法对象
     * @param params 方法参数值
     * @return SpEL 表达式解析上下文
     */
    private static StandardEvaluationContext createSpelContext(Method method, Object[] params) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        Parameter[] methodParameters = method.getParameters();
        for (int i = 0; i < methodParameters.length; i++) {
            context.setVariable(methodParameters[i].getName(), params[i]);
        }

        // 指定特殊值
        context.setVariable("_IP", IpUtil.getIpAddr());

        return context;
    }

    /**
     * 获取方法 key（类名 + # + 方法名 + (param)）
     *
     * @param joinPoint 连接点
     * @return 方法 key
     */
    public static String getMethodKey(ProceedingJoinPoint joinPoint, String param) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return getMethodKey(joinPoint.getTarget(), signature.getMethod(), param);
    }

    /**
     * 获取方法 key（类名 + # + 方法名 + (param)）
     *
     * @param target 代理对象
     * @param method 方法对象
     * @param param 参数
     * @return 方法 key
     */
    public static String getMethodKey(Object target, Method method, String param) {
        StringBuilder sb = new StringBuilder()
                .append(target.getClass().getSimpleName())
                .append("#")
                .append(method.getName());
        if (StrUtil.isNotBlank(param)) {
            sb.append("(").append(param).append(")");
        }

        return sb.toString();
    }

}
