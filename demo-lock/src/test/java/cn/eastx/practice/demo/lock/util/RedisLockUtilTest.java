package cn.eastx.practice.demo.lock.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 锁工具类 测试
 *
 * @author EastX
 * @date 2022/11/25
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RedisLockUtilTest {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockUtil redisLockUtil;

    /** 测试锁可重入 */
    @Test
    public void test_lock() {
        String lockKey = "redis:lock:key";
        redisUtil.delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;
        boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey);
        log.debug("lockSucceed={}, lockMap={}", lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "加锁失败");

        boolean unlockSucceed = redisLockUtil.unlock(lockKey);
        lockMap = redisUtil.hGetAll(lockKey);
        log.debug("unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "解锁失败");
    }

    /** 测试锁可重入 */
    @Test
    public void test_lockReentry() {
        String lockKey = "redis:lock:key:reentry";
        redisUtil.delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;
        boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey);
        log.debug("lockSucceed={}, lockMap={}", lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "加锁失败");

        lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        lockMap = redisUtil.hGetAll(lockKey);
        log.debug("lockSucceed={}, lockMap={}", lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "重入加锁失败");

        boolean unlockSucceed = redisLockUtil.unlock(lockKey);
        lockMap = redisUtil.hGetAll(lockKey);
        log.debug("unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第一次解锁失败");

        unlockSucceed = redisLockUtil.unlock(lockKey);
        lockMap = redisUtil.hGetAll(lockKey);
        log.debug("unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第二次解锁失败");
    }

    /** 测试锁多个不同key */
    @Test
    public void test_lockDifferent() {
        String lockKey1 = "redis:lock:key:diff1";
        String lockKey2 = "redis:lock:key:diff2";
        redisUtil.delete(Arrays.asList(lockKey1, lockKey2));

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;

        boolean lockSucceed = redisLockUtil.lock(lockKey1, ttl, retryNum, retryInterval);
        Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey1);
        log.debug("key={}, lockSucceed={}, lockMap={}", lockKey1, lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "第一把锁加锁失败");

        lockSucceed = redisLockUtil.lock(lockKey2, ttl, retryNum, retryInterval);
        lockMap = redisUtil.hGetAll(lockKey2);
        log.debug("key={}, lockSucceed={}, lockMap={}", lockKey2, lockSucceed, lockMap);
        Assert.isTrue(lockSucceed, "第二把锁加锁失败");

        boolean unlockSucceed = redisLockUtil.unlock(lockKey1);
        lockMap = redisUtil.hGetAll(lockKey1);
        log.debug("key={}, unlockSucceed={}, lockMap={}", lockKey1, unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第一把锁解锁失败");

        unlockSucceed = redisLockUtil.unlock(lockKey2);
        lockMap = redisUtil.hGetAll(lockKey2);
        log.debug("key={}, unlockSucceed={}, lockMap={}", lockKey2, unlockSucceed, lockMap);
        Assert.isTrue(unlockSucceed, "第二把锁解锁失败");
    }

    /** 测试锁多个线程对一个key加锁 */
    @Test
    public void test_lockMultiThread() throws InterruptedException {
        String lockKey = "redis:lock:key:multi:thread";
        redisUtil.delete(lockKey);

        long ttl = 600_000;
        int retryNum = 0;
        long retryInterval = 1000;

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
            Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey);
            log.debug("thread1, lockSucceed={}, lockMap={}", lockSucceed, lockMap);
            Assert.isTrue(lockSucceed, "第一次加锁失败");

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean unlockSucceed = redisLockUtil.unlock(lockKey);
            lockMap = redisUtil.hGetAll(lockKey);
            log.debug("thread1, unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
            Assert.isTrue(unlockSucceed, "第一次解锁失败");
        });

        Thread.sleep(500);

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
            Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey);
            log.debug("thread2, lockSucceed={}, lockMap={}", lockSucceed, lockMap);
            Assert.isTrue(!lockSucceed, "第二次加锁成功");

            boolean unlockSucceed = redisLockUtil.unlock(lockKey);
            lockMap = redisUtil.hGetAll(lockKey);
            log.debug("thread2, unlockSucceed={}, lockMap={}", unlockSucceed, lockMap);
            Assert.isTrue(!unlockSucceed, "第二次解锁成功");
        });

        Thread.sleep(10_000);
    }

    /** 测试锁多个线程对不同key加锁 */
    @Test
    public void test_lockMultiThreadDiffKey() throws InterruptedException {
        String lockKey1 = "redis:lock:key:multi:thread:diff1";
        String lockKey2 = "redis:lock:key:multi:thread:diff2";
        redisUtil.delete(Arrays.asList(lockKey1, lockKey2));

        long ttl = 600_000;
        int retryNum = 0;
        long retryInterval = 1000;

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey1, ttl, retryNum, retryInterval);
            Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey1);
            log.debug("thread1, key={}, lockSucceed={}, lockMap={}",
                    lockKey1, lockSucceed, lockMap);
            Assert.isTrue(lockSucceed, "第一把锁加锁失败");

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean unlockSucceed = redisLockUtil.unlock(lockKey1);
            lockMap = redisUtil.hGetAll(lockKey1);
            log.debug("thread1, key={}, unlockSucceed={}, lockMap={}",
                    lockKey1, unlockSucceed, lockMap);
            Assert.isTrue(unlockSucceed, "第一把锁解锁失败");
        });

        Thread.sleep(500);

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisLockUtil.lock(lockKey2, ttl, retryNum, retryInterval);
            Map<Object, Object> lockMap = redisUtil.hGetAll(lockKey2);
            log.debug("thread2, key={}, lockSucceed={}, lockMap={}",
                    lockKey2, lockSucceed, lockMap);
            Assert.isTrue(lockSucceed, "第一把锁加锁失败");

            boolean unlockSucceed = redisLockUtil.unlock(lockKey2);
            lockMap = redisUtil.hGetAll(lockKey2);
            log.debug("thread2, key={}, unlockSucceed={}, lockMap={}",
                    lockKey2, unlockSucceed, lockMap);
            Assert.isTrue(unlockSucceed, "第二把锁加锁失败");
        });

        Thread.sleep(10_000);
    }

}
