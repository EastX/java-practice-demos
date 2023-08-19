package cn.eastx.practice.demo.cache.util.mc;

import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.demo.cache.config.MemcacheProperties;
import cn.hutool.core.util.ObjectUtil;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.*;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * XMemcached 工具类
 * 注意：需要引入依赖 com.googlecode.xmemcached.xmemcached
 *
 * <a href="https://colobu.com/2016/03/17/spymemcached-vs-xmemcached-vs-Folsom/"> memcached 客户端对比 </a>
 * <a href="https://github.com/killme2008/xmemcached/wiki/Xmemcached-%E4%B8%AD%E6%96%87%E7%94%A8%E6%88%B7%E6%8C%87%E5%8D%97"> XMemcached 用户指南 </a>
 *
 * @author EastX
 * @date 2023/08/19
 */
public class XMemcachedUtil {

    private static final Logger logger = LoggerFactory.getLogger(XMemcachedUtil.class);

    private static volatile MemcachedClient memcachedClient;

    static {
        // JVM关闭时的钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[XMemcachedUtil]JVM退出钩子, 关闭客户端连接");
            if (memcachedClient != null) {
                try {
                    memcachedClient.shutdown();
                } catch (Exception ignore) {}
            }
        }));
    }

    private XMemcachedUtil() {}

    /**
     * 获取 MemcachedClient
     */
    public static MemcachedClient getMcClient() {
        if (memcachedClient == null) {
            synchronized (XMemcachedUtil.class) {
                if (memcachedClient == null) {
                    MemcacheProperties.check();

                    try {
                        String mcServer = MemcacheProperties.getHost()
                                + ":" + MemcacheProperties.getPort();
                        MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(mcServer));
                        /*
                         * 下面这样是配置主从
                         * 其中localhost:11211是主1，localhost:11212是他的从
                         * host2:11211是主2，host2:11212是他的从
                         *
                         * 注意：使用主从配置的前提是builder.setFailureMode(true)
                         */
                        // MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddressMap("127.0.0.1:11211,127.0.0.1:11212"));
                        // 这个默认也是1
                        // builder.setConnectionPoolSize(1);
                        // 使用一致性hash算法
                        builder.setSessionLocator(new KetamaMemcachedSessionLocator(true));
                        // 命令工厂
                        builder.setCommandFactory(new BinaryCommandFactory());
                        // 设置failure模式
                        // builder.setFailureMode(true);
                        // 操作超时时间，默认5s
                        builder.setOpTimeout(3000);
                        memcachedClient = builder.build();
                    } catch (IOException e) {
                        logger.error("[XMemcachedUtil]memcachedClient初始化异常", e);
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
                    "[XMemcachedUtil]获取值出现异常, key={}", key), e);
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
            return client.set(key, expire, value, transcoder);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                            "[XMemcachedUtil]设置值出现异常, key={}, val={}, exp={}, tc={}",
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
            return client.delete(key);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                            "[XMemcachedUtil]设置值出现异常, key={}",
                            key),
                    e);
        }

        return false;
    }

    /**
     * 转码器枚举
     */
    public enum TranscoderEnum {
        SERIALIZING(new SerializingTranscoder()),
        WHALIN(new WhalinTranscoder()),
        WHALIN_V1(new WhalinV1Transcoder()),
        TOKYO_TYRANT(new TokyoTyrantTranscoder()),

        INTEGER(new IntegerTranscoder()),
        LONG(new LongTranscoder()),
        STRING(new StringTranscoder()),
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
            return XMemcachedUtil.get(key, this.getTranscoder());
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
            return XMemcachedUtil.set(key, value, expire, this.getTranscoder());
        }

    }

}
