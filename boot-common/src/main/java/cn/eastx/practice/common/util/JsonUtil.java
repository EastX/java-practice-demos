package cn.eastx.practice.common.util;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * JSON工具类
 *
 * @author EastX
 * @date 2022/10/22
 */
public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private JsonUtil() {}

    private static ObjectMapperFacade defaultOmFacade;
    private static ObjectMapperFacade springOmFacade;

    static {
        ObjectMapper defaultOm = new ObjectMapper();
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
        defaultOm.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        defaultOm.activateDefaultTyping(defaultOm.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        defaultOm.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 支持 LocalDateTime 序列化
        defaultOm.registerModule(new JavaTimeModule());
        defaultOm.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        defaultOm.registerModule((new SimpleModule()));
        defaultOmFacade = new ObjectMapperFacade(defaultOm);
    }

    /**
     * 默认自定义 ObjectMapper 外观对象
     */
    public static ObjectMapperFacade defFacade() {
        return defaultOmFacade;
    }

    /**
     * 获取 Spring ObjectMapper Bean 外观对象
     *  注意需要在 初始化SpringObjectMapper 之后使用，否则为空
     * @see JsonUtil#initSpringOm(com.fasterxml.jackson.databind.ObjectMapper)
     */
    public static ObjectMapperFacade springFacade() {
        return springOmFacade;
    }

    /**
     * 初始化 objectMapper 属性
     * <p>
     * 通过这样的方式，使用 Spring 创建的 ObjectMapper Bean
     *
     * @param om ObjectMapper 对象
     */
    public static void initSpringOm(ObjectMapper om) {
        JsonUtil.springOmFacade = new ObjectMapperFacade(om);
    }

    /**
     * 校验字符串是否为 JSON 格式
     *
     * @param text 字符串
     * @return 是否为 JSON 格式字符串
     */
    public static boolean isJson(String text) {
        return JSONUtil.isTypeJSON(text);
    }

    /**
     * 对象转换为 JSON 字符串（简单格式）
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toSimpleStr(Object obj) {
        if (obj == null) {
            return null;
        }

        // JSONUtil.toJsonStr 会将 基本类型及包装类转换为空对象，String 转换为自身，此处直接调用 toString
        Class clazz = obj.getClass();
        if (ClassUtils.isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            return String.valueOf(obj);
        }

        return JSONUtil.toJsonStr(obj, JSONConfig.create().setIgnoreNullValue(false).setStripTrailingZeros(false));
    }

    /**
     * ObjectMapper 门面封装类，统一方法
     */
    public static class ObjectMapperFacade {

        private final ObjectMapper objectMapper;

        public ObjectMapperFacade(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        /**
         * 对象转换为 JSON 字节数组
         *
         * @param obj 对象
         * @return JSON 字节数组
         */
        public byte[] toJsonByte(Object obj) {
            try {
                return objectMapper.writeValueAsBytes(obj);
            } catch (Exception e) {
                logger.error(GeneralUtil.formatMsg("[JsonUtil]toJsonByte error, obj={}", obj), e);
            }

            return null;
        }

        /**
         * 对象转换为 JSON 字符串
         *
         * @param obj 对象
         * @return JSON 字符串
         */
        public String toJsonStr(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (Exception e) {
                logger.error(GeneralUtil.formatMsg("[JsonUtil]toJsonStr error, obj={}", obj), e);
            }

            return null;
        }

        /**
         * 对象转换为 JSON 字符串（格式美化）
         *
         * @param obj 对象
         * @return JSON 字符串
         */
        public String toJsonPrettyStr(Object obj) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } catch (Exception e) {
                logger.error(GeneralUtil.formatMsg("[JsonUtil]toJsonPrettyStr error, obj={}", obj), e);
            }

            return null;
        }

        /**
         * 字符串解析成指定类型对象
         *
         * @param jsonStr JSON字符串
         * @param clazz 指定类型 Class 对象，注意如果是数组不要传入 Object.class ，会导致转换失败
         * @return 指定类型对象，字符串非 JSON 格式/解析失败 返回null
         */
        public <T> T parseObject(String jsonStr, Class<T> clazz) {
            if (GeneralUtil.isEmpty(jsonStr)) {
                return null;
            }

            try {
                return objectMapper.readValue(jsonStr, clazz);
            } catch (Exception e) {
                logger.error(GeneralUtil.formatMsg("[JsonUtil]parseObject error, jsonStr={}", jsonStr), e);
            }

            return null;
        }

        /**
         * 字符串解析成指定类型对象（支持泛型）
         *
         * @param jsonStr JSON字符串
         * @param typeReference 类型引用
         * @param <T> 类型
         * @return 指定类型对象，字符串非 JSON 格式/解析失败 返回null
         */
        public <T> T parseObject(String jsonStr, TypeReference<T> typeReference) {
            if (!isJson(jsonStr)) {
                return null;
            }

            try {
                return objectMapper.readValue(jsonStr, typeReference);
            } catch (IOException e) {
                logger.error(GeneralUtil.formatMsg("[JsonUtil]parseObject error, jsonStr={}", jsonStr), e);
            }

            return null;
        }

        /**
         * JSON 字符串解析成指定类型对象集合
         *
         * @param jsonStr JSON字符串
         * @param clazz 指定类型 Class 对象
         * @param <T> 类型
         * @return 指定类型对象集合（可变集合），字符串非 JSON 格式/解析失败 返回空集合
         */
        public <T> List<T> parseArray(String jsonStr, Class<T> clazz) {
            if (!isJson(jsonStr)) {
                return new ArrayList<>();
            }

            try {
                return objectMapper.readValue(jsonStr,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
            } catch (IOException e) {
                logger.error(GeneralUtil.formatMsg("[JsonUtil]parseArray error, jsonStr={}", jsonStr), e);
            }

            return new ArrayList<>();
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }
    }

}
