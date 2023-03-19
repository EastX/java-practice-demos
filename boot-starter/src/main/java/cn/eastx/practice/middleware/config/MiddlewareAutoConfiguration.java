package cn.eastx.practice.middleware.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置
 *
 * @author EastX
 * @date 2023/3/19
 */
@Configuration
@EnableConfigurationProperties({MiddlewareProperties.class})
public class MiddlewareAutoConfiguration {

}
