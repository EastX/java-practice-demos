package cn.eastx.practice.common.util;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取当前线程存储的数据，支持父子线程传递
 *
 * @author ruoyi
 */
public class SecureContextHolder {

    public static final String TRACE_ID = "secure_trace_id";

    private static final TransmittableThreadLocal<Map<String, Object>> THREAD_LOCAL = new TransmittableThreadLocal<>();

    public static String get(String key) {
        Map<String, Object> map = getLocalMap();
        return Convert.toStr(map.getOrDefault(key, StringUtils.EMPTY));
    }

    public static <T> T get(String key, Class<T> clazz) {
        Map<String, Object> map = getLocalMap();
        return (T) map.getOrDefault(key, null);
    }

    public static void set(String key, Object value) {
        Map<String, Object> map = getLocalMap();
        if (value == null) {
            // 设值为null则删除相应key
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    public static Map<String, Object> getLocalMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            THREAD_LOCAL.set(map);
        }

        return map;
    }

    public static void setLocalMap(Map<String, Object> threadLocalMap) {
        THREAD_LOCAL.set(threadLocalMap);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    /**
     * 获取跟踪ID
     */
    public static String getTraceId() {
        String traceId = get(TRACE_ID);
        if (GeneralUtil.isEmpty(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            setTraceId(traceId);
        }

        return traceId;
    }

    /**
     * 设置跟踪ID
     */
    public static void setTraceId(String traceId) {
        set(TRACE_ID, traceId);
    }
}
