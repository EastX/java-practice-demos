package cn.eastx.practice.demo.cache.controller;

import cn.eastx.practice.common.response.ResponseEnum;
import cn.eastx.practice.demo.cache.util.LocalCacheUtil;
import cn.eastx.practice.demo.cache.util.RedisUtil;
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
 * Spring Cache 整合 Redis 处理示例 测试类
 *
 * @author EastX
 * @date 2022/10/22
 */
@Slf4j
@SpringBootTest
public class ExpandSpringCacheControllerTest {

    @Resource
    private ExpandSpringCacheController expandCacheController;
    @Resource
    private RedisUtil redisUtil;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() throws Exception {
        log.info("执行初始化");
        mockMvc = MockMvcBuilders.standaloneSetup(expandCacheController).build();
    }

    @Test
    public void test_method_cache() throws Exception {
        check("spring:cache:prefix::hello-spel-1008611", "/cache/spring/spel", "1008611");
        check("spring:cache:prefix::ExpandSpringCacheController#spel2([1008612])", "/cache/spring/spel2", "1008612");
        check("spring:cache:prefix::hello-spel3-1008613", "/cache/spring/spel3", "1008613");

        check("spring:cache:world::ExpandSpringCacheController#ttl([1008614])", "/cache/spring/ttl", "1008614");
        check("spring:cache:yml-ttl::ExpandSpringCacheController#ttl2([1008615])", "/cache/spring/ttl2", "1008615");
        check("spring:cache:hello::ExpandSpringCacheController#ttl3([1008616])", "/cache/spring/ttl3", "1008616");
    }

    /**
     * 校验处理
     *
     * @param cacheKey 缓存key
     * @param urlTemplate 请求路由
     * @param param 参数值
     * @throws Exception 模拟请求可能抛出异常
     */
    private void check(String cacheKey, String urlTemplate, String param) throws Exception {
        // 清除缓存
        redisUtil.delete(cacheKey);
        LocalCacheUtil.invalidate(cacheKey);

        // 模拟请求
        RequestBuilder request = MockMvcRequestBuilders.get(urlTemplate)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", param);   //附加参数
        // 执行请求
        ResultActions result = mockMvc.perform(request);
        MvcResult mvcResult = result.andReturn();   // 返回
        // 分析结果
        result.andDo(MockMvcResultHandlers.print()); // 打印
        result.andExpect(MockMvcResultMatchers.status().isOk());    // 执行状态
        if (mvcResult.getResponse().getContentLength() > 0) {
            result.andExpect(MockMvcResultMatchers.jsonPath("code")
                    .value(ResponseEnum.SUCCESS.getCode())); // 期望值
        }

        String res1 = mvcResult.getResponse().getContentAsString();
        log.info("mvcResult1={}", res1);
        result = mockMvc.perform(request);
        String res2 = result.andReturn().getResponse().getContentAsString();
        log.info("mvcResult2={}", res2);
        Assert.isTrue(Objects.equals(res1, res2), "两次请求返回不一致");

        String redisCacheData = JSONUtil.toJsonStr(redisUtil.get(cacheKey));
        log.info("redisCacheData={}", redisCacheData);
    }

}
