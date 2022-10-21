<h1 align="center">
    <a href="https://github.com/EastX/java-practice-demos/tree/main/demo-cache">
        缓存实践示例
    </a>
</h1>

<p align="center">
    <a href="https://www.oracle.com/technetwork/java/javase/downloads/index.html">
        <img alt="JDK" src="https://img.shields.io/badge/JDK-1.8.0_201-e67621.svg"/>
    </a>
    <a href="https://docs.spring.io/spring-boot/docs/2.7.2/reference/html/">
        <img alt="Spring Boot" src="https://img.shields.io/badge/Spring Boot-2.7.2-6db33f.svg"/>
    </a>
    <a href="https://redis.io/">
        <img alt="Redis" src="https://img.shields.io/badge/Redis-6.0 lettuce-dc382c.svg"/>
    </a>
    <a href="https://github.com/ben-manes/caffeine">
        <img alt="Caffeine" src="https://img.shields.io/badge/Caffeine-2.9.3-6db33f.svg"/>
    </a>
</p>

## 1. 缓存概述
> 解决不同设备间速度不匹配问题。
> 互联网分层架构：降低数据库压力，提升系统整体性能，缩短访问时间。

**读写策略**
- Cache Aside Pattern（旁路缓存）
    - 写，更新 DB ，删除 cache读，cache
    - 读，读不到从 DB 读，把数据放到 cache
    - 缺陷
        - 首次请求数据不在 cache 中；热点数据提前放入
            - 写操作频繁影响命中率；强一致性更新加锁，短暂不一致更新 DB 同时更新 cache 设定较短
        - 保证数据一致性（更新 DB 成功，删除 缓存失败）
            - 缓存失效时间变短（不推荐，治标不治本），对于先操作缓存后操作数据库的场景不适用
            - 增加 cache 更新重试机制（常用），多次重试失败可以存入队列中，等缓存可用删除
- Read/Write Through Pattern（读写穿透）
    - 写，查 cache，cache 中不存在直接更新 DB，否则更新 cache （cache 服务自己同步更新 DB）
    - 读，cache 读，读不到从 DB 加载，写入 cache 后返回
- Write Behind Pattern（异步缓存写入）
    - cache 负责 DB 读写，只更新 cache，异步批量处理更新 DB
    - 消息队列中消息异步写入磁盘，MySQL 中 InnoDB Buffer Pool 机制
    - 写性能高，适合数据经常变化又有一致性要求没那么高的场景，如浏览量、点赞量

**高并发问题**
- 缓存一致性问题：当数据时效性要求很高时，需要保证缓存中的数据与数据库中的保持一致
- 缓存并发问题（击穿）：缓存过期后将尝试从后端数据库获取数据，高并发导致对数据库造成大冲击
    - 缓存永不失效
    - 互斥锁，拿到锁才能查询数据库，降低落库请求，会导致系统性能变差
- 缓存穿透问题：不存在的 key，请求直接落库查询，高并发导致对数据库造成大冲击
    - 参数校验，提前排除
    - 缓存无效 key ，设置较短过期时间
    - 布隆过滤器，存放所有存在的数据
- 缓存雪崩问题：缓存大面积失效，请求直接落库，对数据库造成大冲击
    - 缓存模块出了问题如宕机
        - 采用 Redis 集群，避免单机出现问题整个缓存服务不可用
        - 限流，避免同时处理大量请求大量访问数据（热点缓存）某一时刻大面积失效
        - 设置不同失效时间
    - 缓存永不失效

**参考：**
- [缓存那些事 - 美团技术团队 明辉](https://tech.meituan.com/2017/03/17/cache-about.html)
- [高并发之缓存 - 开拖拉机的蜡笔小新](https://www.cnblogs.com/xiangkejin/p/9277693.html)

## 2. 缓存工具
> 使用工具类封装具体处理，方便变更。

- [x] **本地缓存** [++`LocalCacheUtil.java`++](./src/main/java/cn/eastx/practice/demo/cache/util/LocalCacheUtil.java)
    - 使用 Caffeine 作为本地缓存封装
    - 支持对不同对象设置不同时长 `getCache()`，注意需要小于全局时长（优先）
- [x] **Redis 缓存** [++`RedisUtil.java`++](./src/main/java/cn/eastx/practice/demo/cache/util/RedisUtil.java)
    - 封装 RedisTemplate 常用操作，包括：string、hash、list、set、sorted set、HyperLogLog、GEO、key 相关、脚本
    - 注意值对象为 Object ，可能需要进行转换

## 3. 方法缓存组件
> **需求说明**
> 1. 通过在方法上增加缓存注解，调用方法时根据指定 key 缓存返回数据，再次调用从缓存中获取
> 2. 可通过注解指定不同的缓存时长
> 3. 避免缓存雪崩：可支持每个 key 增加随机时长
> 4. 避免缓存穿透：对于 null 支持短时间存储
> 5. 避免缓存击穿：缓存失效后限制查库数量

### 2.1 Spring Cache 整合 Redis

**不足:**
- 对每个key设定不同的过期时间处理较为生硬
- 缓存时间一致可能导致缓存雪崩

**参考：**
- [Spring cache整合Redis，并给它一个过期时间！](https://zhuanlan.zhihu.com/p/138295935)
- [让 @Cacheable 可配置 Redis 过期时间](https://juejin.cn/post/7062155187200196644)
- [@Cacheable注解配合Redis设置缓存随机失效时间](https://blog.csdn.net/yang_wen_wu/article/details/120348727)
- [聊聊如何基于spring @Cacheable扩展实现缓存自动过期时间以及自动刷新](https://mp.weixin.qq.com/s/zzJH-enXlLZovV8h0RCR6Q)

### 2.2 自定义实现
> 使用 AOP 切面对方法调用结果进行缓存。

**实现方式：** Spring AOP + 注解

**核心类：**
- 方法缓存注解 [++`MethodCacheable.java`++](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheable.java)
    - 支持不同类型缓存 key： `key() + keyType()`
    - 支持依据条件( SpEL 表达式)设定排除不走缓存： `unless()`
    - 支持缓存 key 自定义过期时长（ Redis 缓存）： `timeout() + unit()`
    - 支持缓存 key 自定义过期时长增加随机时长（ Redis 缓存）： `addRandomDuration()` ，注意固定了随机范围
    - 支持本地缓存设置：`useLocal() + localTimeout()` ，注意本地缓存存在全局最大时长限制
- 方法缓存注解操作类 [++`MethodCacheableOperation.java`++](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheableOperation.java)
    - 核心代码：转换注解为操作对象
        ```java
        public static MethodCacheableOperation convert(ProceedingJoinPoint joinPoint) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            MethodCacheable annotation = method.getAnnotation(MethodCacheable.class);
            if (Objects.isNull(annotation)) {
                return null;
            }
        
            Boolean isUnless = AspectUtil.convertSpelValue(annotation.unless(), joinPoint,
                Boolean.class);
            if (Boolean.TRUE.equals(isUnless)) {
                // 匹配条件不满足
                return null;
            }
        
            AspectKeyTypeEnum.KeyTypeData data = AspectKeyTypeEnum.KeyTypeData.builder()
                .prefix("method:cache").key(annotation.key()).build();
            String key = annotation.keyType().obtainTypeKey(joinPoint, data);
            if (StrUtil.isBlank(key)) {
                return null;
            }
        
            Duration duration = Duration.ofSeconds(annotation.unit().toSeconds(annotation.timeout()));
            if (annotation.addRandomDuration()) {
                // 增加随机时长 5 - 30 秒
                duration = duration.plusSeconds(ThreadLocalRandom.current().nextInt(5, 30));
            }
        
            MethodCacheableOperation operation = new MethodCacheableOperation();
            operation.setKey(key);
            operation.setDuration(duration);
            operation.setUseLocal(annotation.useLocal());
            operation.setLocalDuration(annotation.localTimeout());
            return operation;
        }
        ```
- 方法缓存 AOP 处理 [++`MethodCacheAspect.java`++](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheAspect.java)
    - 核心代码
        ```java
        @Around("@annotation(cn.eastx.practice.demo.cache.config.custom.MethodCacheable)")
        public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
            MethodCacheableOperation operation = MethodCacheableOperation.convert(joinPoint);
            if (Objects.isNull(operation)) {
                return joinPoint.proceed();
            }
        
            Object result = getCacheData(operation);
            if (Objects.nonNull(result)) {
                return convertCacheData(result);
            }
        
            // 加锁减少重复查库
            synchronized (operation.getKey().intern()) {
                result = getCacheData(operation);
                if (Objects.nonNull(result)) {
                return convertCacheData(result);
                }
        
                result = joinPoint.proceed();
                setDataCache(operation, result);
            }
        
            return result;
        }
        ```

**测试：**
- 自定义缓存处理示例 Controller [++`CustomCacheController.java`++](./src/main/java/cn/eastx/practice/demo/cache/controller/CustomCacheController.java)
    - 示例代码
        ```java
        @MethodCacheable(key = "hello-all", keyType = AspectKeyTypeEnum.DEFAULT, unless = "${#id<0}", timeout = 300, unit = TimeUnit.SECONDS, addRandomDuration = false, useLocal = true, localTimeout = 60)
        public ResponseResult<String> exactMatchAll(Long id) {
            id += RandomUtil.randomLong();
            log.info("custom:all:{}", id);
            return ResponseResult.success("custom:all:" + id);
        }
        ```
- 自定义缓存处理示例 Controller 测试 [++`CustomCacheControllerTest.java`++](./src/test/java/cn/eastx/practice/demo/cache/controller/CustomCacheControllerTest.java)

