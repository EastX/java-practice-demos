<h1 align="center">
    <a href="https://github.com/EastX/java-practice-demos/tree/main/demo-cache">
        缓存实践示例
    </a>
</h1>

<p align="center">
    <a href="https://www.oracle.com/java/technologies/downloads/archive/">
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
- Cache Aside Pattern（旁路缓存，常用）
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

**推荐阅读：**
- [缓存那些事 - 美团技术团队 明辉](https://tech.meituan.com/2017/03/17/cache-about.html)
- [高并发之缓存 - 开拖拉机的蜡笔小新](https://www.cnblogs.com/xiangkejin/p/9277693.html)

## 2. 缓存工具
> 使用工具类封装具体处理，方便变更。

- [x] **本地缓存** [`LocalCacheUtil.java`](./src/main/java/cn/eastx/practice/demo/cache/util/LocalCacheUtil.java)
    - 使用 Caffeine 作为本地缓存封装
    - 支持对不同对象设置不同时长 `getCache()`，注意需要小于全局时长（优先）
- [x] **Redis 缓存** [`RedisUtil.java`](./src/main/java/cn/eastx/practice/demo/cache/util/RedisUtil.java)
    - 封装 RedisTemplate 常用操作，包括：string、hash、list、set、sorted set、HyperLogLog、GEO、key 相关、脚本
    - 注意值对象为 Object ，可能需要进行转换

## 3. 方法缓存注解组件
> **需求说明**
> 1. 通过在方法上增加缓存注解，调用方法时根据指定 key 缓存返回数据，再次调用从缓存中获取
> 2. 可通过注解指定不同的缓存时长
> 3. 避免缓存雪崩：可支持每个 key 增加随机时长
> 4. 避免缓存穿透：对于 null 支持短时间存储
> 5. 避免缓存击穿：缓存失效后限制查库数量

### 3.1 Spring Cache 整合 Redis
> 利用 Spring Cache 处理 Redis 缓存数据。

#### 1 ：配置注入缓存管理器时，根据配置或代码写死指定缓存时长
- yml 配置缓存空间名称与缓存时长对应关系 [`application-custom.yml`](./src/main/resources/application-custom.yml)
    ```yml
    # Spring Redis Cache 通过配置处理缓存空间名称过期时长
    # Map 接收，key = 缓存空间名称，value = 缓存时长，单位秒
    expand-cache-config:
      ttl-map: '{"yml-ttl":1000,"hello":2000}'
    ```
- Redis 配置自定义缓存管理器 [`ExpandRedisConfig.java`](./src/main/java/cn/eastx/practice/demo/cache/config/spring/ExpandRedisConfig.java)
    - 注意开启 Spring Cache 需要在配置类（或启动类）上增加 `@EnableCaching`
    - 核心处理：引入配置，注入缓存管理器及配置处理缓存文件中的缓存时长
    ```java
    // 引入配置
    @Value("#{${expand-cache-config.ttl-map:null}}")
    private Map<String, Long> ttlMap;
    // 注入缓存管理器及配置处理缓存文件中的缓存时长
    @Bean(BEAN_REDIS_CACHE_MANAGER)
    public RedisCacheManager expandRedisCacheManager(RedisConnectionFactory factory) {
        /*
            使用 Jackson 作为值序列化处理器
            FastJson 存在部分转换问题如：Set 存储后因为没有对应的类型保存无法转换为 JSONArray（实现 List ） 导致失败
        */
        ObjectMapper om = JsonUtil.createJacksonObjectMapper();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(om);

        // 配置key、value 序列化（解决乱码的问题）
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            // key 使用 string 序列化方式
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
            // value 使用 jackson 序列化方式
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
            // 配置缓存空间名称前缀
            .prefixCacheNameWith("spring:cache:")
            // 配置全局缓存过期时间
            .entryTtl(Duration.ofMinutes(30L));
        // 专门指定某些缓存空间的配置，如果过期时间，这里的 key 为缓存空间名称
        Set<Map.Entry<String, Long>> entrySet =
            Optional.ofNullable(ttlMap).map(Map::entrySet).orElse(Collections.emptySet());
        Map<String, RedisCacheConfiguration> configMap =
            Maps.newHashMapWithExpectedSize(entrySet.size());
        // 代码写死示例
        configMap.put("world", config.entryTtl(Duration.ofSeconds(60)));
        for (Map.Entry<String, Long> entry : entrySet) {
            //指定特定缓存空间对应的过期时间
            configMap.put(entry.getKey(), config.entryTtl(Duration.ofSeconds(entry.getValue())));
        }

        RedisCacheWriter redisCacheWriter = RedisCacheWriter.lockingRedisCacheWriter(factory);
        // 使用自定义缓存管理器附带自定义参数随机时间，注意此处为全局设定，5-最小随机秒，30-最大随机秒
        return new ExpandRedisCacheManager(redisCacheWriter, config, configMap, 5, 30);
    }
    ```

#### 2 ：利用缓存空间名附带时间，创建缓存处理器时更新缓存时长
- 使用示例：`@Cacheable(cacheNames = "prefix#5m", cacheManager = "expandRedisCacheManager")`
- 自定义 Redis 缓存管理器 [`ExpandRedisCacheManager.java`](./src/main/java/cn/eastx/practice/demo/cache/config/spring/ExpandRedisCacheManager.java)
    - 重写创建缓存执行器逻辑，支持缓存空间名称中附带缓存时间根据指定符号分隔
    ```java
    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        String theName = name;
        if (name.contains(NAME_SPLIT_SYMBOL)) {
            // 名称中存在#标记，修改实际名称，替换默认配置的缓存时长为指定缓存时长
            String[] nameArr = name.split(NAME_SPLIT_SYMBOL);
            theName = nameArr[0];
            Duration duration = TimeUtil.parseDuration(nameArr[1]);
            if (duration != null) {
                    cacheConfig = cacheConfig.entryTtl(duration);
            }
        }
    
        // 使用自定义缓存处理器附带自定义参数随机时间，将注入的随机时间传递
        return new ExpandRedisCache(theName, cacheWriter, cacheConfig, this.minRandomSecond,
                this.maxRandomSecond);
    }
    ```

#### 3 ：自定义缓存注解实现通过属性配置时长
- 自定义缓存注解 [`ExpandCacheable.java`](./src/main/java/cn/eastx/practice/demo/cache/config/spring/ExpandCacheable.java)
    - 使用 `@Cacheable` 标识，可支持 Spring Cache 处理
    - 增加 `spelKey()` 支持 SpEL key 生成处理，需要与自定义缓存 key 生成器搭配
    - 增加 `timeout() + unit()` 支持过期时间设置，需要与注入注解的初始化配置方生效
- 自定义缓存 key 生成器 [`ExpandKeyGenerator.java`](./src/main/java/cn/eastx/practice/demo/cache/config/spring/ExpandKeyGenerator.java)
    - 与自定义缓存注解 `spelKey()` 搭配处理支持 SpEL key 格式解析，注意使用后初始配置的缓存空间前缀仍会生效 `RedisCacheConfiguration.prefixCacheNameWith("spring:cache:")`
    - 插件式，有需要则在 `ExpandRedisConfig` 中进行注入
    ```java
    @Override
    public Object generate(Object target, Method method, Object... params) {
        ExpandCacheable annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, ExpandCacheable.class);
        if (Objects.isNull(annotation) || StrUtil.isBlank(annotation.spelKey())) {
            String paramStr = JSONUtil.toJsonStr(params).replace("\"", "");
            return AspectUtil.getMethodKey(target, method, paramStr);
        }

        // SpEL 支持
        return AspectUtil.convertSpelValue(annotation.spelKey(), method, params, String.class);
    }
    ```
- 自定义缓存注解过期时间初始化配置 [`ExpandCacheExpireConfig.java`](./src/main/java/cn/eastx/practice/demo/cache/config/spring/ExpandCacheExpireConfig.java)
    - 与自定义缓存注解 `timeout() + unit()` 对应处理
    - 有需要则在 ExpandRedisConfig 中进行注入
    - 利用 Spring Component Bean 获取到使用 `@ExpandCacheable` 注解的方法，利用反射获取注解属性并设置缓存空间过期时间；
      Map 处理，同一名称缓存空间将会出现替换情景
    ```java
    // Spring Bean 加载后，获取所有 @Component 注解的 Bean 判断类中方法是否存在 @SpringCacheable 注解，存在进行过期时间设置
    @PostConstruct
    public void init() {
        Map<String, Object> beanMap = beanFactory.getBeansWithAnnotation(Component.class);
        if (MapUtil.isEmpty(beanMap)) {
            return;
        }

        beanMap.values().forEach(item ->
            ReflectionUtils.doWithMethods(item.getClass(), method -> {
                ReflectionUtils.makeAccessible(method);
                putConfigTtl(method);
            })
        );

        expandRedisCacheManager.initializeCaches();
    }
    // 利用反射设置方法注解上配置的过期时间
    private void putConfigTtl(Method method) {
        ExpandCacheable annotation = method.getAnnotation(ExpandCacheable.class);
        if (annotation == null) {
            return;
        }

        String[] cacheNames = annotation.cacheNames();
        if (ArrayUtil.isEmpty(cacheNames)) {
            cacheNames = annotation.value();
        }

        // 反射获取缓存管理器初始化配置并设值
        Map<String, RedisCacheConfiguration> initialCacheConfiguration =
                (Map<String, RedisCacheConfiguration>)
                        ReflectUtil.getFieldValue(expandRedisCacheManager, "initialCacheConfiguration");
        RedisCacheConfiguration defaultCacheConfig =
                (RedisCacheConfiguration)
                        ReflectUtil.getFieldValue(expandRedisCacheManager, "defaultCacheConfig");
        Duration ttl = Duration.ofSeconds(annotation.unit().toSeconds(annotation.timeout()));
        for (String cacheName : cacheNames) {
            initialCacheConfiguration.put(cacheName, defaultCacheConfig.entryTtl(ttl));
        }
    }
    ```

#### 4 ：继承缓存处理器执行缓存设置时增加随机时长
- 自定义 Redis 缓存执行器 [`ExpandRedisCache.java`](./src/main/java/cn/eastx/practice/demo/cache/config/spring/ExpandRedisCache.java)
  - 重写缓存处理器设值逻辑，支持调整配置时长增加随机时间、null 存储短时间
    ```java
    @Override
    public void put(Object key, @Nullable Object value) {
      Object cacheValue = preProcessCacheValue(value);
      // 替换父类设置缓存时长处理
      Duration duration = getDynamicDuration(cacheValue);
      cacheWriter.put(name, createAndConvertCacheKey(key),
      serializeCacheValue(cacheValue), duration);
    }
    
    // 获取动态时长
    private Duration getDynamicDuration(Object cacheValue) {
        // 如果缓存值为 null，固定返回时长为 30s 避免缓存穿透
        if (NullValue.INSTANCE.equals(cacheValue)) {
            return Duration.ofSeconds(30);
        }

        int randomInt = RandomUtil.randomInt(this.minRandomSecond, this.maxRandomSecond);
        return this.cacheConfig.getTtl().plus(Duration.ofSeconds(randomInt));
    }
    ```

**测试**
- Spring Cache 整合 Redis 处理示例 Controller [`ExpandSpringCacheController.java`](./src/main/java/cn/eastx/practice/demo/cache/controller/ExpandSpringCacheController.java)
    - 示例代码
    ```java
    // 使用 @Cacheable
    @GetMapping("/spel2")
    @Cacheable(cacheNames = "prefix#5m", cacheManager = ExpandRedisConfig.BEAN_REDIS_CACHE_MANAGER, 
            keyGenerator = ExpandRedisConfig.BEAN_KEY_GENERATOR)
    public ResponseResult<String> spel2(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:spel2:{}", id);
        return ResponseResult.success("hello:spel2:" + id);
    }

    // 使用 @ExpandCacheable
    @GetMapping("/spel3")
    @ExpandCacheable(cacheNames = "prefix", spelKey = "hello-spel3-${#id}", timeout = 100, 
            unit = TimeUnit.SECONDS)
    public ResponseResult<String> spel3(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:spel3:{}", id);
        return ResponseResult.success("hello:spel3:" + id);
    }
    ```
- Spring Cache 整合 Redis 处理示例 Controller 测试 [`ExpandSpringCacheControllerTest.java`](./src/test/java/cn/eastx/practice/demo/cache/controller/ExpandSpringCacheControllerTest.java)

**小结**
- 优点
    - 使用 Spring 自带功能，通用性强
- 缺点
    - 针对缓存空间处理缓存时长，缓存时间一致可能导致缓存雪崩，自定义处理需要理解相应源码实现

**参考：**
- [Spring cache整合Redis，并给它一个过期时间！](https://zhuanlan.zhihu.com/p/138295935)
- [让 @Cacheable 可配置 Redis 过期时间](https://juejin.cn/post/7062155187200196644)
- [@Cacheable注解配合Redis设置缓存随机失效时间](https://blog.csdn.net/yang_wen_wu/article/details/120348727)
- [聊聊如何基于spring @Cacheable扩展实现缓存自动过期时间以及自动刷新](https://mp.weixin.qq.com/s/zzJH-enXlLZovV8h0RCR6Q)
- [SpringBoot实现Redis缓存（SpringCache+Redis的整合）](https://blog.csdn.net/user2025/article/details/106595257)

### 3.2 自定义 AOP 实现
> 使用 Spring AOP 切面对注解拦截，处理方法调用结果缓存。

**核心类：**
- 方法缓存注解 [`MethodCacheable.java`](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheable.java)
    - 支持不同类型缓存 key： `key() + keyType()`
    - 支持依据条件( SpEL 表达式)设定排除不走缓存： `unless()`
    - 支持缓存 key 自定义过期时长（ Redis 缓存）： `timeout() + unit()`
    - 支持缓存 key 自定义过期时长增加随机时长（ Redis 缓存）： `addRandomDuration()` ，注意固定了随机范围
    - 支持本地缓存设置：`useLocal() + localTimeout()` ，注意本地缓存存在全局最大时长限制
- 方法缓存注解操作类 [`MethodCacheableOperation.java`](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheableOperation.java)
    - 转换注解为操作对象，AOP 实际使用操作对象进行处理
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
- 方法缓存 AOP 处理 [`MethodCacheAspect.java`](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheAspect.java)
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

        // 加锁处理同步执行
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

**测试使用：**
- 自定义缓存处理示例 Controller [`CustomCacheController.java`](./src/main/java/cn/eastx/practice/demo/cache/controller/CustomCacheController.java)
    - 示例代码
    ```java
    @GetMapping("/default/all")
    @MethodCacheable(key = "hello-all", keyType = AspectKeyTypeEnum.DEFAULT, unless = "${#id<0}",
            timeout = 300, unit = TimeUnit.SECONDS, addRandomDuration = false, useLocal = true,
            localTimeout = 60)
    public ResponseResult<String> exactMatchAll(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:all:{}", id);
        return ResponseResult.success("custom:all:" + id);
    }
    ```
- 自定义缓存处理示例 Controller 测试 [`CustomCacheControllerTest.java`](./src/test/java/cn/eastx/practice/demo/cache/controller/CustomCacheControllerTest.java)

**小结**
- 优点
  - 自定义 Spring AOP 实现，可定制化处理程度较高
  - 支持两级缓存（分布式缓存+本地缓存）
- 缺点
  - 相对于 Spring 自带 Cache ，部分功能存在缺失

