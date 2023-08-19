package cn.eastx.practice.demo.cache.controller;

import cn.eastx.practice.common.response.ResponseEnum;
import cn.eastx.practice.demo.cache.util.IpUtil;
import cn.eastx.practice.demo.cache.util.LocalCacheUtil;
import cn.eastx.practice.demo.cache.util.RedisUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 自定义缓存处理示例测试
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
@SpringBootTest
public class CustomCacheControllerTest {

    @Resource
    private CustomCacheController customCacheController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() throws Exception {
        log.info("执行初始化");
        mockMvc = MockMvcBuilders.standaloneSetup(customCacheController).build();
    }

    @Test
    public void test_method_cache() throws Exception {
        // DEFAULT
        check("aop:method:cache:hello-world", "/cache/custom/default");
        // METHOD
        check("aop:method:cache:CustomCacheController#method", "/cache/custom/method");
        // METHOD_PARAM
        check("aop:method:cache:CustomCacheController#methodParam([10086])", "/cache/custom/method-param");
        // METHOD_SPEL_PARAM
        check("aop:method:cache:CustomCacheController#methodSpelParam(test:method-spel-param:10086)", "/cache/custom/method-spel-param");
        // SPEL
        check("aop:method:cache:test:spel:10086", "/cache/custom/spel");
        // IP
        check("aop:method:cache:" + IpUtil.getIpAddr(), "/cache/custom/ip");
        // DEFAULT null
        check("aop:method:cache:hello-null", "/cache/custom/default/null");
    }

    @Test
    public void test_method_cache_all() throws Exception {
        String cacheKey = "aop:method:cache:hello-all";
        check(cacheKey, "/cache/custom/default/all");
        Long expire = RedisUtil.defTemplate().getExpire(cacheKey);
        log.info("expire={}", expire);
        Assert.isTrue(expire <= 300, "固定缓存时长超过设定值");
    }

    /**
     * 校验处理
     *
     * @param cacheKey 缓存key
     * @param urlTemplate 请求路由
     * @throws Exception 模拟请求可能抛出异常
     */
    private void check(String cacheKey, String urlTemplate) throws Exception {
        // 清除缓存
        RedisUtil.defTemplate().delete(cacheKey);
        LocalCacheUtil.delete(cacheKey);

        // 模拟请求
        RequestBuilder request = MockMvcRequestBuilders.get(urlTemplate)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", "10086");   //附加参数
        // 执行请求
        ResultActions result = mockMvc.perform(request);
        MvcResult mvcResult = result.andReturn();   // 返回
        // 分析结果
        result.andDo(MockMvcResultHandlers.print()); // 打印
        result.andExpect(MockMvcResultMatchers.status().isOk());    // 执行状态
        if (mvcResult.getResponse().getContentLength() > 0) {
            result.andExpect(MockMvcResultMatchers.jsonPath("code").value(ResponseEnum.SUCCESS.getCode())); // 期望值
        }

        String res1 = mvcResult.getResponse().getContentAsString();
        log.info("mvcResult1={}", res1);
        result = mockMvc.perform(request);
        String res2 = result.andReturn().getResponse().getContentAsString();
        log.info("mvcResult2={}", res2);
        Assert.isTrue(Objects.equals(res1, res2), "两次请求返回不一致");

        String redisCacheData = JSONUtil.toJsonStr(RedisUtil.opsValue().get(cacheKey));
        log.info("redisCacheData={}", redisCacheData);
        String localCacheData = JSONUtil.toJsonStr(LocalCacheUtil.get(cacheKey));
        log.info("localCacheData={}", localCacheData);
        if (ObjectUtil.isAllNotEmpty(redisCacheData, localCacheData)) {
            Assert.isTrue(Objects.equals(redisCacheData, localCacheData), "redis缓存与本地缓存数据不一致");
        } else {
            Assert.isNull(localCacheData, "本地缓存数据不为null");
        }
    }

}
