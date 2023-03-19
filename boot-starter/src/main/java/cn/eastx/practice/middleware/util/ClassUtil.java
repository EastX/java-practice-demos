package cn.eastx.practice.middleware.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class 工具类
 *
 * @author EastX
 * @date 2023/3/19
 */
public class ClassUtil {

    private static Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    private ClassUtil() {}

    /**
     * 反射执行无参静态方法获取返回值
     *
     * @return ObjectMapper 对象
     */
    public static Object invokeStatic(Class<?> clazz, String methodName) {
        try {
            Method method = clazz.getMethod(methodName);
            return method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            logger.error("[ClassUtil]invokeStatic出现异常, clazz={}, methodName={}, e={}",
                    clazz, methodName, ex);
        }

        return null;
    }

}
