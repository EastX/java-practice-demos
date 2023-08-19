package cn.eastx.practice.demo.cache.controller;

import cn.eastx.practice.common.response.ResponseResult;
import cn.eastx.practice.demo.cache.config.spring.ExpandCacheable;
import cn.eastx.practice.demo.cache.config.spring.ExpandRedisConfig;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cache 整合 Redis 处理示例
 *
 * @author EastX
 * @date 2022/10/22
 */
@Slf4j
@RestController
@RequestMapping("/cache/spring")
public class ExpandSpringCacheController {

    /**
     * 测试 SpEL 格式 key 缓存（使用 @SpringCacheable ）
     * key=spring:cache:prefix::hello-spel-1008611
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/spel")
    @ExpandCacheable(cacheNames = "prefix#5m", spelKey = "hello-spel-${#id}")
    public ResponseResult<String> spel(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:spel:{}", id);
        return ResponseResult.success("hello:spel:" + id);
    }

    /**
     * 测试 SpEL 格式 key 缓存（使用 @Cacheable ）
     * key=spring:cache:prefix::ExpandSpringCacheController#spel2([1008612])
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/spel2")
    @Cacheable(cacheNames = "prefix#5m", cacheManager = ExpandRedisConfig.BEAN_REDIS_CACHE_MANAGER, 
            keyGenerator = ExpandRedisConfig.BEAN_KEY_GENERATOR)
    public ResponseResult<String> spel2(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:spel2:{}", id);
        return ResponseResult.success("hello:spel2:" + id);
    }

    /**
     * 测试 SpEL 格式 key 缓存
     * key=spring:cache:prefix::hello-spel3-1008613
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/spel3")
    @ExpandCacheable(cacheNames = "prefix", spelKey = "hello-spel3-${#id}", timeout = 100, 
            unit = TimeUnit.SECONDS)
    public ResponseResult<String> spel3(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:spel3:{}", id);
        return ResponseResult.success("hello:spel3:" + id);
    }

    /**
     * 测试 SpEL 格式 key 缓存（使用 @Cacheable ）
     * key=spring:cache:prefix::hello-ttl-1008613
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/ttl")
    @Cacheable(cacheNames = "world", cacheManager = ExpandRedisConfig.BEAN_REDIS_CACHE_MANAGER, 
            keyGenerator = ExpandRedisConfig.BEAN_KEY_GENERATOR)
    public ResponseResult<String> ttl(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:ttl:{}", id);
        return ResponseResult.success("hello:ttl:" + id);
    }

    /**
     * 测试 SpEL 格式 key 缓存（使用 @Cacheable ）
     * key=spring:cache:prefix::ExpandSpringCacheController#spel2([1008612])
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/ttl2")
    @Cacheable(cacheNames = "yml-ttl", cacheManager = ExpandRedisConfig.BEAN_REDIS_CACHE_MANAGER, 
            keyGenerator = ExpandRedisConfig.BEAN_KEY_GENERATOR)
    public ResponseResult<String> ttl2(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:ttl2:{}", id);
        return ResponseResult.success("hello:ttl2:" + id);
    }

    /**
     * 测试 SpEL 格式 key 缓存（使用 @Cacheable ）
     * key=spring:cache:prefix::ExpandSpringCacheController#spel2([1008612])
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/ttl3")
    @Cacheable(cacheNames = "hello", cacheManager = ExpandRedisConfig.BEAN_REDIS_CACHE_MANAGER, 
            keyGenerator = ExpandRedisConfig.BEAN_KEY_GENERATOR)
    public ResponseResult<String> ttl3(Long id) {
        id += RandomUtil.randomLong();
        log.info("hello:ttl3:{}", id);
        return ResponseResult.success("hello:ttl3:" + id);
    }

}
