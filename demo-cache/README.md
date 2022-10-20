# 缓存相关示例

## 1. 缓存工具类
> 使用工具类封装具体处理，方便变更。

- [x] 本地缓存 [`LocalCacheUtil.java`](./src/main/java/cn/eastx/practice/demo/cache/util/LocalCacheUtil.java)
- [x] Redis 缓存 [`RedisUtil.java`](./src/main/java/cn/eastx/practice/demo/cache/util/RedisUtil.java)

## 2. 方法缓存
### 2.1 Spring Cache 整合 Redis

不足:
- 对每个key设定不同的过期时间处理较为生硬
- 缓存时间一致可能导致缓存雪崩

参考：
- [Spring cache整合Redis，并给它一个过期时间！](https://zhuanlan.zhihu.com/p/138295935)
- [让 @Cacheable 可配置 Redis 过期时间](https://juejin.cn/post/7062155187200196644)
- [@Cacheable注解配合Redis设置缓存随机失效时间](https://blog.csdn.net/yang_wen_wu/article/details/120348727)

### 2.2 自定义实现
> 使用 AOP 切面对方法调用结果进行缓存。

实现方式：Spring AOP + 注解

逻辑设定：通过在方法上增加缓存注解，调用方法时根据指定key缓存返回数据，再次调用从缓存中获取

核心类：
- 方法缓存注解 [`MethodCacheable.java`](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheable.java)
- 方法缓存注解操作类 [`MethodCacheableOperation.java`](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheableOperation.java)
- 方法缓存 AOP 处理 [`MethodCacheAspect.java`](./src/main/java/cn/eastx/practice/demo/cache/config/custom/MethodCacheAspect.java)

测试：
- 自定义缓存处理示例 Controller [`CustomCacheController.java`](./src/main/java/cn/eastx/practice/demo/cache/controller/CustomCacheController.java)
- 自定义缓存处理示例 Controller 测试 [`CustomCacheControllerTest.java`](./src/test/java/cn/eastx/practice/demo/cache/controller/CustomCacheControllerTest.java)
