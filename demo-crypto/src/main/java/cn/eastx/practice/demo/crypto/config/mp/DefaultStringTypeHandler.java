package cn.eastx.practice.demo.crypto.config.mp;

import cn.eastx.practice.demo.crypto.util.CryptoDataUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.StringTypeHandler;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 默认字符串类型处理器
 *  部分数据进行转换时无法指定类型处理器导致解密失效 {@link OverallCryptoTypeHandler} {@link FuzzyCryptoTypeHandler}，
 *  此类替换 MyBatis 默认的 StringTypeHandler 增加处理判断符合加密数据的值尝试进行解密处理
 *
 * @author EastX
 * @date 2023/2/25
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.CHAR})
@MappedTypes(String.class)
@Slf4j
public class DefaultStringTypeHandler  extends StringTypeHandler {

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Pair<Boolean, String> resPair = getResultPair(rs.getString(columnName));
        if (resPair.getKey()) {
            return resPair.getValue();
        }

        return super.getNullableResult(rs, columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Pair<Boolean, String> resPair = getResultPair(rs.getString(columnIndex));
        if (resPair.getKey()) {
            return resPair.getValue();
        }

        return super.getNullableResult(rs, columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Pair<Boolean, String> resPair = getResultPair(cs.getString(columnIndex));
        if (resPair.getKey()) {
            return resPair.getValue();
        }

        return super.getNullableResult(cs, columnIndex);
    }

    /**
     * 获取自定义结果处理数据
     *
     * @param value 值
     * @return 自定义结果处理数据
     */
    private Pair<Boolean, String> getResultPair(String value) {
        // 加密值进行解密处理
        if (CryptoDataUtil.checkEncrypted(value)) {
            try {
                return Pair.of(true, CryptoDataUtil.decrypt(value));
            } catch (Exception e) {
                log.info("解密数据出现异常，替换为默认处理，value={}, e={}",
                        value, ExceptionUtil.stacktraceToString(e));
            }
        }

        return Pair.of(false, null);
    }

}
