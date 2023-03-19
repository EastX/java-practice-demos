<h1 align="center">
    <a href="https://github.com/EastX/java-practice-demos/tree/main/boot-starter">
        SpringBoot starter 测试示例
    </a>
</h1>

<p align="center">
    <a href="https://docs.spring.io/spring-boot/docs/2.7.9/reference/html/">
        <img alt="Spring Boot" src="https://img.shields.io/badge/Spring Boot-2.7.9-6db33f.svg"/>
    </a>
    <a href="../boot-starter">
        <img alt="boot-starter" src="https://img.shields.io/badge/boot_starter-1.0-e67621.svg"/>
    </a>
</p>


## 1. 配置处理
#### 1.1 ：pom 依赖
- 引入依赖 [`pom.xml`](./pom.xml)
    ```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>cn.eastx.practice</groupId>
            <artifactId>boot-starter</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
    ```

#### 1.2 ：Properties
- 配置 [`application.yml`](./src/main/resources/application.yml)
  ```yml
  practice:
    middleware:
      spelContexts:
        - name: _RAND
          value-class: org.apache.commons.lang3.RandomUtils
          value-method: nextLong
      cache:
        local-cache:
          maximum-size: 1000
          expire-after-write: 10
  ```

**参考**
- [自定义starter - pdai](https://pdai.tech/md/spring/springboot/springboot-y-starter.html#%E5%B0%81%E8%A3%85starter)

## 2. 测试
- 缓存相关测试 [`CacheTest.java`](./src/test/java/cn/eastx/practice/middleware/test/cache/CacheTest.java)
- 锁相关测试 [`LockTest.java`](./src/test/java/cn/eastx/practice/middleware/test/cache/LockTest.java)


