package cn.eastx.practice.demo.crypto.config.mp;

import cn.eastx.practice.demo.crypto.util.CryptoDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库字段值加密处理（模糊加密，支持模糊查询）
 *  注意：
 *      1. TypeHandler 仅处理 insert、update、select result ，对于查询条件通过 加密拦截器 {@link CryptoCondInterceptor} 进行处理
 *      2. 模糊加密建议保持字符串 存储值与查询值 长度超过分隔字符数 {@link CryptoDataUtil#DEFAULT_SPLIT_NORMAL_CHAR_NUM} ，否则可能导致模糊查询数据不一致
 *  使用示例：
 *      1. MyBatis-Plus 注解（自动生产 ResultMap ，存在场景不生效）
 *          @TableField(typeHandler = FuzzyCryptoTypeHandler.class)
 *      2. 自定义 ResultMap 配置
 *          <result column="email" property="email" typeHandler="cn.eastx.practice.demo.crypto.config.mp.FuzzyCryptoTypeHandler" />
 *
 * @see CryptoDataUtil#fuzzyEncrypt(String) 默认模糊加密处理
 * @see CryptoDataUtil#decrypt(String) 默认解密处理
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
public class FuzzyCryptoTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        /*
            对非null参数值进行加密，需要通过实体类处理方可，支持 INSERT/UPDATE ENTITY
            当前处理 INSERT ENTITY，UPDATE ENTITY 会先通过拦截器处理
            因为拦截器修改元数据将导致实体类属性值产生变更，所以实体类还是由 TypeHandler 来进行处理
         */
        ps.setString(i, CryptoCond.EncryptionEnum.FUZZY.getCryptoUseVal(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 对可为null的结果进行解密
        return CryptoDataUtil.decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        // 对可为null的结果进行解密
        return CryptoDataUtil.decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        // 对可为null的结果进行解密
        return CryptoDataUtil.decrypt(cs.getString(columnIndex));
    }

}
