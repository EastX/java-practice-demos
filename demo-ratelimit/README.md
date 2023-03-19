<h1 align="center">
    <a href="https://github.com/EastX/java-practice-demos/tree/main/demo-ratelimit">
        限流实践示例
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

## 1. 限流概述
> 系统存在服务上限，流量超过服务上限会导致系统卡死、崩溃。
> <br> 限流：为了在高并发时系统稳定可用，牺牲或延迟部分请求流量以保证系统整体服务可用。

**限流算法**
- 固定窗口计数
    - 将时间划分为多个窗口；
    - 在每个窗口内每有一次请求就将计数器加一；
    - 如果计数器超过了限制数量，则本窗口内所有的请求都被丢弃当时间到达下一个窗口时，计数器重置。
- 滑动窗口计数
    - 将时间划分为多个区间；
    - 在每个区间内每有一次请求就将计数器加一维持一个时间窗口，占据多个区间；
    - 每经过一个区间的时间，则抛弃最老的一个区间，并纳入最新的一个区间；
    - 如果当前窗口内区间的请求计数总和超过了限制数量，则本窗口内所有的请求都被丢弃。
- 漏桶
    - 将每个请求视作"水滴"放入"漏桶"进行存储；
    - "漏桶"以固定速率向外"漏"出请求来执行如果"漏桶"空了则停止"漏水"；
    - 如果"漏桶"满了则多余的"水滴"会被直接丢弃。
- 令牌桶
    - 令牌以固定速率生成；
    - 生成的令牌放入令牌桶中存放，如果令牌桶满了则多余的令牌会直接丢弃，当请求到达时，会尝试从令牌桶中取令牌，取到了令牌的请求可以执行；
    - 如果桶空了，那么尝试取令牌的请求会被直接丢弃。

**漏桶和令牌桶对比**
- 两者实际上是相同的
    - 在实现上是相同的基本算法，描述不同。
    - 给定等效参数的情况下，这两种算法会将完全相同的数据包视为符合或不符合。
    - 两者实现的属性和性能差异完全是由于实现的差异造成的，即它们不是源于底层算法的差异。
- 漏桶算法在用作计量时，可以允许具有抖动或突发性的一致输出数据包流，可用于流量管制和整形，并且可以用于可变长度数据包。
- 参考：
    - [漏桶 - wikipedia](https://en.wikipedia.org/wiki/Leaky_bucket)
    - [令牌桶 - wikipedia](https://en.wikipedia.org/wiki/Token_bucket)

**相关阅读：**
- [分布式服务限流实战，已经为你排好坑了](https://www.infoq.cn/article/Qg2tX8fyw5Vt-f3HH673)
- [接口限流算法总结 - 穿林度水](https://www.cnblogs.com/clds/p/5850070.html)


## 2. 限流注解组件
> **说明**
> 1. 利用 Spring 拦截器实现。
> 2. 使用方式：Controller 方法或类加上限流注解，请求到达拦截器时进行拦截处理。
> 3. 使用 Redis 记录数据，Lua 保证多个命令原子性执行。

**核心：**
- 限流注解 [`RateLimit.java`](./src/main/java/cn/eastx/practice/demo/ratelimit/config/custom/RateLimit.java)
    - 支持不同类型缓存 key： `key() + keyType()`
    - 支持使用不同限流算法： `rateLimiter()`
    - 限流流量阈值设置： `threshold()`
    - 限流时长设置： `time() + timeUnit()`
- 限流拦截器处理 [`RateLimitInterceptor.java`](./src/main/java/cn/eastx/practice/demo/ratelimit/config/custom/RateLimitInterceptor.java)
    ```java
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = ((HandlerMethod) handler);
        // 从方法和类上获取注解
        RateLimit annotation = AspectUtil.findMethodOrClassAnnotation(handlerMethod.getMethod(),
                RateLimit.class);
        if (annotation == null) {
            return true;
        }

        AspectKeyTypeEnum.KeyTypeData data = AspectKeyTypeEnum.KeyTypeData.builder()
                .prefix("rate:limit").key(annotation.key()).build();
        String limitKey = annotation.keyType()
                .obtainTypeKey(handlerMethod.getMethod(), handlerMethod.getMethodParameters(), data);
        RateLimiterEnum limiterEnum = annotation.rateLimiter();

        // 执行限流脚本
        Long isLimit = redisUtil.execute(limiterEnum.obtainScript(),
                Lists.newArrayList(limitKey), limiterEnum.obtainArgvs(annotation).toArray());
        if (isLimit != null && isLimit != 0L) {
            return true;
        }

        throw new ResponseException(ResponseEnum.RATE_LIMITED);
    }
    ```
- 限流算法 lua 脚本
    - 固定窗口： [`fixed_window_rate_limiter.lua`](./src/main/resources/scripts/fixed_window_rate_limiter.lua)
    - 滑动窗口： [`sliding_window_rate_limiter.lua`](./src/main/resources/scripts/sliding_window_rate_limiter.lua)
    - 漏桶： [`leaky_bucket_rate_limiter.lua`](./src/main/resources/scripts/leaky_bucket_rate_limiter.lua)
    - 令牌桶： [`token_bucket_rate_limiter.lua`](./src/main/resources/scripts/token_bucket_rate_limiter.lua)

**测试：**
- 限流处理示例 Controller [`RateLimitController.java`](./src/main/java/cn/eastx/practice/demo/ratelimit/controller/RateLimitController.java)
    - 示例代码
    ```java
    @GetMapping("/fixed/window")
    @RateLimit(threshold = 10, rateLimiter = RateLimiterEnum.FIXED_WINDOW, time = 10, timeUnit = TimeUnit.SECONDS)
    public ResponseResult<String> fixedWindow(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:fixedWindow:{}", id);
        return ResponseResult.success("custom:fixedWindow:" + id);
    }
    ```
- 限流处理示例 Controller 测试 [`RateLimitControllerTest.java`](./src/test/java/cn/eastx/practice/demo/ratelimit/controller/RateLimitControllerTest.java)


