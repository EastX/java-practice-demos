package cn.eastx.practice.demo.crypto.config.mp;

import cn.eastx.practice.demo.crypto.util.SqlUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
 * @see INTERCEPT 线程私有变量设置是否拦截
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
@Intercepts(value = {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class CryptoCondInterceptor implements Interceptor {

    /** 加密拦截器执行拦截线程本地变量 */
    private static ThreadLocal<Boolean> INTERCEPT = ThreadLocal.withInitial(() -> true);

    /**
     * 设置加密拦截器是否处理拦截（默认处理拦截）
     *
     * @param intercept 是否处理拦截
     * @see CryptoCondInterceptor#getIntercept()
     */
    public static void setIntercept(boolean intercept) {
        CryptoCondInterceptor.INTERCEPT.set(intercept);
    }

    /**
     * 获取加密拦截器是否处理拦截（默认处理拦截）
     *
     * @return 是否处理拦截
     * @see CryptoCondInterceptor#setIntercept(boolean)
     */
    public static boolean getIntercept() {
        return Boolean.TRUE.equals(INTERCEPT.get());
    }

    /**
     * 清除加密拦截器是否处理拦截（默认处理拦截）
     */
    public static void clearIntercept() {
        INTERCEPT.remove();
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement =
                (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        // 支持处理 SELECT、UPDATE、DELETE
        boolean canHandle =
                Stream.of(SqlCommandType.SELECT, SqlCommandType.UPDATE, SqlCommandType.DELETE)
                        .noneMatch(item -> item.equals(mappedStatement.getSqlCommandType()))
                || !getIntercept();
        clearIntercept();
        if (canHandle) {
            return invocation.proceed();
        }

        // 判断是否有参数需要处理
        BoundSql boundSql = statementHandler.getBoundSql();
        if (Objects.isNull(boundSql.getParameterObject())) {
            return invocation.proceed();
        }

        // 获取自定义注解，通过 MapperID 获取到 Mapper 对应的实体类，获取实体类所有注解字段与注解对应 Map
        Map<String, CryptoCond> condMap = mapEntityFieldCond(mappedStatement.getId());
        if (MapUtil.isNotEmpty(condMap)) {
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
        Pair<String, List<SqlCondOperation>> sqlPair = SqlUtil.getSqlCondOperationPair(sql);
        List<SqlCondOperation> operationList = sqlPair.getValue();
        if (CollUtil.isEmpty(operationList)) {
            return;
        }

        sql = sqlPair.getKey();
        MetaObject paramMetaObject = configuration.newMetaObject(boundSql.getParameterObject());
        List<ParameterMapping> mappingList = boundSql.getParameterMappings();

        int mappingStartIdx = 0;
        int addIdxLen = 0;
        for (SqlCondOperation operation : operationList) {
            String condStr = operation.getOriginCond();
            int prepareNum = SqlUtil.countPreparePlaceholder(condStr);
            CryptoCond ann = condMap.get(operation.getColumnName());
            if (Objects.nonNull(ann)) {
                sql = replaceColumnNameHandle(sql, addIdxLen, operation, ann);

                if (prepareNum > 0) {
                    prepareHandle(mappingList, mappingStartIdx, prepareNum, ann);
                } else {
                    // 非编译参数，替换属性值为加密值
                    sql = notPrepareHandle(sql, paramMetaObject, addIdxLen, operation, ann);
                }
            }

            mappingStartIdx += prepareNum;
            addIdxLen += operation.getOriginCond().length() - condStr.length();
        }

        ReflectUtil.setFieldValue(boundSql, "sql", sql);
    }

    /**
     * 编译条件值处理
     *
     * @param mappingList 映射列表
     * @param mappingListStartIdx 映射列表起始索引
     * @param prepareNum 编译值数量
     * @param ann 加密注解
     */
    private void prepareHandle(List<ParameterMapping> mappingList, int mappingListStartIdx,
                               int prepareNum, CryptoCond ann) {
        // 预编译语句条件通过替换类型处理器处理
        for (int i = 0; i < prepareNum; i++) {
            ParameterMapping mapping = mappingList.get(mappingListStartIdx + i);
            ReflectUtil.setFieldValue(mapping, "typeHandler", ann.encryption().getTypeHandler());
        }
    }

    /**
     * 非预编译条件值处理
     *
     * @param sql SQL 字符串
     * @param paramMetaObject
     * @param addIdxLen 增加索引位置
     * @param operation 操作对象
     * @param ann 加密注解
     * @return 处理后的 SQL 语句字符串
     */
    private String notPrepareHandle(String sql, MetaObject paramMetaObject, int addIdxLen,
                                    SqlCondOperation operation, CryptoCond ann) {
        // 存在非预编译语句条件，直接替换 SQL 条件值
        String propVal =
                String.valueOf(paramMetaObject.getValue(operation.getColumnName()));
        String useVal = ann.encryption().getCryptoUseVal(propVal);
        return operation.replaceSqlCond(sql, addIdxLen, propVal, useVal);
    }

    /**
     * 替换 SQL 条件列名处理
     * 
     * @param sql SQL 字符串
     * @param addIdxLen 增加索引位置
     * @param operation 操作对象
     * @param ann 加密注解
     * @return 替换后的 SQL
     */
    private String replaceColumnNameHandle(String sql, int addIdxLen, SqlCondOperation operation, 
                                           CryptoCond ann) {
        boolean cantHandle = StrUtil.isBlank(ann.replacedColumn()) || operation.checkSetCond();
        if (cantHandle) {
            return sql;
        }

        // 替换查询条件参数中的列名
        return operation.replaceSqlCond(sql, addIdxLen, operation.getColumnName(), ann.replacedColumn());
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
            String typeName = pt.getRawType().getTypeName();
            if (!"com.baomidou.mybatisplus.core.mapper.BaseMapper".equals(typeName)) {
                if (!BaseMapper.class.isAssignableFrom(Class.forName(typeName))) {
                    continue;
                }
            }

            Class entityClazz = (Class) pt.getActualTypeArguments()[0];
            return mapFieldAnnotation(entityClazz, CryptoCond.class);
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
