package cn.eastx.practice.demo.ratelimit.config;

import cn.eastx.practice.demo.ratelimit.config.custom.RateLimitInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author EastX
 * @date 2022/11/5
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor());
    }

    /**
     * 限流处理拦截器 Bean 对象
     *
     * @return 限流处理拦截器 Bean 对象
     */
    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

}
