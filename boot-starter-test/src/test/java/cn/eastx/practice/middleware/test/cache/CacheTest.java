package cn.eastx.practice.middleware.test.cache;

import cn.eastx.practice.middleware.util.JsonUtil;
import cn.eastx.practice.middleware.util.LocalCacheUtil;
import cn.eastx.practice.middleware.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * 缓存相关测试
 *
 * @author EastX
 * @date 2022/10/20
 */
@SpringBootTest
public class CacheTest {

    private Logger logger = LoggerFactory.getLogger(CacheTest.class);

    @Resource
    private CacheTestService cacheTestService;
    @Resource
    private RedisUtil redisUtil;

    @Test
    public void test_method_cache() throws Exception {
        // DEFAULT
        check("aop:method:cache:hello-world", "exactMatch");
        // METHOD
        check("aop:method:cache:CustomCacheController#method", "method");
        // METHOD_PARAM
        check("aop:method:cache:CustomCacheController#methodParam([10086])", "methodParam");
        // METHOD_SPEL_PARAM
        check("aop:method:cache:CustomCacheController#methodSpelParam(test:method-spel-param:10086)", "methodSpelParam");
        // SPEL
        check("aop:method:cache:test:spel:10086", "spel");
        // DEFAULT ALL
        check("aop:method:cache:hello-all", "exactMatchAll");
        // DEFAULT null
        check("aop:method:cache:hello-null", "exactMatchNull");

        // _RAND
        cacheTestService.randNum();
        logger.debug("localCacheMap={}", LocalCacheUtil.asMap());
    }

    /**
     * 校验处理
     *
     * @param cacheKey 缓存key
     * @param methodName 方法名称
     * @throws Exception 模拟请求可能抛出异常
     */
    private void check(String cacheKey, String methodName) throws Exception {
        // 清除缓存
        redisUtil.delete(cacheKey);
        LocalCacheUtil.invalidate(cacheKey);

        Method method = CacheTestService.class.getMethod(methodName, Long.class);

        Object res1 = method.invoke(cacheTestService, 10086L);
        Object res2 = method.invoke(cacheTestService, 10086L);
        logger.debug("res1={}, res2={}", res1, res2);
        Assert.isTrue(Objects.equals(res1, res2), "两次执行返回不一致");

        Object redisData = Optional.ofNullable(redisUtil.get(cacheKey))
                .filter(data -> !"NULL-VALUE".equals(data)).orElse(null);
        String redisCacheData = JsonUtil.toJsonStr(redisData);
        String localCacheData = JsonUtil.toJsonStr(LocalCacheUtil.getIfPresent(cacheKey));
        logger.debug("redisCacheData={}, localCacheData={}", redisCacheData, localCacheData);
        if (Objects.nonNull(localCacheData)) {
            Assert.isTrue(Objects.equals(redisCacheData, localCacheData), "redis缓存与本地缓存数据不一致");
        }
    }

}
