package cn.eastx.practice.demo.crypto.config.mp;

import cn.eastx.practice.demo.crypto.util.SqlUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Stream;

/**
 * 加密拦截器
 *  根据 Mapper 泛型实体类中字段特定注解拦截 sql，将条件中的明文字段替换为密文字段（如有）、值替换为加密值
 *  注意：模糊查询条件需要 中文（非asscii字符）大于等于两个字符，英文（assii字符）大于等于4个字符
 *
 * @see CryptoCond 加密条件注解
 * @see intercept 线程私有变量设置是否拦截
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
@Intercepts(value = {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class CryptoCondInterceptor implements Interceptor {

    /** 加密拦截器执行拦截线程本地变量 */
    private static ThreadLocal<Boolean> intercept = ThreadLocal.withInitial(() -> true);

    /**
     * 设置加密拦截器是否处理拦截（默认处理拦截）
     *
     * @param intercept 是否处理拦截
     * @see CryptoCondInterceptor#getIntercept()
     */
    public static void setIntercept(boolean intercept) {
        CryptoCondInterceptor.intercept.set(intercept);
    }

    /**
     * 获取加密拦截器是否处理拦截（默认处理拦截）
     *
     * @return 是否处理拦截
     * @see CryptoCondInterceptor#setIntercept(boolean)
     */
    public static boolean getIntercept() {
        return Boolean.TRUE.equals(intercept.get());
    }

    /**
     * 清除加密拦截器是否处理拦截（默认处理拦截）
     */
    public static void clearIntercept() {
        intercept.remove();
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement =
                (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        // 支持处理 SELECT、UPDATE、DELETE
        boolean canHandler = Stream.of(SqlCommandType.SELECT, SqlCommandType.UPDATE,
                        SqlCommandType.DELETE)
                .anyMatch(item -> item.equals(mappedStatement.getSqlCommandType()));
        if (canHandler && !getIntercept()) {
            clearIntercept();
            return invocation.proceed();
        }

        clearIntercept();
        // 判断是否有参数需要处理
        BoundSql boundSql = statementHandler.getBoundSql();
        if (Objects.isNull(boundSql.getParameterObject())) {
            return invocation.proceed();
        }

        // 获取自定义注解，通过 MapperID 获取到 Mapper 对应的实体类，获取实体类所有注解字段与注解对应 Map
        Map<String, CryptoCond> condMap = mapEntityFieldCond(mappedStatement.getId());
        if (CollectionUtil.isNotEmpty(condMap)) {
            replaceHandle(mappedStatement.getConfiguration(), condMap, boundSql);
        }

        return invocation.proceed();
    }

    /**
     * 替换数据处理
     *  部分参照处理：https://blog.csdn.net/weixin_43861630/article/details/113936742
     *
     * @param configuration 配置
     * @param condMap 条件注解Map
     * @param boundSql 绑定SQL
     */
    private void replaceHandle(Configuration configuration, Map<String, CryptoCond> condMap,
                               BoundSql boundSql) {
        String sql = boundSql.getSql();
        List<SqlCondOperation> operationList = SqlUtil.listSqlCondOperation(sql);
        if (CollectionUtil.isEmpty(operationList)) {
            return;
        }

        MetaObject paramMetaObject = configuration.newMetaObject(boundSql.getParameterObject());
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        int condParamStart = SqlUtil.getSqlCondParamStartIdx(sql);

        int mappingStartIdx = 0;
        for (SqlCondOperation operation : operationList) {
            String columnName = operation.getColumnName();
            String condStr = operation.getOriginCond();
            int condNum = SqlUtil.countPreparePlaceholder(condStr);
            CryptoCond ann = condMap.get(operation.getColumnName());
            if (Objects.nonNull(ann)) {
                // 替换查询条件参数中的列名
                if (StrUtil.isNotBlank(ann.replacedColumn())
                        && condParamStart < operation.getOriginCondStartIdx()) {
                    sql = sql.replace(condStr,
                            condStr.replace(columnName, ann.replacedColumn()));
                }

                // 替换属性值为加密值
                if (condNum == 0) {
                    // 存在非预编译语句条件，直接替换 SQL 条件值
                    String propVal = String.valueOf(paramMetaObject.getValue(columnName));
                    String useVal = getCryptoUseVal(ann, propVal);
                    sql = sql.replace(condStr, condStr.replace(propVal, useVal));
                } else {
                    // 预编译语句条件通过替换条件值处理
                    for (int i = 0; i < condNum; i++) {
                        String propName = mappings.get(mappingStartIdx + i).getProperty();
                        if (!propName.startsWith("et.")) {
                            // 非实体类属性进行值替换，实体类属性通过 TypeHandler 处理
                            String propVal = String.valueOf(paramMetaObject.getValue(propName));
                            paramMetaObject.setValue(propName, getCryptoUseVal(ann, propVal));
                        }
                    }
                }
            }

            mappingStartIdx += condNum;
        }

        ReflectUtil.setFieldValue(boundSql, "sql", sql);
    }

    /**
     * 获取属性值加密后对应的使用值
     *
     * @param ann 加密条件注解
     * @param propertyVal 属性值
     * @return 属性值加密后对应的使用值
     */
    private String getCryptoUseVal(CryptoCond ann, String propertyVal) {
        String actualVal = SqlUtil.val2Normal(propertyVal);
        String useVal = propertyVal.replace(actualVal, ann.encryption().encrypt(actualVal));
        if (Objects.equals(propertyVal, actualVal)) {
            return useVal;
        }

        return propertyVal.replace(actualVal, ann.encryption().encrypt(actualVal));
    }

    /**
     * 获取实体字段注解集合
     *
     * @param mapperId 执行方法唯一ID
     * @return 实体字段注解集合
     */
    private Map<String, CryptoCond> mapEntityFieldCond(String mapperId) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(mapperId.substring(0, mapperId.lastIndexOf(".")));
        Type[] typeArr = clazz.getGenericInterfaces();

        for (Type type : typeArr) {
            if (!(type instanceof ParameterizedType)) {
                continue;
            }

            ParameterizedType pt = (ParameterizedType) type;
            if ("com.baomidou.mybatisplus.core.mapper.BaseMapper".equals(pt.getRawType().getTypeName())) {
                Class entityClazz = (Class) pt.getActualTypeArguments()[0];
                return mapFieldAnnotation(entityClazz, CryptoCond.class);
            }
        }

        return Collections.emptyMap();
    }

    /**
     * 获取字段名 与 注解 对应Map
     *  注意：字段名默认为下划线形式，如果通过 {@link TableField} 指定了数据库字段则使用数据库字段
     *
     * @param entityClazz 实体类型
     * @param annotationClazz 注解类型
     * @return 字段名 与 注解 对应 Map
     */
    private <T extends Annotation> Map<String, T> mapFieldAnnotation(Class entityClazz,
                                                                     Class<T> annotationClazz) {
        Field[] fieldArr = entityClazz.getDeclaredFields();
        Map<String, T> resultMap = Maps.newHashMapWithExpectedSize(fieldArr.length);
        for (Field field : fieldArr) {
            T ann = field.getAnnotation(annotationClazz);
            if (Objects.nonNull(ann)) {
                String key = StrUtil.toUnderlineCase(field.getName());
                TableField tableField = field.getAnnotation(TableField.class);
                boolean existField = Optional.ofNullable(tableField)
                        .filter(TableField::exist)
                        .filter(tf -> StrUtil.isNotBlank(tf.value()))
                        .isPresent();
                if (existField) {
                    key = tableField.value();
                }

                resultMap.put(key, ann);
            }
        }

        return resultMap;
    }

}
