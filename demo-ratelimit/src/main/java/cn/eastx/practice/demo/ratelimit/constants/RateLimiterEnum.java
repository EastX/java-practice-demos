package cn.eastx.practice.demo.ratelimit.constants;

import cn.eastx.practice.demo.ratelimit.config.custom.RateLimit;
import cn.eastx.practice.demo.ratelimit.util.LocalCacheUtil;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 流量限制器枚举
 *
 * @author EastX
 * @date 2022/11/5
 */
@Getter
@AllArgsConstructor
public enum RateLimiterEnum {
    /** 固定窗口限流 */
    FIXED_WINDOW("scripts/fixed_window_rate_limiter.lua"),
    /** 滑动窗口限流 */
    SLIDING_WINDOW("scripts/sliding_window_rate_limiter.lua") {
        @Override
        public List<Object> obtainArgvs(RateLimit annotation) {
            List<Object> argvList = super.obtainArgvs(annotation);
            // 唯一ID
            argvList.add(UUID.randomUUID().toString().replaceAll("-", "").toLowerCase());
            return argvList;
        }
    },
    /** 漏桶限流 */
    LEAKY_BUCKET("scripts/leaky_bucket_rate_limiter.lua") {
        @Override
        public List<Object> obtainArgvs(RateLimit annotation) {
            List<Object> argvList = super.obtainArgvs(annotation);
            // 每毫秒水流出数量
            argvList.add(obtainLimitRate(annotation));
            return argvList;
        }
    },
    /** 令牌桶限流 */
    TOKEN_BUCKET("scripts/token_bucket_rate_limiter.lua") {
        @Override
        public List<Object> obtainArgvs(RateLimit annotation) {
            List<Object> argvList = super.obtainArgvs(annotation);
            // 每毫秒许可证数量
            argvList.add(obtainLimitRate(annotation));
            return argvList;
        }
    },
    ;

    /**
     * lua 脚本路径
     */
    private String path;

    /**
     * 获取执行脚本
     * 脚本返回数据为 Long，1=未被限流，0=被限流
     *
     * @return Redis 执行脚本
     */
    public DefaultRedisScript<Long> obtainScript() {
        String scriptPath = this.getPath();
        Object cacheScript = LocalCacheUtil.getIfPresent(scriptPath);
        if (Objects.nonNull(cacheScript)) {
            return (DefaultRedisScript<Long>) cacheScript;
        }

        // 加锁处理同步执行
        synchronized (scriptPath.intern()) {
            cacheScript = LocalCacheUtil.getIfPresent(scriptPath);
            if (Objects.nonNull(cacheScript)) {
                return (DefaultRedisScript<Long>) cacheScript;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
            script.setResultType(Long.class);
            LocalCacheUtil.put(scriptPath, script, 1000);
            return script;
        }
    }

    /**
     * 获取lua执行脚本参数集合
     *
     * @param annotation 注解
     * @return lua执行脚本参数集合
     */
    public List<Object> obtainArgvs(RateLimit annotation) {
        // 最大访问量
        long max = annotation.threshold();
        // 超时间隔（毫秒）
        long timeout = annotation.timeUnit().toMillis(annotation.time());
        // 当前时间毫秒数
        long now = Instant.now().toEpochMilli();
        return Lists.newArrayList(max, timeout, now);
    }

    /**
     * 获取限制速率
     *
     * @return 限制速率，单位：每毫秒
     */
    protected double obtainLimitRate(RateLimit annotation) {
        BigDecimal timeoutBd =
                BigDecimal.valueOf(annotation.timeUnit().toMillis(annotation.time()));
        return BigDecimal.valueOf(annotation.threshold())
                .divide(timeoutBd, 4, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

}
