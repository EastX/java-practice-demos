<h1 align="center">
    <a href="https://github.com/EastX/java-practice-demos/tree/main/boot-starter">
        SpringBoot starter 实践示例
    </a>
</h1>

<p align="center">
    <a href="https://www.oracle.com/java/technologies/downloads/archive/">
        <img alt="JDK" src="https://img.shields.io/badge/JDK-1.8.0_201-e67621.svg"/>
    </a>
    <a href="https://docs.spring.io/spring-boot/docs/2.7.9/reference/html/">
        <img alt="Spring Boot" src="https://img.shields.io/badge/Spring Boot-2.7.9-6db33f.svg"/>
    </a>
    <a href="https://redis.io/">
        <img alt="Redis" src="https://img.shields.io/badge/Redis-6.0 lettuce-dc382c.svg"/>
    </a>
    <a href="https://github.com/ben-manes/caffeine">
        <img alt="Caffeine" src="https://img.shields.io/badge/Caffeine-2.9.3-6db33f.svg"/>
    </a>
</p>

## 1. 概述
> 将模块封装为 starter ，提供给其他 SpringBoot 项目使用

### 已封装模块
| 名称 | 介绍 |
| --- | --- |
| [demo-cache](../demo-cache) | 缓存实践<br>功能：缓存工具、方法缓存注解组件（基于 Spring AOP） |
| [demo-lock](../demo-lock)   | 锁实践<br>功能：自定义分布式锁工具 |

### 测试
[boot-starter-test](../boot-starter-test)

## 2. 封装处理
#### 2.1 ：pom 依赖
- 配置依赖 [`pom.xml`](./pom.xml)
    ```xml
    <packaging>jar</packaging>
  
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
    ```

#### 2.2 ：Properties
> 静态配置只需要处理 set 方法为普通方法即可，如：cn.eastx.practice.middleware.config.MiddlewareProperties.setSpelContexts()

- 自定义全局配置项 [`MiddlewareProperties.java`](./src/main/java/cn/eastx/practice/middleware/config/MiddlewareProperties.java)
<br> 管理 `practice.middleware` 配置项
    - spelContexts: SpEL 上下文内容自定义填充
      <br> 转换 SpEL 表达式处理 [`AspectUtil.java`](./src/main/java/cn/eastx/practice/middleware/util/AspectUtil.java)
- 自定义缓存相关配置项 [`CacheProperties.java`](./src/main/java/cn/eastx/practice/middleware/config/CacheProperties.java)
  <br> 管理 `practice.middleware.cache` 配置项
  - localCache: 本地缓存内容
    <br> 本地缓存工具处理 [`LocalCacheUtil.java`](./src/main/java/cn/eastx/practice/middleware/util/LocalCacheUtil.java)

#### 2.3 ：AutoConfiguration
> 静态配置只需要处理 set 方法为普通方法即可，如：cn.eastx.practice.middleware.config.MiddlewareProperties.setSpelContexts()

- 自定义全局自动配置 [`MiddlewareAutoConfiguration.java`](./src/main/java/cn/eastx/practice/middleware/config/MiddlewareAutoConfiguration.java)
  <br> 配合 `MiddlewareProperties.java` ，全局相关 Bean 放在此类配置
- 自定义缓存相关自动配置 [`CacheAutoConfiguration.java`](./src/main/java/cn/eastx/practice/middleware/config/CacheAutoConfiguration.java)
  <br> 配合 `CacheProperties.java` ，缓存相关 Bean 放在此类配置

#### 2.4 ：spring.factories
- 注入服务实现 [`spring.factories`](./src/main/resources/META-INF/spring.factories)

**参考**
- [自定义starter - pdai](https://pdai.tech/md/spring/springboot/springboot-y-starter.html#%E5%B0%81%E8%A3%85starter)

