package cn.eastx.practice.demo.ratelimit.config.custom;

import cn.eastx.practice.demo.ratelimit.config.exception.ResponseException;
import cn.eastx.practice.demo.ratelimit.constants.AspectKeyTypeEnum;
import cn.eastx.practice.demo.ratelimit.constants.RateLimiterEnum;
import cn.eastx.practice.demo.ratelimit.constants.ResponseEnum;
import cn.eastx.practice.demo.ratelimit.util.AspectUtil;
import cn.eastx.practice.demo.ratelimit.util.RedisUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 限流拦截器
 *  依赖 {@link RateLimit} 支持
 *
 * @author EastX
 * @date 2022/11/5
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = ((HandlerMethod) handler);
        // 从方法和类上获取注解
        RateLimit annotation = AspectUtil.findMethodOrClassAnnotation(handlerMethod.getMethod(),
                RateLimit.class);
        if (annotation == null) {
            return true;
        }

        AspectKeyTypeEnum.KeyTypeData data = AspectKeyTypeEnum.KeyTypeData.builder()
                .prefix("rate:limit").key(annotation.key()).build();
        String limitKey = annotation.keyType()
                .obtainTypeKey(handlerMethod.getMethod(), handlerMethod.getMethodParameters(), data);
        RateLimiterEnum limiterEnum = annotation.rateLimiter();

        // 执行限流脚本
        Long isLimit = redisUtil.execute(limiterEnum.obtainScript(),
                Lists.newArrayList(limitKey), limiterEnum.obtainArgvs(annotation).toArray());
        if (isLimit != null && isLimit != 0L) {
            return true;
        }

        throw new ResponseException(ResponseEnum.RATE_LIMITED);
    }

}
