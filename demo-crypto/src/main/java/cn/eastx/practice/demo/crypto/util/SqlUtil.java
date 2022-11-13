package cn.eastx.practice.demo.crypto.util;

import cn.eastx.practice.demo.crypto.config.mp.SqlCondOperation;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 处理 相关工具类
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
public class SqlUtil {

    /**
     * SQL 条件起始位置匹配正则
     */
    public static Pattern COND_PARAM_START_PATTERN =
            Pattern.compile("([\\s]+)(where|having)([\\s]+)", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件操作对象分割正则
     */
    public static Pattern COND_OPERATION_PATTERN =
            Pattern.compile("(([\\s]+)(and|or)([\\s]+))", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件 UPDATE SET 操作对象分割正则
     */
    public static Pattern COND_SET_OPERATION_PATTERN =
            Pattern.compile("([\\s]*)(,)([\\s]*)", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件列名正则
     */
    public static Pattern COND_COLUMN_PATTERN =
            Pattern.compile("([\\s]*)([\\w]+)([\\s]*)", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件 UPDATE SET 字符串正则
     */
    public static Pattern COND_UPDATE_SET_PATTERN =
            Pattern.compile("([\\s]+)(set)((\\s|.)+)(where|limit)([\\s]+)",
                    Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件 UPDATE SET 字符串排除干扰正则
     */
    public static Pattern COND_UPDATE_SET_STR_PATTERN =
            Pattern.compile("([\\s]+)(set|where|limit)([\\s]+)", Pattern.CASE_INSENSITIVE);

    private SqlUtil() {}

    /**
     * 获取 SQL 条件操作对象集合
     *
     * @param sql sql 语句
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperation(String sql) {
        Matcher matcher = COND_PARAM_START_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return Collections.emptyList();
        }

        // 重置匹配器
        matcher.reset();

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                return listSqlCondOperationBySelect(statement, matcher);
            } else if (statement instanceof Update) {
                return listSqlCondOperationByUpdate(statement, matcher, sql);
            } else if (statement instanceof Delete) {
                return listSqlCondOperationByDelete(statement, matcher);
            }
        } catch (JSQLParserException e) {
            log.error("[listSqlCondOperation]exception={}", ExceptionUtil.stacktraceToString(e));
        }

        return Collections.emptyList();
    }

    /**
     * 获取 SQL 条件操作对象集合（ SELECT 语句）
     *
     * @param statement SELECT 语句体
     * @param matcher   条件匹配对象
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperationBySelect(Statement statement,
                                                                      Matcher matcher) {
        PlainSelect plain = (PlainSelect) ((Select) statement).getSelectBody();

        List<SqlCondOperation> resultList = new ArrayList<>();
        resultList.addAll(expression2SqlCondOperationList(plain.getWhere(), matcher));
        resultList.addAll(expression2SqlCondOperationList(plain.getHaving(), matcher));
        return resultList;
    }

    /**
     * 获取 SQL 条件操作对象集合（ UPDATE 语句）
     *
     * @param statement UPDATE 语句体
     * @param matcher   条件匹配对象
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperationByUpdate(Statement statement,
                                                                      Matcher matcher,
                                                                      String sql) {
        Update update = (Update) statement;
        Matcher setMatcher = COND_UPDATE_SET_PATTERN.matcher(sql);

        List<SqlCondOperation> resultList = new ArrayList<>();
        if (setMatcher.find()) {
            Matcher setStrMatcher = COND_UPDATE_SET_STR_PATTERN.matcher(setMatcher.group());
            String multiCondStr = setStrMatcher.replaceAll("");
            setStrMatcher.reset().find();
            Matcher multiCondMatcher = COND_SET_OPERATION_PATTERN.matcher(multiCondStr);
            resultList.addAll(condStr2SqlCondOperationList2(
                    multiCondStr, setMatcher.start() + setStrMatcher.end(), multiCondMatcher));
        }

        resultList.addAll(expression2SqlCondOperationList(update.getWhere(), matcher));
        return resultList;
    }

    /**
     * 获取 SQL 条件操作对象集合（ DELETE 语句）
     *
     * @param statement DELETE 语句体
     * @param matcher   条件匹配对象
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperationByDelete(Statement statement,
                                                                      Matcher matcher) {
        Delete delete = (Delete) statement;

        List<SqlCondOperation> resultList = new ArrayList<>();
        resultList.addAll(expression2SqlCondOperationList(delete.getWhere(), matcher));
        return resultList;
    }

    /**
     * SQL 条件字符串转换为 SQL 条件操作对象集合
     *
     * @param multiCondExpression 条件字符串解析器，包含多个条件
     * @param matcher             条件字符串起始匹配正则
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> expression2SqlCondOperationList(Expression multiCondExpression,
                                                                         Matcher matcher) {
        if (multiCondExpression == null || !matcher.find()) {
            return Collections.emptyList();
        }

        String multiCondStr = multiCondExpression.toString();
        int multiCondStart = matcher.end();
        Matcher multiCondMatcher = COND_OPERATION_PATTERN.matcher(multiCondStr);
        return condStr2SqlCondOperationList2(multiCondStr, multiCondStart, multiCondMatcher);
    }

    /**
     * SQL 条件字符串转换为 SQL 条件操作对象集合
     *
     * @param multiCondStr     条件字符串，包含多个条件
     * @param multiCondStart   条件字符串起始位置
     * @param multiCondMatcher 条件字符串匹配正则，SET 使用 ‘,’ ，WHERE/HAVING 使用 and/or
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> condStr2SqlCondOperationList2(String multiCondStr,
                                                                       int multiCondStart,
                                                                       Matcher multiCondMatcher) {
        int singleCondStart = 0;
        int multiCondStrLen = multiCondStr.length();

        List<SqlCondOperation> resultList = new ArrayList<>();
        while (singleCondStart < multiCondStrLen) {
            int originalCondStart = multiCondStart + singleCondStart;
            String singleCondStr;
            if (multiCondMatcher.find()) {
                singleCondStr = multiCondStr.substring(singleCondStart, multiCondMatcher.start());
                singleCondStart = multiCondMatcher.end();
            } else {
                singleCondStr = multiCondStr.substring(singleCondStart);
                singleCondStart = multiCondStrLen;
            }

            Matcher condMatcher = COND_COLUMN_PATTERN.matcher(singleCondStr);
            if (condMatcher.find()) {
                SqlCondOperation operation = SqlCondOperation.builder()
                        .columnName(condMatcher.group().trim())
                        .originCond(singleCondStr)
                        .originCondStartIdx(originalCondStart)
                        .build();
                resultList.add(operation);
            }
        }

        return resultList;
    }

    /**
     * 统计预编译 SQL 占位符数量
     *
     * @param condSqlStr 条件 SQL 字符串
     * @return 条件 SQL 字符串占位符数量
     */
    public static int countPreparePlaceholder(String condSqlStr) {
        char placeholder = '?';
        char[] condSqlStrArr = condSqlStr.toCharArray();

        int result = 0;
        for (char val : condSqlStrArr) {
            if (val == placeholder) {
                result++;
            }
        }

        return result;
    }

    /**
     * 条件值替换为正常值
     * LIKE 条件值可能包含%和_占位符
     * 此处仅仅替换首位占位符
     *
     * @param val 参数值
     * @return 正常值
     */
    public static String val2Normal(String val) {
        Set<String> placeholders = Sets.newHashSet("%", "_");
        boolean needSub = false;
        int startIdx = 0;
        if (placeholders.contains(val.substring(0, 1))) {
            startIdx++;
            needSub = true;
        }

        int endIdx = val.length();
        if (placeholders.contains(val.substring(endIdx - 1, endIdx))) {
            endIdx--;
            needSub = true;
        }

        return needSub ? val.substring(startIdx, endIdx) : val;
    }

    /**
     * 获取 SQL 条件参数起始位置
     * 不存在返回 SQL 的长度
     *
     * @param sql SQL 语句字符串
     * @return SQL 条件参数起始位置
     */
    public static int getSqlCondParamStartIdx(String sql) {
        Matcher condParamMatcher = COND_PARAM_START_PATTERN.matcher(sql);
        if (condParamMatcher.find()) {
            return condParamMatcher.start();
        }

        return sql.length();
    }

}
