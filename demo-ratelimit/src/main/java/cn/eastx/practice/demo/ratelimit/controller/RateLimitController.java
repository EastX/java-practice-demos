package cn.eastx.practice.demo.ratelimit.controller;

import cn.eastx.practice.demo.ratelimit.config.custom.RateLimit;
import cn.eastx.practice.demo.ratelimit.constants.RateLimiterEnum;
import cn.eastx.practice.demo.ratelimit.pojo.ResponseResult;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 限流处理示例
 *
 * @author EastX
 * @date 2022/11/5
 */
@Slf4j
@RestController
@RequestMapping("/ratelimit/custom")
@RateLimit(threshold = 10, rateLimiter = RateLimiterEnum.FIXED_WINDOW, time = 10, timeUnit = TimeUnit.SECONDS)
public class RateLimitController {

    /**
     * 测试 类注解限流
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/class/ann")
    public ResponseResult<String> classAnnotation(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:classAnnotation:{}", id);
        return ResponseResult.success("custom:classAnnotation:" + id);
    }

    /**
     * 测试 固定窗口限流
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/fixed/window")
    @RateLimit(threshold = 10, rateLimiter = RateLimiterEnum.FIXED_WINDOW, time = 10, timeUnit = TimeUnit.SECONDS)
    public ResponseResult<String> fixedWindow(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:fixedWindow:{}", id);
        return ResponseResult.success("custom:fixedWindow:" + id);
    }

    /**
     * 测试 滑动窗口限流
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/sliding/window")
    @RateLimit(threshold = 10, rateLimiter = RateLimiterEnum.SLIDING_WINDOW, time = 10, timeUnit = TimeUnit.SECONDS)
    public ResponseResult<String> slidingWindow(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:slidingWindow:{}", id);
        return ResponseResult.success("custom:slidingWindow:" + id);
    }

    /**
     * 测试 漏桶限流
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/leaky/bucket")
    @RateLimit(threshold = 10, rateLimiter = RateLimiterEnum.LEAKY_BUCKET, time = 10, timeUnit = TimeUnit.SECONDS)
    public ResponseResult<String> leakyBucket(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:leakyBucket:{}", id);
        return ResponseResult.success("custom:leakyBucket:" + id);
    }

    /**
     * 测试 令牌桶限流
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/token/bucket")
    @RateLimit(threshold = 10, rateLimiter = RateLimiterEnum.TOKEN_BUCKET, time = 10, timeUnit = TimeUnit.SECONDS)
    public ResponseResult<String> tokenBucket(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:tokenBucket:{}", id);
        return ResponseResult.success("custom:tokenBucket:" + id);
    }

}
