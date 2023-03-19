package cn.eastx.practice.middleware.constant;

import cn.eastx.practice.middleware.util.AspectUtil;
import cn.eastx.practice.middleware.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

/**
 * AOP 切面 key 类型枚举
 *
 * @author EastX
 * @date 2022/10/20
 */
public enum AspectKeyTypeEnum {
    /**
     * 默认 前缀 + key
     * 示例：key=hello-world
     *          =>  "aop:method:cache:hello-world"
     */
    DEFAULT {
        @Override
        public String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data) {
            return data.getPrefix() + data.getKey();
        }
    },
    /**
     * 前缀 + 类名 + # + 方法名
     * 示例：method=obtainTypeKey
     *          =>  "aop:method:cache:AspectKeyTypeEnum#obtainTypeKey"
     */
    METHOD {
        @Override
        public String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data) {
            return data.getPrefix() + AspectUtil.getMethodKey(method, null);
        }
    },
    /**
     * 前缀 + 类名 + # + 方法名 + 默认参数
     * 示例：param={joinPoint=null,data=null}
     *          =>  "aop:method:cache:AspectKeyTypeEnum#obtainTypeKey(null,null)"
     */
    METHOD_PARAM {
        @Override
        public String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data) {
            String paramStr = JsonUtil.toJsonStr(methodParams).replace("\"", "");
            return data.getPrefix() + AspectUtil.getMethodKey(method, paramStr);
        }
    },
    /**
     * 前缀 + 类名 + # + 方法名 + SPELkey格式参数
     * 示例：spel="${#data.prefix}-${#data.key}"
     *          =>  data={prefix=hello,key=world}
     *          =>  "aop:method:cache:AspectKeyTypeEnum#obtainTypeKey(hello-world)"
     */
    METHOD_SPEL_PARAM {
        @Override
        public String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data) {
            String paramStr = AspectUtil.convertSpelValue(data.getKey(), method, methodParams,
                    String.class);
            return data.getPrefix() + AspectUtil.getMethodKey(method, paramStr);
        }
    },
    /**
     * 转换后的 spelKey
     * 示例：spel="${#data.prefix}-${#data.key}"
     *          =>  data={prefix=hello,key=world}
     *          =>  "aop:method:cache:hello-world"
     */
    SPEL {
        @Override
        public String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data) {
            return data.getPrefix() + AspectUtil.convertSpelValue(data.getKey(), method,
                    methodParams, String.class);
        }
    },
    ;

    /**
     * 获取 AOP 切面 key
     *
     * @param method 方法对象
     * @param methodParams 方法参数值数组
     * @param data 其他自定义数据
     * @return AOP 切面 key
     */
    @Nullable
    public abstract String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data);

    /**
     * AOP 切面 key 类型传入数据
     */
    public static class KeyTypeData {
        /**
         * 缓存前缀
         */
        private String prefix;
        /**
         * 传入 key，可能为 SpEL 格式
         */
        private String key;

        public KeyTypeData(String prefix, String key) {
            this.prefix = prefix;
            this.key = key;
        }

        public String getPrefix() {
            if (StringUtils.isBlank(this.prefix)) {
                return "";
            }

            return "aop:" + this.prefix.trim() + ":";
        }

        public String getKey() {
            return key;
        }
        
    }

}
