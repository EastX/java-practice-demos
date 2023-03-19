<h1 align="center">
    <a href="https://github.com/EastX/java-practice-demos/tree/main/demo-cache">
        锁实践示例
    </a>
</h1>

<p align="center">
    <a href="https://www.oracle.com/java/technologies/downloads/archive/">
        <img alt="JDK" src="https://img.shields.io/badge/JDK-1.8.0_201-e67621.svg"/>
    </a>
    <a href="https://docs.spring.io/spring-boot/docs/2.7.9/reference/html/">
        <img alt="Spring Boot" src="https://img.shields.io/badge/Spring Boot-2.7.9-6db33f.svg"/>
    </a>
    <a href="https://redis.io/">
        <img alt="Redis" src="https://img.shields.io/badge/Redis-6.0 lettuce-dc382c.svg"/>
    </a>
</p>

## 1. 锁概述
> 在计算机科学中，锁是在执行多线程时用于强行限制资源访问的同步机制，即用于在并发控制中保证对互斥要求的满足。

**锁相关概念**
- 锁开销：完成一个锁可能额外耗费的资源，比如一个周期所需要的时间，内存空间。
- 锁竞争：一个线程或进程，要获取另一个线程或进程所持有的锁，边会发生锁竞争。锁粒度越小，竞争的可能越小。
- 死锁：多个线程争夺资源互相等待资源释放导致阻塞；由于无限期阻塞，程序不能正常终止。

**分类**
- 乐观锁、悲观锁：是否锁定同步资源。
    - 乐观锁：认为其他线程对数据访问时 **不会** 修改数据，实际未加锁，更新数据时判断是否被其他线程更新了（读时不加锁，写时加锁）。 
      - 适合多读的场景，因为读操作没有加锁。
      - 实现原理：CAS (compare-and-swap) ，无锁算法，原子操作比较更新。
      - 使用：
        - Java 中的 CAS 锁（AtomicXxx）通过 JNI 调用 CPU 中的 cmpxchg 汇编指令实现
        - 数据库表增加 version 字段，更新时判断 version 未改变。
      - 缺陷：
        - ABA 问题：数据发生类似变化（A -> B -> A），会认为数据没有改变。
        <br> JDK 1.5 引入 AtomicStampedReference 增加标志位（1A -> 2B -> 3A）
        - 自旋问题：CAS 无法获取到锁会在超时时间内循环获取，造成 CPU 资源浪费
    - 悲观锁：认为其他线程对数据访问时 **一定会** 修改数据，访问数据时加锁同步处理（一开始加锁无论读写）。 
      - 适合多写的场景，独占数据的读写权限，确保数据的读取和更新都是准确的。
- 读写锁
  - 读锁：共享锁，可支持多线程并发读。
  - 写锁：独享锁，读写、写写互斥。
  - 示例：ReentrantReadWriteLock
- 可重入锁、不可重入锁
    - 可重入锁（递归锁）：一个线程在已加锁范围内代码中再次进行加锁能够获取到锁
      - synchronized 、 ReentrantLock
    - 不可重入锁：一个线程对在已加锁范围内代码中再次进行加锁操作，由于第二次加锁时需要等待上次锁释放才可以加锁造成锁的互相等待
- 公平锁、非公平锁
  - 公平锁：多个线程按照申请锁的顺序来获取锁，依赖 AQS 队列，线程直接进入队列中排队，第一个线程才能获取到锁
  - 非公平锁：多个线程加锁时尝试直接获取锁，获取不到进入队列，可能出现后申请锁的线程先获取到锁
    - 优点：可以减少唤起线程的开销，整体吞吐效率高
    - 缺点：处于等待队列中的线程可能饿死
    - synchronized
  - 示例：ReentrantLock 默认为非公平锁，构造方法可指定为公平锁 `new ReentrantLock(true);`
- 偏向锁、轻量锁、重量锁：synchronized 的三种锁状态。
    - 偏向锁：锁标志位 101，在对象头（Mark Word）和栈帧中锁记录（Lock Record）里存储线程ID，通过 **对比 Mark Word** 避免执行 CAS
      - JDK 6 引入，JDK 15 标记废弃，可通过 JVM 参数（-XX:+UseBiasedLocking）手动启用
    - 轻量锁：锁标志位 000，偏向锁时出现竞争升级为轻量锁，未获取到锁的线程自旋获取，通过 **CAS + 自旋** 避免线程阻塞唤醒
    - 重量锁：锁标志位 010，轻量锁自旋超过一定此处升级为重量锁，未获取到锁的线程休眠
- 分段锁、自旋锁：锁设计，非特定的锁。
  - 分段锁：将要锁定的数据拆分成段后对所需数据段加锁，减少锁定范围
    - ConcurrentHashMap 在 JDK 8 之前使用 Segment （继承 ReentrantLock）对桶数组分割分段加锁
  - 自旋锁：试探获取资源，未获取到采取自旋循环 `where(true)` 再次试探获取，不阻塞线程
    - 轻量锁通过 **CAS + 自旋** 实现
    - 优点：减少上下文切换
    - 缺点：占用 CPU

**相关阅读：**
- [Java中的锁 - 沈三白](https://zhuanlan.zhihu.com/p/352349404)
- [听说你知道什么是锁 --JAVA - 罗小扇](https://www.cnblogs.com/adrien/p/11063391.html)


## 2. 自定义锁工具

### 2.1 Redis 分布式锁（简单实现）
> 使用 ThreadLocal 保存锁对应的唯一标识
> <br> 加锁：使用 STRING 保存锁定标识， 'SET key value PX NX' 确保一个 key 只能加锁一次
> <br> 解锁：Lua 脚本判断是自己加的锁进行释放

- Redis 分布式锁（简单实现） [`RedisSimpleLockUtil.java`](./src/main/java/cn/eastx/practice/demo/lock/util/RedisSimpleLockUtil.java)
    ```java
    // 使用 ThreadLocal 保存锁对应的唯一标识
    private static final ThreadLocal<String> LOCK_FLAG = ThreadLocal.withInitial(() ->
            UUID.randomUUID().toString().replace("-", "").toLowerCase()
    );
  
    // 尝试加锁
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

    // 解锁
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
    ```
- 解锁 Lua 脚本 [`redis_unlock_simple.lua`](./src/main/resources/scripts/redis_unlock_simple.lua)
    ```lua
    local lock_key = KEYS[1];
    local lock_flag = ARGV[1];
    
    --- 判断锁定的唯一标识与参数一致删除锁
    --- 返回值：1=解锁成功（删除成功），0=锁已失效或删除失败，-1=非自己的锁不支持解锁
    local val = redis.call('GET', lock_key);
    if (not val) then
        return 0;
    elseif (val == lock_flag) then
        return redis.call('DEL', lock_key);
    else
        return -1;
    end
    ```
- 测试 [`RedisSimpleLockUtilTest.java`](./src/test/java/cn/eastx/practice/demo/lock/util/RedisSimpleLockUtilTest.java)
- 缺陷
  - 只能单次加锁（唯一标识通过 ThreadLocal 存储，解锁时会清理 ThreadLocal，多次加解锁会导致与预期不符）
  - 不可重入
- 参考：https://github.com/realpdai/tech-pdai-spring-demos/blob/main/264-springboot-demo-redis-jedis-distribute-lock/src/main/java/tech/pdai/springboot/redis/jedis/lock/lock/RedisDistributedLock.java

### 2.2 Redis 分布式锁
> 使用 ThreadLocal 保存 锁key 与 相应的唯一标识
> <br> 加锁：使用 HASH 保存锁标识与加锁次数
> <br> 解锁：Lua 脚本判断是自己加的锁进行释放
> <br> 功能：可重入（Redis HASH）、支持对不同 key 进行加解锁（ThreadLocal<Map<String, String>>）

- Redis 分布式锁 [`RedisLockUtil.java`](./src/main/java/cn/eastx/practice/demo/lock/util/RedisLockUtil.java)
    ```java
    // 使用 ThreadLocal 保存 锁key 与 唯一标识
    private static final ThreadLocal<Map<String, String>> LOCK_FLAG =
            ThreadLocal.withInitial(HashMap::new);
    // 尝试加锁
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
            log.debug("tryLock, lock_flag={}, key={}, args={}, lockRes={}",
                    LOCK_FLAG.get(), key, args, lockRes);
            return lockRes != null ? lockRes : 0L;
        } catch (Exception e) {
            log.error("tryLock occurred an exception", e);
        }

        return 0L;
    }

    // 尝试解锁
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
            log.debug("unlock, key={}, args={}, unlockRes={}", key, args, unlockRes);
            lockNum = unlockRes != null ? unlockRes : 0L;
        } catch (Exception e) {
            log.error("release lock occurred an exception", e);
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
    ```
- Lua 脚本
    - 加锁 [`redis_lock.lua`](./src/main/resources/scripts/redis_lock.lua)
    - 解锁 [`redis_unlock.lua`](./src/main/resources/scripts/redis_unlock.lua)
- 测试 [`RedisLockUtilTest.java`](./src/test/java/cn/eastx/practice/demo/lock/util/RedisLockUtilTest.java)


