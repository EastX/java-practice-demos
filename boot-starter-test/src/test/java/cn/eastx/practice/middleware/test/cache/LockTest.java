package cn.eastx.practice.middleware.test.cache;

import cn.eastx.practice.middleware.cache.RedisUtil;
import cn.eastx.practice.middleware.util.RedisLockUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 锁相关测试
 *
 * @author EastX
 * @date 2023/3/19
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class LockTest {

    private Logger logger = LoggerFactory.getLogger(LockTest.class);

    @Resource
    private RedisLockUtil redisLockUtil;

    /** 测试锁可重入 */
    @Test
    public void test_lock() {
        String lockKey = "redis:lock:key";
        RedisUtil.defTemplate().delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;
        boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey);
        logger.debug("lockSucceed={}, lockMap={}", lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "加锁失败");

        boolean unlockSucceed = redisLockUtil.unlock(lockKey);
        lockMap = RedisUtil.opsHash().entries(lockKey);
        logger.debug("unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "解锁失败");
    }

    /** 测试锁可重入 */
    @Test
    public void test_lockReentry() {
        String lockKey = "redis:lock:key:reentry";
        RedisUtil.defTemplate().delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;
        boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey);
        logger.debug("lockSucceed={}, lockMap={}", lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "加锁失败");

        lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        lockMap = RedisUtil.opsHash().entries(lockKey);
        logger.debug("lockSucceed={}, lockMap={}", lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "重入加锁失败");

        boolean unlockSucceed = redisLockUtil.unlock(lockKey);
        lockMap = RedisUtil.opsHash().entries(lockKey);
        logger.debug("unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第一次解锁失败");

        unlockSucceed = redisLockUtil.unlock(lockKey);
        lockMap = RedisUtil.opsHash().entries(lockKey);
        logger.debug("unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第二次解锁失败");
    }

    /** 测试锁多个不同key */
    @Test
    public void test_lockDifferent() {
        String lockKey1 = "redis:lock:key:diff1";
        String lockKey2 = "redis:lock:key:diff2";
        RedisUtil.defTemplate().delete(Arrays.asList(lockKey1, lockKey2));

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;

        boolean lockSucceed = redisLockUtil.lock(lockKey1, ttl, retryNum, retryInterval);
        Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey1);
        logger.debug("key={}, lockSucceed={}, lockMap={}", lockKey1, lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "第一把锁加锁失败");

        lockSucceed = redisLockUtil.lock(lockKey2, ttl, retryNum, retryInterval);
        lockMap = RedisUtil.opsHash().entries(lockKey2);
        logger.debug("key={}, lockSucceed={}, lockMap={}", lockKey2, lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "第二把锁加锁失败");

        boolean unlockSucceed = redisLockUtil.unlock(lockKey1);
        lockMap = RedisUtil.opsHash().entries(lockKey1);
        logger.debug("key={}, unlockSucceed={}, lockMap={}", lockKey1, unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第一把锁解锁失败");

        unlockSucceed = redisLockUtil.unlock(lockKey2);
        lockMap = RedisUtil.opsHash().entries(lockKey2);
        logger.debug("key={}, unlockSucceed={}, lockMap={}", lockKey2, unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第二把锁解锁失败");
    }

    /** 测试锁多个线程对一个key加锁 */
    @Test
    public void test_lockMultiThread() throws InterruptedException {
        String lockKey = "redis:lock:key:multi:thread";
        RedisUtil.defTemplate().delete(lockKey);

        long ttl = 600_000;
        int retryNum = 0;
        long retryInterval = 1000;

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
            Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey);
            logger.debug("thread1, lockSucceed={}, lockMap={}", lockSucceed, lockMap);
            Assert.isTrue(lockSucceed, "第一次加锁失败");

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean unlockSucceed = redisLockUtil.unlock(lockKey);
            lockMap = RedisUtil.opsHash().entries(lockKey);
            logger.debug("thread1, unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
            Assert.isTrue(unlockSucceed, "第一次解锁失败");
        });

        Thread.sleep(500);

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
            Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey);
            logger.debug("thread2, lockSucceed={}, lockMap={}", lockSucceed, lockMap);
            Assert.isTrue(!lockSucceed, "第二次加锁成功");

            boolean unlockSucceed = redisLockUtil.unlock(lockKey);
            lockMap = RedisUtil.opsHash().entries(lockKey);
            logger.debug("thread2, unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
            Assert.isTrue(!unlockSucceed, "第二次解锁成功");
        });

        Thread.sleep(10_000);
    }

    /** 测试锁多个线程对不同key加锁 */
    @Test
    public void test_lockMultiThreadDiffKey() throws InterruptedException {
        String lockKey1 = "redis:lock:key:multi:thread:diff1";
        String lockKey2 = "redis:lock:key:multi:thread:diff2";
        RedisUtil.defTemplate().delete(Arrays.asList(lockKey1, lockKey2));

        long ttl = 600_000;
        int retryNum = 0;
        long retryInterval = 1000;

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey1, ttl, retryNum, retryInterval);
            Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey1);
            logger.debug("thread1, key={}, lockSucceed={}, lockMap={}",
                    lockKey1, lockSucceed, lockMap);
            Assert.isTrue(lockSucceed, "第一把锁加锁失败");

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean unlockSucceed = redisLockUtil.unlock(lockKey1);
            lockMap = RedisUtil.opsHash().entries(lockKey1);
            logger.debug("thread1, key={}, unlockSucceed={}, lockMap={}",
                    lockKey1, unlockSucceed, lockMap);
            Assert.isTrue(unlockSucceed, "第一把锁解锁失败");
        });

        Thread.sleep(500);

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey2, ttl, retryNum, retryInterval);
            Map<String, Object> lockMap = RedisUtil.opsHash().entries(lockKey2);
            logger.debug("thread2, key={}, lockSucceed={}, lockMap={}",
                    lockKey2, lockSucceed, lockMap);
            Assert.isTrue(lockSucceed, "第一把锁加锁失败");

            boolean unlockSucceed = redisLockUtil.unlock(lockKey2);
            lockMap = RedisUtil.opsHash().entries(lockKey2);
            logger.debug("thread2, key={}, unlockSucceed={}, lockMap={}",
                    lockKey2, unlockSucceed, lockMap);
            Assert.isTrue(unlockSucceed, "第二把锁加锁失败");
        });

        Thread.sleep(10_000);
    }

}
