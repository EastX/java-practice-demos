package cn.eastx.practice.middleware.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.*;

/**
 * Redis 锁工具类
 *  加锁：使用 HASH 保存锁标识与加锁次数
 *  解锁：判断是自己加的锁进行释放
 *
 * 功能：可重入（Redis HASH）、支持对不同 key 进行加解锁（ThreadLocal<Map<String, String>>）
 *
 * @author EastX
 * @date 2022/11/25
 */
public class RedisLockUtil {

    private Logger logger = LoggerFactory.getLogger(RedisLockUtil.class);
    
    /**
     * unique lock flag based on thread local.
     */
    private static final ThreadLocal<Map<String, String>> LOCK_FLAG =
            ThreadLocal.withInitial(HashMap::new);
    /**
     * lock script
     */
    private static final DefaultRedisScript<Long> LOCK_SCRIPT;
    /**
     * unlock script
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        // 加锁脚本初始化
        LOCK_SCRIPT = new DefaultRedisScript<>();
        LOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource(
                "scripts/redis_lock.lua")));
        LOCK_SCRIPT.setResultType(Long.class);
        // 解锁脚本初始化
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource(
                "scripts/redis_unlock.lua")));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLockUtil(RedisTemplate<String, Object> redisTemplate) {
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
        boolean result = tryLock(key, ttl) > 0;

        // retry if needed
        while ((!result) && retryNum-- > 0) {
            try {
                logger.debug("lock failed, retrying...{}", retryNum);
                Thread.sleep(retryInterval);
            } catch (Exception e) {
                logger.debug("lock failed occurred an exception", e);
                return false;
            }

            result = tryLock(key, ttl) > 0;
        }

        return result;
    }

    /**
     * 尝试加锁
     *
     * @param key 缓存 key
     * @param ttl 锁定时长，单位毫秒
     * @return 已加锁次数，-1 = 未加锁成功，0 = 返回有误
     */
    private long tryLock(String key, long ttl) {
        String uniqueFlag = LOCK_FLAG.get().get(key);
        if (uniqueFlag == null) {
            uniqueFlag = UUID.randomUUID().toString().replace("-", "");
            LOCK_FLAG.get().put(key, uniqueFlag);
        }

        try {
            List<String> keys = Collections.singletonList(key);
            Object[] args = {uniqueFlag, ttl};
            Long lockRes = redisTemplate.execute(LOCK_SCRIPT, keys, args);
            logger.debug("tryLock, lock_flag={}, key={}, args={}, lockRes={}",
                    LOCK_FLAG.get(), key, args, lockRes);
            return lockRes != null ? lockRes : 0L;
        } catch (Exception e) {
            logger.error("tryLock occurred an exception", e);
        }

        return 0L;
    }

    /**
     * 解锁
     *
     * @param key 缓存key
     * @return 是否解锁成功
     */
    public boolean unlock(String key) {
        return tryUnlock(key) >= 0L;
    }

    /**
     * 尝试解锁
     *
     * @param key 锁 key
     * @return 剩余加锁次数，-1 = 非自己加的
     */
    public long tryUnlock(String key) {
        String uniqueFlag = LOCK_FLAG.get().get(key);
        if (uniqueFlag == null) {
            return 0L;
        }

        long lockNum = -1L;
        try {
            List<String> keys = Collections.singletonList(key);
            Object[] args = {uniqueFlag};
            Long unlockRes = redisTemplate.execute(UNLOCK_SCRIPT, keys, args);
            logger.debug("unlock, key={}, args={}, unlockRes={}", key, args, unlockRes);
            lockNum = unlockRes != null ? unlockRes : 0L;
        } catch (Exception e) {
            logger.error("release lock occurred an exception", e);
        } finally {
            if (lockNum == 0L) {
                LOCK_FLAG.get().remove(key);
                if (LOCK_FLAG.get().isEmpty()) {
                    LOCK_FLAG.remove();
                }
            }
        }

        return lockNum;
    }

}
