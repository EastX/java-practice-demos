package cn.eastx.practice.middleware.config;

import cn.eastx.practice.middleware.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * 全局配置
 *
 * @author EastX
 * @date 2023/3/19
 */
@ConfigurationProperties(prefix = "practice.middleware")
public class MiddlewareProperties {

    private Logger logger = LoggerFactory.getLogger(MiddlewareProperties.class);

    /**
     * SpEL 上下文
     * @see cn.eastx.practice.middleware.util.AspectUtil
     */
    private static List<SpelContext> spelContexts = Collections.emptyList();

    public static List<SpelContext> getSpelContexts() {
        return spelContexts;
    }

    public void setSpelContexts(List<SpelContext> spelContexts) {
        MiddlewareProperties.spelContexts = spelContexts;
        logger.debug("[middlewareProperties]spelContexts={}", spelContexts);
    }

    /**
     * SpEL 上下文填充
     */
    public static class SpelContext {
        /**
         * SpEL 上下文名称
         */
        private String name;
        /**
         * SpEL 上下文值对应的反射类
         */
        private Class<?> valueClass;
        /**
         * SpEL 上下文值对应的反射方法名（无参静态方法）
         * 示例：cn.eastx.practice.middleware.util.JsonUtil#createJacksonObjectMapper()
         */
        private String valueMethod;

        /**
         * 反射执行上下文值对应的方法
         *
         * @return 上下文值
         */
        public Object invokeValue() {
            return ClassUtil.invokeStatic(this.getValueClass(), this.getValueMethod());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class<?> getValueClass() {
            return valueClass;
        }

        public void setValueClass(Class<?> valueClass) {
            this.valueClass = valueClass;
        }

        public String getValueMethod() {
            return valueMethod;
        }

        public void setValueMethod(String valueMethod) {
            this.valueMethod = valueMethod;
        }

        @Override
        public String toString() {
            return "SpelContext{" +
                    "name='" + name + '\'' +
                    ", valueClass=" + valueClass +
                    ", valueMethod='" + valueMethod + '\'' +
                    '}';
        }
    }

}
