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

}
