package cn.eastx.practice.demo.crypto.config.mp;

import cn.eastx.practice.demo.crypto.util.CryptoDataUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 加密条件注解
 *  处理 SQL 查询条件
 *
 * @see CryptoCondInterceptor 加密条件 SQL 查询拦截器
 *
 * @author EastX
 * @date 2022/11/11
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CryptoCond {

    /**
     * 将当前字段替换为指定列名
     *  替换 SQL 条件中的字段名（WHERE/HAVING ...），不处理参数值（UPDATE SET ...）
     */
    String replacedColumn() default "";

    /**
     * 加密方式
     *  对 SQL 中的条件值、参数值进行加密处理
     */
    EncryptionEnum encryption() default EncryptionEnum.DEFAULT_OVERALL;

    /**
     * 加密方式枚举
     */
    @Getter
    @AllArgsConstructor
    enum EncryptionEnum {
        /** 整体加密 */
        DEFAULT_OVERALL {
            @Override
            public String encrypt(String data) {
                return CryptoDataUtil.overallEncrypt(data);
            }
        },
        /** 模糊加密 */
        DEFAULT_FUZZY {
            @Override
            public String encrypt(String data) {
                return CryptoDataUtil.fuzzyEncrypt(data);
            }
        },
        ;

        /**
         * 加密数据
         *
         * @param data 数据明文
         * @return 数据密文
         */
        public abstract String encrypt(String data);
    }

}
