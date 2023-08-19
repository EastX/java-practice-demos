package cn.eastx.practice.demo.cache.controller;

import cn.eastx.practice.common.response.ResponseResult;
import cn.eastx.practice.demo.cache.config.custom.MethodCacheable;
import cn.eastx.practice.demo.cache.constants.AspectKeyTypeEnum;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 自定义缓存处理示例
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
@RestController
@RequestMapping("/cache/custom")
public class CustomCacheController {

    /**
     * 测试 DEFAULT 格式 key 缓存
     *  key=aop:method:cache:hello-world
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/default")
    @MethodCacheable(key = "hello-world", keyType = AspectKeyTypeEnum.DEFAULT, useLocal = false)
    public ResponseResult<String> exactMatch(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:default:{}", id);
        return ResponseResult.success("custom:default:" + id);
    }

    /**
     * 测试 DEFAULT 格式 key 缓存（全部参数设定）
     *  key=aop:method:cache:hello-all
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/default/all")
    @MethodCacheable(key = "hello-all", keyType = AspectKeyTypeEnum.DEFAULT, unless = "${#id<0}",
            timeout = 300, unit = TimeUnit.SECONDS, addRandTtl = false, useLocal = true,
            localTimeout = 60)
    public ResponseResult<String> exactMatchAll(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:all:{}", id);
        return ResponseResult.success("custom:all:" + id);
    }

    /**
     * 测试 DEFAULT 格式 key 缓存 null
     *  key=aop:method:cache:hello-null
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/default/null")
    @MethodCacheable(key = "hello-null", keyType = AspectKeyTypeEnum.DEFAULT, useLocal = false)
    public ResponseResult<String> exactMatchNull(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:default:null:{}", id);
        return null;
    }

    /**
     * 测试 METHOD 格式 key 缓存
     *  key=aop:method:cache:CustomCacheController#method
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/method")
    @MethodCacheable(keyType = AspectKeyTypeEnum.METHOD, useLocal = true)
    public ResponseResult<String> method(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:method:{}", id);
        return ResponseResult.success("custom:method:" + id);
    }

    /**
     * 测试 METHOD_PARAM 格式 key 缓存
     *  key=aop:method:cache:CustomCacheController#methodParam([10086])
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/method-param")
    @MethodCacheable(keyType = AspectKeyTypeEnum.METHOD_PARAM, useLocal = false)
    public ResponseResult<String> methodParam(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:method-param:{}", id);
        return ResponseResult.success("custom:method-param:" + id);
    }

    /**
     * 测试 METHOD_SPEL_PARAM 格式 key 缓存
     *  key=aop:method:cache:CustomCacheController#methodSpelParam(custom:method-spel-param:10086)
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/method-spel-param")
    @MethodCacheable(key = "custom:method-spel-param:${#id}",
            keyType = AspectKeyTypeEnum.METHOD_SPEL_PARAM, useLocal = true)
    public ResponseResult<String> methodSpelParam(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:method-spel-param:{}", id);
        return ResponseResult.success("custom:method-spel-param:" + id);
    }

    /**
     * 测试 SPEL 格式 key 缓存
     *  key=aop:method:cache:custom:spel:10086
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/spel")
    @MethodCacheable(key = "custom:spel:${#id}", keyType = AspectKeyTypeEnum.SPEL, useLocal = false)
    public ResponseResult<String> spel(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:spel:{}", id);
        return ResponseResult.success("custom:spel:" + id);
    }

    /**
     * 测试 METHOD_SPEL_PARAM 格式 key 缓存
     *  key=aop:method:cache:127.0.0.1
     *
     * @param id 传入id
     * @return 返回结果
     */
    @GetMapping("/ip")
    @MethodCacheable(keyType = AspectKeyTypeEnum.IP, useLocal = true)
    public ResponseResult<String> ip(Long id) {
        id += RandomUtil.randomLong();
        log.info("custom:ip:{}", id);
        return ResponseResult.success("custom:ip:" + id);
    }

}
