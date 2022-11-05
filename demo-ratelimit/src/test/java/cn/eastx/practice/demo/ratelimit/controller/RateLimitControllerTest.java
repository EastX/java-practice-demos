package cn.eastx.practice.demo.ratelimit.controller;

import cn.eastx.practice.demo.ratelimit.constants.ResponseEnum;
import cn.eastx.practice.demo.ratelimit.pojo.ResponseResult;
import cn.eastx.practice.demo.ratelimit.util.RedisUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StopWatch;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 限流处理示例 测试
 *
 * @author EastX
 * @date 2022/11/5
 */
@Slf4j
@SpringBootTest
public class RateLimitControllerTest {

    @Resource
    private RateLimitController rateLimitController;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() throws Exception {
        log.info("执行初始化");
        // 通过参数指定一组控制器，这样就不需要从上下文获取了，不过不会走拦截器
//        mockMvc = MockMvcBuilders.standaloneSetup(rateLimitController).build();
        // 指定WebApplicationContext，将会从该上下文获取相应的控制器并得到相应的MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    public void test_classAnnotation() throws Exception {
        redisUtil.delete("aop:rate:limit:RateLimitController#fixedWindow");
        RequestBuilder request = MockMvcRequestBuilders.get("/ratelimit/custom/class/ann")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", "10086");   //附加参数

        StopWatch stopWatch = new StopWatch();
        // 最先进入的10个未被限流，10s后过期了再放10个不限流
        for (long i = 1; i <= 300; i++) {
            String res = mockMvc.perform(request).andReturn().getResponse()
                    .getContentAsString(CharsetUtil.CHARSET_UTF_8);
            ResponseResult result = JSONUtil.toBean(res, ResponseResult.class);
            boolean limited = Objects.equals(result.getCode(), ResponseEnum.RATE_LIMITED.getCode());

            log.debug("rateLimit, i={}, limited={}", i, limited);
            if (limited && !stopWatch.isRunning()) {
                stopWatch.start();
            } else if (!limited && stopWatch.isRunning()) {
                stopWatch.stop();
                double ms = stopWatch.getLastTaskTimeMillis();
                log.info("end limited second={}", ms / 1000);
            }

            TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(100, 300));
        }
    }

    @Test
    public void test_fixedWindow() throws Exception {
        redisUtil.delete("aop:rate:limit:RateLimitController#fixedWindow");
        RequestBuilder request = MockMvcRequestBuilders.get("/ratelimit/custom/fixed/window")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", "10086");   //附加参数

        StopWatch stopWatch = new StopWatch();
        // 最先进入的10个未被限流，10s后过期了再放10个不限流
        for (long i = 1; i <= 300; i++) {
            String res = mockMvc.perform(request).andReturn().getResponse()
                    .getContentAsString(CharsetUtil.CHARSET_UTF_8);
            ResponseResult result = JSONUtil.toBean(res, ResponseResult.class);
            boolean limited = Objects.equals(result.getCode(), ResponseEnum.RATE_LIMITED.getCode());

            log.debug("rateLimit, i={}, limited={}", i, limited);
            if (limited && !stopWatch.isRunning()) {
                stopWatch.start();
            } else if (!limited && stopWatch.isRunning()) {
                stopWatch.stop();
                double ms = stopWatch.getLastTaskTimeMillis();
                log.info("end limited second={}", ms / 1000);
            }

            TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(100, 300));
        }
    }

    @Test
    public void test_slidingWindow() throws Exception {
        redisUtil.delete("aop:rate:limit:RateLimitController#slidingWindow");
        RequestBuilder request = MockMvcRequestBuilders.get("/ratelimit/custom/sliding/window")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", "10086");   //附加参数

        StopWatch stopWatch = new StopWatch();
        // 最先进入的10个未被限流，后续每次计算时间差（当前时间-限流时间），去除之前请求重新放入请求
        for (long i = 1; i <= 300; i++) {
            String res = mockMvc.perform(request).andReturn().getResponse()
                    .getContentAsString(CharsetUtil.CHARSET_UTF_8);
            ResponseResult result = JSONUtil.toBean(res, ResponseResult.class);
            boolean limited = Objects.equals(result.getCode(), ResponseEnum.RATE_LIMITED.getCode());

            log.debug("rateLimit, i={}, limited={}", i, limited);
            if (limited && !stopWatch.isRunning()) {
                stopWatch.start();
            } else if (!limited && stopWatch.isRunning()) {
                stopWatch.stop();
                double ms = stopWatch.getLastTaskTimeMillis();
                log.info("end limited second={}", ms / 1000);
            }

            TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(100, 300));
        }
    }

    @Test
    public void test_leakyBucket() throws Exception {
        redisUtil.delete("aop:rate:limit:RateLimitController#leakyBucket");
        RequestBuilder request = MockMvcRequestBuilders.get("/ratelimit/custom/leaky/bucket")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", "10086");   //附加参数

        /*
            最先进入的10个未被限流（默认水量为0，达到阈值限流）
            后续按照速率流出水量（(当前时间-上次开始加水时间)*(阈值/限流时长)）
            流完后重新开始加水设置开始加水时间
         */
        StopWatch stopWatch = new StopWatch();
        for (long i = 1; i <= 300; i++) {
            String res = mockMvc.perform(request).andReturn().getResponse()
                    .getContentAsString(CharsetUtil.CHARSET_UTF_8);
            ResponseResult result = JSONUtil.toBean(res, ResponseResult.class);
            boolean limited = Objects.equals(result.getCode(), ResponseEnum.RATE_LIMITED.getCode());

            log.debug("rateLimit, i={}, limited={}", i, limited);
            if (limited && !stopWatch.isRunning()) {
                stopWatch.start();
            } else if (!limited && stopWatch.isRunning()) {
                stopWatch.stop();
                double ms = stopWatch.getLastTaskTimeMillis();
                log.info("end limited second={}", ms / 1000);
            }

            TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(10, 30));
        }
    }

    @Test
    public void test_tokenBucket() throws Exception {
        redisUtil.delete("aop:rate:limit:RateLimitController#tokenBucket");
        RequestBuilder request = MockMvcRequestBuilders.get("/ratelimit/custom/token/bucket")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)    //发送所用的数据格式
                .accept(MediaType.APPLICATION_JSON) //接收所使用的数据格式
                .param("id", "10086");   //附加参数

        /*
            最先进入的10个未被限流（默认令牌为最大值，进入一个去掉一个令牌）
            后续按照速率生成令牌（(当前时间-上次生成时间)*(阈值/限流时长)）
            生成了令牌就设置上次生成时间
         */
        StopWatch stopWatch = new StopWatch();
        for (long i = 1; i <= 300; i++) {
            String res = mockMvc.perform(request).andReturn().getResponse()
                    .getContentAsString(CharsetUtil.CHARSET_UTF_8);
            ResponseResult result = JSONUtil.toBean(res, ResponseResult.class);
            boolean limited = Objects.equals(result.getCode(), ResponseEnum.RATE_LIMITED.getCode());

            log.debug("rateLimit, i={}, limited={}", i, limited);
            if (limited && !stopWatch.isRunning()) {
                stopWatch.start();
            } else if (!limited && stopWatch.isRunning()) {
                stopWatch.stop();
                double ms = stopWatch.getLastTaskTimeMillis();
                log.info("end limited second={}", ms / 1000);
            }

            TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(10, 30));
        }
    }

}
