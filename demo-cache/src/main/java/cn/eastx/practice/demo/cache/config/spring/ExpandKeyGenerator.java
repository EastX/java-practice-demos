package cn.eastx.practice.demo.cache.config.spring;

import cn.eastx.practice.common.util.AspectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义 key 生成器
 *  默认使用 类名 + # + 方法名 + (param)
 *  可与自定义缓存注解 {@link ExpandCacheable} 搭配使用支持 SpEL key 生成
 *
 * @author EastX
 * @date 2022/10/22
 */
public class ExpandKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        ExpandCacheable annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, ExpandCacheable.class);
        if (Objects.isNull(annotation) || StrUtil.isBlank(annotation.spelKey())) {
            String paramStr = JSONUtil.toJsonStr(params).replace("\"", "");
            return AspectUtil.getMethodKey(method, paramStr);
        }

        // SpEL 支持
        Map<String, Object> spelVars = AspectUtil.buildSpelVars(method, params);
        return AspectUtil.parseSpel(annotation.spelKey(), spelVars, String.class);
    }

}
