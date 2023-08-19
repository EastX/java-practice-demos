package cn.eastx.practice.demo.cache.util.mc;

import cn.eastx.practice.common.util.GeneralUtil;
import cn.eastx.practice.demo.cache.config.MemcacheProperties;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import com.spotify.folsom.MemcacheStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Folsom Memcached 工具类（推荐使用）
 * 注意：需要引入依赖 com.spotify.folsom
 *
 * <a href="https://colobu.com/2016/03/17/spymemcached-vs-xmemcached-vs-Folsom/"> memcached 客户端对比 </a>
 * <a href="https://github.com/spotify/folsom"> Folsom（github） </a>
 *
 * @author EastX
 * @date 2023/08/19
 */
public class FolsomMemcachedUtil {

    private static final Logger logger = LoggerFactory.getLogger(FolsomMemcachedUtil.class);

    private static volatile MemcacheClient<String> memcachedClient;

    static {
        // JVM关闭时的钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[FolsomMemcachedUtil]JVM退出钩子, 关闭客户端连接");
            if (memcachedClient != null) {
                try {
                    memcachedClient.shutdown();
                } catch (Exception ignore) {}
            }
        }));
    }

    private FolsomMemcachedUtil() {}

    /**
     * 获取 MemcachedClient
     */
    public static MemcacheClient<String> getMcClient() {
        if (memcachedClient == null) {
            synchronized (FolsomMemcachedUtil.class) {
                if (memcachedClient == null) {
                    MemcacheProperties.check();

                    try {
                        memcachedClient = MemcacheClientBuilder.newStringClient()
                                .withAddress(MemcacheProperties.getHost(), MemcacheProperties.getPort())
                                .connectBinary();
                        memcachedClient.awaitConnected(10, TimeUnit.SECONDS);
                    } catch (TimeoutException | InterruptedException e) {
                        logger.error("[FolsomMemcachedUtil]memcachedClient初始化异常", e);
                    }
                }
            }
        }

        return memcachedClient;
    }

    /**
     * 获取值
     *
     * @param key 缓存Key
     * @return 缓存值对象
     */
    public static String get(String key) {
        if (GeneralUtil.isEmpty(key)) {
            return null;
        }

        MemcacheClient<String> client = getMcClient();

        try {
            return client.get(key).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(GeneralUtil.formatMsg(
                    "[FolsomMemcachedUtil]获取值出现异常, key={}", key), e);
        }

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
    public static boolean set(String key, String value, int expire) {
        if (GeneralUtil.isEmpty(key)) {
            return false;
        }

        MemcacheClient<String> client = getMcClient();

        try {
            MemcacheStatus mcState = client.set(key, value, expire).toCompletableFuture().get();
            if (MemcacheStatus.OK.equals(mcState)) {
                return true;
            }

            logger.info("[FolsomMemcachedUtil]设置值失败, key={}, value={}, mcState={}",
                    key, value, mcState);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                    "[FolsomMemcachedUtil]设置值出现异常, key={}, val={}, exp={}",
                    key, value, expire),
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

        MemcacheClient<String> client = getMcClient();

        try {
            MemcacheStatus mcState = client.delete(key).toCompletableFuture().get();
            if (MemcacheStatus.OK.equals(mcState)) {
                return true;
            }

            logger.info("[FolsomMemcachedUtil]删除key失败, key={}, mcState={}", key, mcState);
        } catch (Exception e) {
            logger.error(GeneralUtil.formatMsg(
                    "[FolsomMemcachedUtil]设置值出现异常, key={}",
                            key),
                    e);
        }

        return false;
    }

}
