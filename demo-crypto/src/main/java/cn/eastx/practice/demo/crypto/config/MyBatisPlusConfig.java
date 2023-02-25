package cn.eastx.practice.demo.crypto.config;

import cn.eastx.practice.demo.crypto.config.mp.CommonFieldHandler;
import cn.eastx.practice.demo.crypto.config.mp.CryptoCondInterceptor;
import cn.eastx.practice.demo.crypto.config.mp.DefaultStringTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author EastX
 * @date 2022/11/11
 */
@Configuration
@MapperScan("cn.eastx.practice.demo.crypto.dao")
public class MyBatisPlusConfig {

    /**
     * 公共字段处理器
     */
    @Bean
    public CommonFieldHandler commonFieldHandler() {
        return new CommonFieldHandler();
    }

    /**
     * 加密条件拦截器
     */
    @Bean
    public CryptoCondInterceptor cryptoCondInterceptor() {
        return new CryptoCondInterceptor();
    }

    /**
     * 自定义默认字符串类型处理器
     */
    @Bean
    public DefaultStringTypeHandler defaultStringTypeHandler() {
        return new DefaultStringTypeHandler();
    }

}
