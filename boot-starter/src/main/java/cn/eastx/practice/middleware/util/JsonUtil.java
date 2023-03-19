package cn.eastx.practice.middleware.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON 工具类
 *
 * @author EastX
 * @date 2022/10/22
 */
public class JsonUtil {

    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

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

    /**
     * 转换对象为 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJsonStr(Object obj) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("[JSON]转换对象为JSON字符串出现异常, obj={}, e={}", obj, e);
        }

        return null;
    }

    /**
     * 转换 JSON 字符串为对象
     * @param jsonStr JSON 字符串
     * @param clazz 对象 Class
     * @param <T> 对象类型
     * @return 实体对象
     */
    public static <T> T toObject(String jsonStr, Class<T> clazz) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(jsonStr, clazz);
        } catch (JsonProcessingException e) {
            logger.error("[JSON]转换JSON字符串为对象出现异常, jsonStr={}, type={}, e={}",
                    jsonStr, clazz, e);
        }

        return null;
    }

}
