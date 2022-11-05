package cn.eastx.practice.demo.ratelimit.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 工具类
 *
 * @author EastX
 * @date 2022/10/22
 */
@Slf4j
public class JsonUtil {

    private JsonUtil() {}

    /**
     * 创建 Jackson ObjectMapper 对象
     *  针对部分场景默认 Jackson 序列化处理不支持
     *
     * @return ObjectMapper 对象
     */
    public static ObjectMapper createJacksonObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(om.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 支持 LocalDateTime 序列化
        om.registerModule(new JavaTimeModule());
        om.registerModule((new SimpleModule()));
        return om;
    }

}
