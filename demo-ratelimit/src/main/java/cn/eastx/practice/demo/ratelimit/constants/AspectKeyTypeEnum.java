package cn.eastx.practice.demo.ratelimit.constants;

import cn.eastx.practice.demo.ratelimit.util.AspectUtil;
import cn.eastx.practice.demo.ratelimit.util.IpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Builder;
import lombok.Data;
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
            String paramStr = JSONUtil.toJsonStr(methodParams).replace("\"", "");
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
    /**
     * ipKey
     * 示例：ip="127.0.0.1"
     *          =>  "aop:method:cache:127.0.0.1"
     */
    IP {
        @Override
        public String obtainTypeKey(Method method, Object[] methodParams, KeyTypeData data) {
            return data.getPrefix() + IpUtil.getIpAddr();
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
    @Data
    @Builder
    public static class KeyTypeData {
        /**
         * 缓存前缀
         */
        private String prefix;
        /**
         * 传入 key，可能为 SpEL 格式
         */
        private String key;

        public String getPrefix() {
            if (StrUtil.isBlank(this.prefix)) {
                return "";
            }

            return "aop:" + this.prefix.trim() + ":";
        }
    }

}
