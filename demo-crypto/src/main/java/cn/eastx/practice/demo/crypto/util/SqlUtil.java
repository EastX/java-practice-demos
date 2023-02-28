package cn.eastx.practice.demo.crypto.util;

import cn.eastx.practice.demo.crypto.config.mp.SqlCondOperation;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;

import java.util.*;
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
            Pattern.compile("([\\s]+)(set|where|having)([\\s]+)", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件列名正则
     */
    public static Pattern COND_COLUMN_PATTERN =
            Pattern.compile("([\\s]*)([\\w]+)([\\s]*)", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件中表别名正则
     */
    public static Pattern COND_ALIAS_PATTERN =
            Pattern.compile("([\\s]*)([\\w]+)([\\s]*)([.])", Pattern.CASE_INSENSITIVE);
    /**
     * SQL 条件 UPDATE SET 字符串正则
     */
    public static Pattern COND_UPDATE_SET_PATTERN =
            Pattern.compile("([\\s]+)(set)((\\s|.)+)(where|limit)([\\s]+)", Pattern.CASE_INSENSITIVE);

    private SqlUtil() {}

    /**
     * 获取 SQL 与 条件操作对象集合
     *
     * @param sql sql 语句
     * @return SQL 条件操作对象集合
     */
    public static Pair<String, List<SqlCondOperation>> getSqlCondOperationPair(String sql) {
        Matcher matcher = COND_PARAM_START_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return Pair.of(sql, Collections.emptyList());
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            String formatSql = statement.toString();
            List<SqlCondOperation> operationList = Collections.emptyList();
            if (statement instanceof Select) {
                operationList = listSqlCondOperationBySelect(statement);
            } else if (statement instanceof Update) {
                operationList = listSqlCondOperationByUpdate(statement);
            } else if (statement instanceof Delete) {
                operationList = listSqlCondOperationByDelete(statement);
            }

            return Pair.of(formatSql, operationList);
        } catch (JSQLParserException e) {
            log.error("[getSqlCondOperationPair]exception={}", ExceptionUtil.stacktraceToString(e));
        }

        return Pair.of(sql, Collections.emptyList());
    }

    /**
     * 获取 SQL 条件操作对象集合（ SELECT 语句）
     *
     * @param statement SELECT 语句体
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperationBySelect(Statement statement) {
        PlainSelect plain = (PlainSelect) ((Select) statement).getSelectBody();
        String sql = statement.toString();

        List<Pair<Expression, SqlCondOperation.CondTypeEnum>> multiCondPairList =
                new ArrayList<>(2);
        multiCondPairList.add(Pair.of(plain.getWhere(), SqlCondOperation.CondTypeEnum.WHERE));
        multiCondPairList.add(Pair.of(plain.getHaving(), SqlCondOperation.CondTypeEnum.HAVING));
        return expressions2SqlCondOperationList(sql, multiCondPairList);
    }

    /**
     * 获取 SQL 条件操作对象集合（ UPDATE 语句）
     *
     * @param statement UPDATE 语句体
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperationByUpdate(Statement statement) {
        Update update = (Update) statement;
        String sql = statement.toString();

        // SET 条件
        Matcher setMatcher = COND_UPDATE_SET_PATTERN.matcher(sql);
        setMatcher.find();
        int multiCondStart = setMatcher.start();
        setMatcher = COND_UPDATE_SET_PATTERN.matcher(getUpdateSet(update));
        String multiCondStr = setMatcher.replaceAll("");
        setMatcher.reset();
        if (setMatcher.find()) {
            multiCondStart += setMatcher.end();
        }

        List<SqlCondOperation> resultList = new ArrayList<>();
        resultList.addAll(condStr2SqlCondOperationList(
                multiCondStr, multiCondStart, SqlCondOperation.CondTypeEnum.SET));
        List<Pair<Expression, SqlCondOperation.CondTypeEnum>> multiCondPairList =
                Arrays.asList(Pair.of(update.getWhere(), SqlCondOperation.CondTypeEnum.WHERE));
        resultList.addAll(expressions2SqlCondOperationList(sql, multiCondPairList));
        return resultList;
    }

    /**
     * 获取 UPDATE 语句中 SET 子句
     *
     * @param update UPDATE 语句对象
     * @return SET 子句字符串
     */
    private static String getUpdateSet(Update update) {
        StringBuilder setStrSb = new StringBuilder();
        // 与 Update.toString() 一致获取 SET ...
        int j = 0;
        for (UpdateSet updateSet : update.getUpdateSets()) {
            if (j > 0) {
                setStrSb.append(", ");
            }

            if (updateSet.isUsingBracketsForColumns()) {
                setStrSb.append("(");
            }

            for (int i = 0; i < updateSet.getColumns().size(); i++) {
                if (i > 0) {
                    setStrSb.append(", ");
                }
                setStrSb.append(updateSet.getColumns().get(i));
            }

            if (updateSet.isUsingBracketsForColumns()) {
                setStrSb.append(")");
            }

            setStrSb.append(" = ");

            if (updateSet.isUsingBracketsForValues()) {
                setStrSb.append("(");
            }

            for (int i = 0; i < updateSet.getExpressions().size(); i++) {
                if (i > 0) {
                    setStrSb.append(", ");
                }
                setStrSb.append(updateSet.getExpressions().get(i));
            }
            if (updateSet.isUsingBracketsForValues()) {
                setStrSb.append(")");
            }

            j++;
        }

        // JSqlParser 4.5 版本
//        UpdateSet.appendUpdateSetsTo(setStrSb, update.getUpdateSets());
        return setStrSb.toString();
    }

    /**
     * 获取 SQL 条件操作对象集合（ DELETE 语句）
     *
     * @param statement DELETE 语句体
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> listSqlCondOperationByDelete(Statement statement) {
        Delete delete = (Delete) statement;
        String sql = statement.toString();
        List<Pair<Expression, SqlCondOperation.CondTypeEnum>> multiCondPairList =
                Arrays.asList(Pair.of(delete.getWhere(), SqlCondOperation.CondTypeEnum.WHERE));
        return expressions2SqlCondOperationList(sql, multiCondPairList);
    }

    /**
     * SQL 条件字符串转换为 SQL 条件操作对象集合
     *
     * @param sql SQL 字符串
     * @param multiCondPairList 条件字符串解析器与条件类型对应集合，条件字符串解析器包含多个条件
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> expressions2SqlCondOperationList(String sql,
                                                                          List<Pair<Expression, SqlCondOperation.CondTypeEnum>> multiCondPairList) {
        if (CollectionUtil.isEmpty(multiCondPairList)) {
            return Collections.emptyList();
        }

        int prevIdx = 0;
        List<SqlCondOperation> resultList = new ArrayList<>();
        for (Pair<Expression, SqlCondOperation.CondTypeEnum> multiCondPair : multiCondPairList) {
            Expression multiCondExpression = multiCondPair.getKey();
            if (Objects.isNull(multiCondExpression)) {
                continue;
            }

            String multiCondStr = multiCondExpression.toString();
            int multiCondStart = sql.indexOf(multiCondStr, prevIdx);

            resultList.addAll(condStr2SqlCondOperationList(multiCondStr, multiCondStart,
                    multiCondPair.getValue()));
            prevIdx = multiCondStart + multiCondStr.length();
        }

        return resultList;
    }

    /**
     * SQL 条件字符串转换为 SQL 条件操作对象集合
     *
     * @param multiCondStr 条件字符串，包含多个条件
     * @param multiCondStart 条件字符串起始位置
     * @param condTypeEnum 条件类型
     * @return SQL 条件操作对象集合
     */
    public static List<SqlCondOperation> condStr2SqlCondOperationList(String multiCondStr,
                                                                      int multiCondStart,
                                                                      SqlCondOperation.CondTypeEnum condTypeEnum) {
        int singleCondStart = 0;
        int multiCondStrLen = multiCondStr.length();
        Matcher multiCondMatcher = condTypeEnum.getSplitPattern().matcher(multiCondStr);

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

            Matcher condMatcher =
                    COND_COLUMN_PATTERN.matcher(
                            COND_ALIAS_PATTERN.matcher(singleCondStr).replaceAll(""));
            if (condMatcher.find()) {
                SqlCondOperation operation = SqlCondOperation.builder()
                        .columnName(condMatcher.group().trim())
                        .originCond(singleCondStr)
                        .originCondStartIdx(originalCondStart)
                        .condType(condTypeEnum)
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
        if (StrUtil.isBlank(val)) {
            return val;
        }

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
     * 获取 SQL 中表名集合
     *
     * @param sql SQL 语句
     * @return 表名集合
     */
    public static Set<String> listSqlTableName(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                SelectBody selectBody = ((Select) statement).getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plain = (PlainSelect) selectBody;
                    return listSqlTableName(((Table) plain.getFromItem()), plain.getJoins());
                } else if (selectBody instanceof SetOperationList) {
                    SetOperationList operationList = (SetOperationList) selectBody;
                    Set<String> tableNames = new HashSet<>();
                    for (SelectBody tmpBody : operationList.getSelects()) {
                        PlainSelect plain = (PlainSelect) tmpBody;
                        tableNames.addAll(listSqlTableName(((Table) plain.getFromItem()), plain.getJoins()));
                    }

                    return tableNames;
                }

            } else if (statement instanceof Update) {
                Update update = (Update) statement;
                return listSqlTableName(update.getTable(), update.getStartJoins());
            } else if (statement instanceof Delete) {
                Delete delete = (Delete) statement;
                return listSqlTableName(delete.getTable(), delete.getJoins());
            }

        } catch (JSQLParserException e) {
            log.error("[listSqlTableName] sql={}, e={}", sql, ExceptionUtil.stacktraceToString(e));
        }

        return Sets.newHashSet();
    }

    /**
     * 获取 SQL 中表名集合
     *
     * @param table 表数据
     * @param joins 连表数据
     * @return 表名集合
     */
    private static Set<String> listSqlTableName(Table table, List<Join> joins) {
        Set<String> tbs = Sets.newHashSetWithExpectedSize(1);
        tbs.add(getTableName(table));
        if (CollUtil.isEmpty(joins)) {
            return tbs;
        }

        for (Join join : joins) {
            tbs.add(getTableName((Table) join.getRightItem()));
        }

        return tbs;
    }

    /**
     * 获取表名（完整表名可能为 库名.表名 ）
     *
     * @param table 表
     * @return 表名
     */
    private static String getTableName(Table table) {
        String tableName = table.getFullyQualifiedName();
        int pIdx = tableName.lastIndexOf(".");
        if (pIdx > -1) {
            return tableName.substring(pIdx + 1);
        }

        return tableName;
    }

}
