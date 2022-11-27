package cn.eastx.practice.demo.lock.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 锁工具类（简单实现）
 *  加锁：使用 STRING 保存锁定标识， 'SET key value PX NX' 确保一个 key 只能加锁一次
 *  解锁：判断是自己加的锁进行释放
 *
 * 缺陷：
 *  只能单次加锁（唯一标识通过 ThreadLocal 存储，解锁时会清理 ThreadLocal，多次加解锁会导致与预期不符）
 *  非可重入
 *
 * 参考：https://github.com/realpdai/tech-pdai-spring-demos/blob/main/264-springboot-demo-redis-jedis-distribute-lock/src/main/java/tech/pdai/springboot/redis/jedis/lock/lock/RedisDistributedLock.java
 *
 * @author EastX
 * @date 2022/11/26
 */
@Slf4j
public class RedisSimpleLockUtil {

    /**
     * unique lock flag based on thread local.
     */
    private static final ThreadLocal<String> LOCK_FLAG = ThreadLocal.withInitial(() ->
            UUID.randomUUID().toString().replace("-", "").toLowerCase()
    );
    /**
     * unlock script
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        // 解锁脚本初始化
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource(
                "scripts/redis_unlock_simple.lua")));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSimpleLockUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 加锁
     *
     * @param key 锁 key
     * @param ttl 锁定时长，单位毫秒
     * @param retryNum 重试次数
     * @param retryInterval 重试间隔时长，单位毫秒
     * @return 是否加锁成功
     */
    public boolean lock(String key, long ttl, int retryNum, long retryInterval) {
        boolean result = tryLock(key, ttl);

        // retry if needed
        while ((!result) && retryNum-- > 0) {
            try {
                log.debug("lock failed, retrying...{}", retryNum);
                Thread.sleep(retryInterval);
            } catch (Exception e) {
                return false;
            }

            result = tryLock(key, ttl);
        }

        return result;
    }

    /**
     * 尝试加锁
     *
     * @param key 缓存 key
     * @param ttl 锁定时长，单位毫秒
     * @return 是否加锁成功
     */
    private boolean tryLock(String key, long ttl) {
        try {
            String val = LOCK_FLAG.get();
            Boolean lockRes = redisTemplate.opsForValue()
                    .setIfAbsent(key, val, ttl, TimeUnit.MILLISECONDS);
            log.debug("tryLock, key={}, val={}, lockRes={}", key, val, lockRes);
            return Boolean.TRUE.equals(lockRes);
        } catch (Exception e) {
            log.error("tryLock occurred an exception", e);
        }

        return false;
    }

    /**
     * 解锁
     *
     * @param key 缓存key
     * @return 是否解锁成功
     */
    public boolean unlock(String key) {
        boolean succeed = false;
        try {
            List<String> keys = Collections.singletonList(key);
            Object[] args = {LOCK_FLAG.get()};
            Long unlockRes = redisTemplate.execute(UNLOCK_SCRIPT, keys, args);
            log.debug("unlock, key={}, args={}, unlockRes={}", key, args, unlockRes);
            succeed = Optional.ofNullable(unlockRes).filter(res -> res > 0).isPresent();
        } catch (Exception e) {
            log.error("unlock occurred an exception", e);
        } finally {
            if (succeed) {
                LOCK_FLAG.remove();
            }
        }

        return succeed;
    }

}
