package cn.eastx.practice.demo.cache.util.mc;

import cn.eastx.practice.common.util.GeneralUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * 通用 Memcached 工具类
 * 判断依赖使用相应工具类
 * <a href="https://colobu.com/2016/03/17/spymemcached-vs-xmemcached-vs-Folsom/"> memcached 客户端对比 </a>
 *
 * @see FolsomMemcachedUtil 推荐
 * @see XMemcachedUtil
 * @see SpyMemcachedUtil
 *
 * @author EastX
 * @date 2023/08/19
 */
public class GeneralMcUtil {

    private static final Logger logger = LoggerFactory.getLogger(GeneralMcUtil.class);

    /**
     * Memcached 工具类类型枚举
     */
    private static final TypeEnum MC_TYPE;

    static {
        TypeEnum[] typeEnums = new TypeEnum[]{TypeEnum.FOLSOM, TypeEnum.XMC, TypeEnum.SPY};
        MC_TYPE = Stream.of(typeEnums).filter(TypeEnum::canUse).findFirst().orElse(TypeEnum.NON);
        logger.info("[GeneralMcUtil]初始化工具类类型枚举，MC_TYPE={}", MC_TYPE);
    }

    private GeneralMcUtil() {}

    /**
     * 获取值
     *
     * @param key 缓存Key
     * @return 缓存值对象
     */
    public static String get(String key) {
        return MC_TYPE.get(key);
    }

    /**
     * 设置值
     *
     * @param key    缓存Key
     * @param value  缓存值
     * @param expire 过期时间，单位秒，不超过30天（60*60*24*30），超过将被认为是unix时间值
     * @return 是否设值成功
     */
    public static boolean set(String key, String value, int expire) {
        return MC_TYPE.set(key, value, expire);
    }

    /**
     * 删除key
     *
     * @param key 缓存Key
     * @return 是否删除成功
     */
    public static boolean delete(String key) {
        return MC_TYPE.delete(key);
    }

    enum TypeEnum {
        /** 未配置 Memcache 依赖，不支持使用 Memcache */
        NON(null),
        FOLSOM ("com.spotify.folsom.MemcacheClient") {
            @Override
            public String get(String key) {
                return FolsomMemcachedUtil.get(key);
            }

            @Override
            public boolean set(String key, String value, int expire) {
                return FolsomMemcachedUtil.set(key, value, expire);
            }

            @Override
            public boolean delete(String key) {
                return FolsomMemcachedUtil.delete(key);
            }
        },
        XMC ("net.rubyeye.xmemcached.MemcachedClient") {
            @Override
            public String get(String key) {
                return StrUtil.toStringOrNull(XMemcachedUtil.TranscoderEnum.STRING.get(key));
            }

            @Override
            public boolean set(String key, String value, int expire) {
                return XMemcachedUtil.TranscoderEnum.STRING.set(key, value, expire);
            }

            @Override
            public boolean delete(String key) {
                return XMemcachedUtil.delete(key);
            }
        },
        SPY ("net.spy.memcached.MemcachedClient") {
            @Override
            public String get(String key) {
                return StrUtil.toStringOrNull(SpyMemcachedUtil.TranscoderEnum.SERIALIZING.get(key));
            }

            @Override
            public boolean set(String key, String value, int expire) {
                return SpyMemcachedUtil.TranscoderEnum.SERIALIZING.set(key, value, expire);
            }

            @Override
            public boolean delete(String key) {
                return SpyMemcachedUtil.delete(key);
            }
        },
        ;

        private String clientClassName;

        TypeEnum(String clientClassName) {
            this.clientClassName = clientClassName;
        }

        /**
         * 判断是否能使用当前枚举
         */
        public boolean canUse() {
            if (GeneralUtil.isEmpty(this.clientClassName)) {
                logger.error("[GeneralMcUtil]未配置clientClassName, MC_TYPE={}", this);
                return false;
            }

            try {
                Class mcClient = Class.forName(this.clientClassName);
                return true;
            } catch (ClassNotFoundException ignore) {}

            return false;
        }

        /**
         * 获取值
         *
         * @param key 缓存Key
         * @return 缓存值对象
         */
        public String get(String key) {
            logger.error("[GeneralMcUtil]不支持获取值, MC_TYPE={}, key={}", this, key);
            return null;
        }

        /**
         * 设置值
         *
         * @param key    缓存Key
         * @param value  缓存值
         * @param expire 过期时间，单位秒，不超过30天（60*60*24*30），超过将被认为是unix时间值
         * @return 是否设值成功
         */
        public boolean set(String key, String value, int expire) {
            logger.error("[GeneralMcUtil]不支持设置值, MC_TYPE={}, key={}, value={}, exp={}",
                    this, key, value, expire);
            return false;
        }

        /**
         * 删除key
         *
         * @param key 缓存Key
         * @return 是否删除成功
         */
        public boolean delete(String key) {
            logger.error("[GeneralMcUtil]不支持删除key, MC_TYPE={}, key={}", this, key);
            return false;
        }

        @Override
        public String toString() {
            String str = this.name();
            if (this == NON) {
                str += "--(未引入mc依赖)";
            }

            return str;
        }
    }

}
