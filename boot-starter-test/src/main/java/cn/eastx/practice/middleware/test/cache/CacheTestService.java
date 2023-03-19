package cn.eastx.practice.middleware.test.cache;

import cn.eastx.practice.middleware.annotation.MethodCacheable;
import cn.eastx.practice.middleware.constant.AspectKeyTypeEnum;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存测试业务
 *
 * @author EastX
 * @date 2023/3/19
 */
@Component
public class CacheTestService {

    private Logger logger = LoggerFactory.getLogger(CacheTestService.class);

    /**
     * 测试 DEFAULT 格式 key 缓存
     *  key=aop:method:cache:hello-world
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(key = "hello-world", keyType = AspectKeyTypeEnum.DEFAULT, useLocal = true)
    public String exactMatch(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:default:{}", id);
        return "custom:default:" + id;
    }

    /**
     * 测试 DEFAULT 格式 key 缓存（全部参数设定）
     *  key=aop:method:cache:hello-all
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(key = "hello-all", keyType = AspectKeyTypeEnum.DEFAULT, unless = "${#id<0}",
            timeout = 300, unit = TimeUnit.SECONDS, addRandomDuration = false, useLocal = true,
            localTimeout = 60)
    public String exactMatchAll(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:all:{}", id);
        return "custom:all:" + id;
    }

    /**
     * 测试 DEFAULT 格式 key 缓存 null
     *  key=aop:method:cache:hello-null
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(key = "hello-null", keyType = AspectKeyTypeEnum.DEFAULT, useLocal = false)
    public String exactMatchNull(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:default:null:{}", id);
        return null;
    }

    /**
     * 测试 METHOD 格式 key 缓存
     *  key=aop:method:cache:CustomCacheController#method
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(keyType = AspectKeyTypeEnum.METHOD, useLocal = true)
    public String method(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:method:{}", id);
        return "custom:method:" + id;
    }

    /**
     * 测试 METHOD_PARAM 格式 key 缓存
     *  key=aop:method:cache:CustomCacheController#methodParam([10086])
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(keyType = AspectKeyTypeEnum.METHOD_PARAM, useLocal = false)
    public String methodParam(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:method-param:{}", id);
        return "custom:method-param:" + id;
    }

    /**
     * 测试 METHOD_SPEL_PARAM 格式 key 缓存
     *  key=aop:method:cache:CustomCacheController#methodSpelParam(custom:method-spel-param:10086)
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(key = "custom:method-spel-param:${#id}",
            keyType = AspectKeyTypeEnum.METHOD_SPEL_PARAM, useLocal = true)
    public String methodSpelParam(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:method-spel-param:{}", id);
        return "custom:method-spel-param:" + id;
    }

    /**
     * 测试 SPEL 格式 key 缓存
     *  key=aop:method:cache:custom:spel:10086
     *
     * @param id 传入id
     * @return 返回结果
     */
    @MethodCacheable(key = "custom:spel:${#id}", keyType = AspectKeyTypeEnum.SPEL, useLocal = false)
    public String spel(Long id) {
        id += RandomUtils.nextLong();
        logger.info("custom:spel:{}", id);
        return "custom:spel:" + id;
    }

    /**
     * 测试 配置 SpelContext 处理
     *  key=aop:method:cache:hello-world
     *
     * @return 返回结果
     */
    @MethodCacheable(key = "${#_RAND}", keyType = AspectKeyTypeEnum.SPEL, useLocal = true)
    public String randNum() {
        Long id = RandomUtils.nextLong();
        logger.info("custom:_RAND:{}", id);
        return "custom:default:" + id;
    }

}
