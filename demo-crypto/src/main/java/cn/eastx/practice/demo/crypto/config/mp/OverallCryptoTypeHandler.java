package cn.eastx.practice.demo.crypto.config.mp;

import cn.eastx.practice.demo.crypto.util.CryptoDataUtil;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 数据库字段值加密处理（整体加密，不支持模糊查询）
 *  注意：TypeHandler 仅处理 insert、update、select result ，对于查询条件通过 加密拦截器 {@link CryptoCondInterceptor} 进行处理
 *  使用示例：
 *      1. MyBatis-Plus 注解（自动生产 ResultMap ，存在场景不生效）
 *          @TableField(typeHandler = OverallCryptoTypeHandler.class)
 *      2. 自定义 ResultMap 配置
 *          <result column="phone" property="phone" typeHandler="cn.eastx.practice.demo.crypto.config.mp.OverallCryptoTypeHandler" />
 *
 * @see CryptoDataUtil#overallEncrypt(String) 默认整体加密处理
 * @see CryptoDataUtil#decrypt(String) 默认解密处理
 *
 * @author EastX
 * @date 2022/11/11
 */
public class OverallCryptoTypeHandler extends DefaultStringTypeHandler {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        /*
            对非null参数值进行加密，需要通过实体类处理方可，支持 INSERT/UPDATE ENTITY
            当前处理 INSERT ENTITY，UPDATE ENTITY 会先通过拦截器处理
            因为拦截器修改元数据将导致实体类属性值产生变更，所以实体类还是由 TypeHandler 来进行处理
         */
        ps.setString(i, CryptoCond.EncryptionEnum.OVERALL.getCryptoUseVal(parameter));
    }

}
