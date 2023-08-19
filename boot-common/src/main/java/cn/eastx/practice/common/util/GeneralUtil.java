package cn.eastx.practice.common.util;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 通用工具类
 *
 * @author EastX
 * @date 2023/08/19
 */
public class GeneralUtil {

    private static final Logger logger = LoggerFactory.getLogger(GeneralUtil.class);

    private GeneralUtil() {}

    /**
     * 是否存在{@code null}或空对象
     *
     * @param args 被检查对象
     * @return 是否存在
     */
    public static boolean hasEmpty(Object... args) {
        if (ArrayUtil.isNotEmpty(args)) {
            for (Object element : args) {
                if (isEmpty(element)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 是否全部为{@code null}或空对象或小于0
     * 通过{@link GeneralUtil#isEmpty(Object)} 判断元素
     *
     * @param args 被检查对象
     * @return 是否全部为null或空对象
     */
    public static boolean isAllEmpty(Object... args) {
        if (ArrayUtil.isNotEmpty(args)) {
            for (Object element : args) {
                if (isNotEmpty(element)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 是否非空对象或非正数
     *
     * @param obj 被检查对象
     * @return 是否存在
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 是否为空对象或非正数
     *
     * @param obj 被检查对象
     * @return 是否存在
     */
    public static boolean isEmpty(Object obj) {
        if (null == obj) {
            return true;
        }

        if (obj instanceof CharSequence) {
            return StrUtil.isBlank((CharSequence) obj);
        } else if (obj instanceof Map) {
            return MapUtil.isEmpty((Map) obj);
        } else if (obj instanceof Iterable) {
            return IterUtil.isEmpty((Iterable) obj);
        } else if (obj instanceof Iterator) {
            return IterUtil.isEmpty((Iterator) obj);
        } else if (ArrayUtil.isArray(obj)) {
            return ArrayUtil.isEmpty(obj);
        } else if (obj instanceof Number) {
            return !isPositive((Number) obj);
        }

        return false;
    }

    /**
     * 数值是否为正数值
     * null 视为非正数值
     *
     * @param num 数值
     * @return 是否为正数值，null返回false
     */
    public static boolean isPositive(Number num) {
        return null != num && num.doubleValue() > 0;
    }

    /**
     * 数值是否为非正数值
     * null 视为非正数值
     *
     * @param num 数值
     * @return 是否为非正数，null返回ture
     */
    public static boolean isNotPositive(Number num) {
        return !isPositive(num);
    }

    /**
     * 转换消息字符串
     *
     * @param template 格式化字符串模板
     * @param args     参数集合
     * @return 消息实际字符串
     */
    public static String formatMsg(String template, Object... args) {
        if (ArrayUtil.isEmpty(args)) {
            return template;
        }

        return StrUtil.format(template, args);
    }

    /**
     * LocalDateTime转换为Date
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return null;
        }

        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取调用者堆栈信息
     *
     * @return 调用者堆栈信息
     */
    public static StackTraceElement getCallerStack() {
        StackTraceElement[] arr = Thread.currentThread().getStackTrace();
        int callerIdx = 3;
        if (arr.length < callerIdx) {
            return null;
        }

        return arr[callerIdx];
    }

}
