package cn.eastx.practice.demo.cache.util.mc;

import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.demo.cache.config.MemcacheProperties;
import cn.hutool.core.util.ObjectUtil;
import net.spy.memcached.*;
import net.spy.memcached.transcoders.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * SpyMemcached 工具类
 * 注意：需要引入依赖 net.spy.memcached
 *
 * <a href="https://colobu.com/2016/03/17/spymemcached-vs-xmemcached-vs-Folsom/"> memcached 客户端对比 </a>
 *
 * @author EastX
 * @date 2023/08/19
 */
public class SpyMemcachedUtil {

    private static final Logger logger = LoggerFactory.getLogger(SpyMemcachedUtil.class);

    private static volatile MemcachedClient memcachedClient;

    static {
        // JVM关闭时的钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SpyMemcachedUtil]JVM退出钩子, 关闭客户端连接");
            if (memcachedClient != null) {
                try {
                    memcachedClient.shutdown();
                } catch (Exception ignore) {}
            }
        }));
    }

    private SpyMemcachedUtil() {}

    /**
     * 获取 MemcachedClient
     */
    public static MemcachedClient getMcClient() {
        if (memcachedClient == null) {
            synchronized (SpyMemcachedUtil.class) {
                if (memcachedClient == null) {
                    MemcacheProperties.check();

                    try {
                        ConnectionFactory connectionFactory = new ConnectionFactoryBuilder()
                                // 以毫秒为单位设置默认的操作超时时间
                                .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                                // 以毫秒为单位设置默认的操作超时时间
                                .setOpTimeout(3000)
                                // 设置最大超时异常阈值
                                .setTimeoutExceptionThreshold(5998)
                                // 设置哈希算法
                                .setHashAlg(DefaultHashAlgorithm.KETAMA_HASH)
                                // 设置定位器类型(ARRAY_MOD,CONSISTENT),默认是ARRAY_MOD
                                .setLocatorType(ConnectionFactoryBuilder.Locator.CONSISTENT)
                                // 设置故障模式(取消，重新分配，重试)，默认是重新分配
                                .setFailureMode(FailureMode.Redistribute)
                                // 不使用Nagle算法
                                .setUseNagleAlgorithm(false)
                                .build();
                        String mcServer = MemcacheProperties.getHost()
                                + ":" + MemcacheProperties.getPort();
                        memcachedClient = new MemcachedClient(connectionFactory, AddrUtil.getAddresses(mcServer));
                    } catch (IOException e) {
                        logger.error("[SpyMemcachedUtil]memcachedClient初始化异常", e);
                    }
                }
            }
        }

        return memcachedClient;
    }

    /**
     * 获取值
     *
     * @param key        缓存Key
     * @param transcoder 转码器
     * @return 缓存值对象
     */
    public static Object get(String key, Transcoder transcoder) {
        if (GeneralUtil.isEmpty(key)) {
            return null;
        }

        MemcachedClient client = getMcClient();

        try {
            transcoder = ObjectUtil.defaultIfNull(transcoder, client.getTranscoder());
            return client.get(key, transcoder);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                    "[SpyMemcachedUtil]获取值出现异常, key={}", key), e);
        }

        return null;
    }

    /**
     * 设置值
     *
     * @param key        缓存Key
     * @param value      缓存值
     * @param expire     过期时间，单位秒，不超过30天（60*60*24*30），超过将被认为是unix时间值
     * @param transcoder 转码器
     * @return 是否设值成功
     */
    public static boolean set(String key, Object value, int expire, Transcoder transcoder) {
        if (GeneralUtil.isEmpty(key)) {
            return false;
        }

        MemcachedClient client = getMcClient();

        try {
            transcoder = ObjectUtil.defaultIfNull(transcoder, client.getTranscoder());
            Future<Boolean> f = client.set(key, expire, value, transcoder);
            return getBooleanValue(f);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                            "[SpyMemcachedUtil]设置值出现异常, key={}, val={}, exp={}, tc={}",
                            key, value, expire, transcoder),
                    e);
        }

        return false;
    }

    /**
     * 删除key
     *
     * @param key 缓存Key
     * @return 是否删除成功
     */
    public static boolean delete(String key) {
        if (GeneralUtil.isEmpty(key)) {
            return false;
        }

        MemcachedClient client = getMcClient();

        try {
            Future<Boolean> f = client.delete(key);
            return getBooleanValue(f);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                            "[SpyMemcachedUtil]设置值出现异常, key={}",
                            key),
                    e);
        }

        return false;
    }

    private static boolean getBooleanValue(Future<Boolean> f) {
        try {
            Boolean bool = f.get(5, TimeUnit.SECONDS);
            return bool.booleanValue();
        } catch (Exception e) {
            f.cancel(false);
            return false;
        }
    }

    /**
     * 转码器枚举
     */
    public enum TranscoderEnum {
        SERIALIZING(new SerializingTranscoder()),
        WHALIN(new WhalinTranscoder()),
        WHALIN_V1(new WhalinV1Transcoder()),

        INTEGER(new IntegerTranscoder()),
        LONG(new LongTranscoder()),
        ;

        private Transcoder transcoder;

        TranscoderEnum(Transcoder transcoder) {
            this.transcoder = transcoder;
        }

        public Transcoder getTranscoder() {
            return transcoder;
        }

        /**
         * 获取值
         *
         * @param key 缓存Key
         * @return 缓存值对象
         */
        public Object get(String key) {
            return SpyMemcachedUtil.get(key, this.getTranscoder());
        }

        /**
         * 设置值
         *
         * @param key    缓存Key
         * @param value  缓存值
         * @param expire 过期时间，单位秒，不超过30天（60*60*24*30），超过将被认为是unix时间值
         * @return 是否设值成功
         */
        public boolean set(String key, Object value, int expire) {
            return SpyMemcachedUtil.set(key, value, expire, this.getTranscoder());
        }

    }

}
