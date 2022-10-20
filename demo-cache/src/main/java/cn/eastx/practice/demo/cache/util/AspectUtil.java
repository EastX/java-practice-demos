package cn.eastx.practice.demo.cache.util;

import cn.hutool.core.util.StrUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Parameter;

/**
 * AOP 切面工具类
 *
 * @author EastX
 * @date 2022/10/20
 */
public class AspectUtil {

    private AspectUtil() {}

    /**
     * 转换 SpEL 解析表达式
     * 示例：${1==1} => true
     *
     * @param spelStr   SpEL字符串
     * @param joinPoint 连接点
     * @return SpEL 表达式的值
     */
    @Nullable
    public static <T> T convertSpelValue(String spelStr, ProceedingJoinPoint joinPoint,
                                         @Nullable Class<T> desiredResultType) {
        if (spelStr == null || spelStr.trim().isEmpty()) {
            return null;
        }

        // 1. 创建SpEL上下文
        StandardEvaluationContext context = createSpelContext(joinPoint);

        // 2. 创建解析器
        SpelExpressionParser parser = new SpelExpressionParser();

        // 3. 创建解析模板
        TemplateParserContext template = new TemplateParserContext("${", "}");

        return parser.parseExpression(spelStr, template).getValue(context, desiredResultType);
    }

    /**
     * 创建 SpEL 字符串上下文
     *
     * @param joinPoint 连接点
     * @return SpEL 表达式解析上下文
     */
    private static StandardEvaluationContext createSpelContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] methodParameters = signature.getMethod().getParameters();
        Object[] params = joinPoint.getArgs();
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < methodParameters.length; i++) {
            context.setVariable(methodParameters[i].getName(), params[i]);
        }

        // 指定特殊值
        context.setVariable("_IP", IpUtil.getIpAddr());

        return context;
    }

    /**
     * 获取方法 key（前缀 + 类名 + # + 方法名 + (param)）
     *
     * @param joinPoint 连接点
     * @return 默认 key
     */
    public static String getMethodKey(ProceedingJoinPoint joinPoint, String param) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StringBuilder sb = new StringBuilder()
                .append(joinPoint.getTarget().getClass().getSimpleName())
                .append("#")
                .append(signature.getMethod().getName());
        if (StrUtil.isNotBlank(param)) {
            sb.append("(").append(param).append(")");
        }

        return sb.toString();
    }

}
