package cn.eastx.practice.demo.crypto.config.mp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL 条件对应的操作类
 *
 * @author EastX
 * @date 2022/11/13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlCondOperation {

    /**
     * 列名
     */
    private String columnName;

    /**
     * 原始条件字符串
     */
    private String originCond;

    /**
     * 原始条件字符串开始索引
     */
    private Integer originCondStartIdx;

    /**
     * 替换 SQL 中的条件字符串
     *
     * @param sql 原 SQL 字符串
     * @param addIdxLen 增加的索引长度
     * @param condTarget 条件字符串原始值
     * @param condReplacement 条件字符串替换值
     * @return 替换后的 SQL 字符串
     */
    public String replaceSqlCond(String sql, int addIdxLen, String condTarget, String condReplacement) {
        int prefixEnd = this.getOriginCondStartIdx() + addIdxLen;
        String prefix = sql.substring(0, prefixEnd);

        int suffixStart = prefixEnd + this.getOriginCond().length();
        String suffix = sql.substring(suffixStart);

        /*
            替换条件字符串中首个匹配的字符串（截取字符串处理）
            使用 String.replace() 将替换所有
            使用 String.replaceFirst() 会根据正则进行替换，存在正则符号会与实际替换值产生冲突（可通过转义原字符串处理）
         */
        int condStart = this.getOriginCond().indexOf(condTarget);
        int condEnd = condStart + condTarget.length();
        String replacedCond = this.getOriginCond().substring(0, condStart)
                + condReplacement
                + this.getOriginCond().substring(condEnd);
        this.setOriginCond(replacedCond);

        return prefix + replacedCond + suffix;
    }

}
