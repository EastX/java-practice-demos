package cn.eastx.practice.demo.lock.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 锁工具类（简单实现） 测试
 *
 * @author EastX
 * @date 2022/11/26
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RedisSimpleLockUtilTest {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisSimpleLockUtil redisSimpleLockUtil;

    @Test
    public void test_lock() {
        String lockKey = "redis:simple:lock:key";
        redisUtil.delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;
        boolean lockSucceed = redisSimpleLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        String lockVal = (String) redisUtil.get(lockKey);
        log.debug("lockSucceed={}, lockVal={}", lockSucceed, lockVal);
        Assert.isTrue(lockSucceed, "加锁失败");

        boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey);
        lockVal = (String) redisUtil.get(lockKey);
        log.debug("unlockSucceed={}, lockVal={}", unlockSucceed, lockVal);
        Assert.isTrue(unlockSucceed, "解锁失败");
    }

    /** 测试锁可重入 */
    @Test
    public void test_lockReentry() {
        String lockKey = "redis:simple:lock:key";
        redisUtil.delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;
        boolean lockSucceed = redisSimpleLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        String lockVal = (String) redisUtil.get(lockKey);
        log.debug("lockSucceed={}, lockVal={}", lockSucceed, lockVal);
        Assert.isTrue(lockSucceed, "加锁失败");

        lockSucceed = redisSimpleLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
        lockVal = (String) redisUtil.get(lockKey);
        log.debug("lockSucceed={}, lockVal={}", lockSucceed, lockVal);
        Assert.isTrue(!lockSucceed, "重入加锁成功");

        boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey);
        lockVal = (String) redisUtil.get(lockKey);
        log.debug("unlockSucceed={}, lockVal={}", unlockSucceed, lockVal);
        Assert.isTrue(unlockSucceed, "第一次解锁失败");

        unlockSucceed = redisSimpleLockUtil.unlock(lockKey);
        lockVal = (String) redisUtil.get(lockKey);
        log.debug("unlockSucceed={}, lockVal={}", unlockSucceed, lockVal);
        Assert.isTrue(!unlockSucceed, "第二次解锁成功");
    }

    /** 测试锁多个不同key */
    @Test
    public void test_lockDifferent() {
        String lockKey1 = "redis:simple:lock:key:diff1";
        String lockKey2 = "redis:simple:lock:key:diff2";
        redisUtil.delete(Arrays.asList(lockKey1, lockKey2));

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;

        boolean lockSucceed = redisSimpleLockUtil.lock(lockKey1, ttl, retryNum, retryInterval);
        String lockVal = (String) redisUtil.get(lockKey1);
        log.debug("key={}, lockSucceed={}, lockVal={}", lockKey1, lockSucceed, lockVal);
        Assert.isTrue(lockSucceed, "第一把锁加锁失败");

        lockSucceed = redisSimpleLockUtil.lock(lockKey2, ttl, retryNum, retryInterval);
        lockVal = (String) redisUtil.get(lockKey2);
        log.debug("key={}, lockSucceed={}, lockVal={}", lockKey2, lockSucceed, lockVal);
        Assert.isTrue(lockSucceed, "第二把锁加锁失败");

        boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey1);
        lockVal = (String) redisUtil.get(lockKey1);
        log.debug("key={}, unlockSucceed={}, lockVal={}", lockKey1, unlockSucceed, lockVal);
        Assert.isTrue(unlockSucceed, "用第一把锁解锁失败");

        unlockSucceed = redisSimpleLockUtil.unlock(lockKey2);
        lockVal = (String) redisUtil.get(lockKey2);
        log.debug("key={}, unlockSucceed={}, lockVal={}", lockKey2, unlockSucceed, lockVal);
        Assert.isTrue(!unlockSucceed, "用第二把锁解锁成功");
    }

    /** 测试锁多个线程对一个key加锁 */
    @Test
    public void test_lockMultiThread() throws InterruptedException {
        String lockKey = "redis:simple:lock:key:multi:thread";
        redisUtil.delete(lockKey);

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisSimpleLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
            String lockVal = (String) redisUtil.get(lockKey);
            log.debug("thread1, lockSucceed={}, lockVal={}", lockSucceed, lockVal);
            Assert.isTrue(lockSucceed, "第一次加锁失败");

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey);
            lockVal = (String) redisUtil.get(lockKey);
            log.debug("thread1, unlockSucceed={}, lockVal={}", unlockSucceed, lockVal);
            Assert.isTrue(unlockSucceed, "第一次解锁失败");
        });

        Thread.sleep(500);

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisSimpleLockUtil.lock(lockKey, ttl, retryNum, retryInterval);
            String lockVal = (String) redisUtil.get(lockKey);
            log.debug("thread2, lockSucceed={}, lockVal={}", lockSucceed, lockVal);
            Assert.isTrue(!lockSucceed, "第二次加锁成功");

            boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey);
            lockVal = (String) redisUtil.get(lockKey);
            log.debug("thread2, unlockSucceed={}, lockVal={}", unlockSucceed, lockVal);
            Assert.isTrue(!unlockSucceed, "第二次解锁成功");
        });

        Thread.sleep(10_000);
    }

    /** 测试锁多个线程对不同key加锁 */
    @Test
    public void test_lockMultiThreadDiffKey() throws InterruptedException {
        String lockKey1 = "redis:simple:lock:key:multi:thread:diff1";
        String lockKey2 = "redis:simple:lock:key:multi:thread:diff2";
        redisUtil.delete(Arrays.asList(lockKey1, lockKey2));

        long ttl = 600_000;
        int retryNum = 1;
        long retryInterval = 1000;

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisSimpleLockUtil.lock(lockKey1, ttl, retryNum, retryInterval);
            String lockVal = (String) redisUtil.get(lockKey1);
            log.debug("thread1, key={}, lockSucceed={}, lockVal={}",
                    lockKey1, lockSucceed, lockVal);
            Assert.isTrue(lockSucceed, "第一把锁加锁失败");

            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey1);
            lockVal = (String) redisUtil.get(lockKey1);
            log.debug("thread1, unlockSucceed={}, lockVal={}", unlockSucceed, lockVal);
            Assert.isTrue(!unlockSucceed, "第一把锁解锁失败");
        });

        Thread.sleep(500);

        CompletableFuture.runAsync(() -> {
            boolean lockSucceed = redisSimpleLockUtil.lock(lockKey2, ttl, retryNum, retryInterval);
            String lockVal = (String) redisUtil.get(lockKey2);
            log.debug("thread2, key={}, lockSucceed={}, lockVal={}",
                    lockKey2, lockSucceed, lockVal);
            Assert.isTrue(lockSucceed, "第二把锁加锁失败");

            boolean unlockSucceed = redisSimpleLockUtil.unlock(lockKey2);
            lockVal = (String) redisUtil.get(lockKey2);
            log.debug("thread2, key={}, unlockSucceed={}, lockVal={}",
                    lockKey2, unlockSucceed, lockVal);
            Assert.isTrue(unlockSucceed, "第二把锁解锁成功");
        });

        Thread.sleep(10_000);
    }

}
