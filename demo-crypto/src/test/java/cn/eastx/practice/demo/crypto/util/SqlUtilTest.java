package cn.eastx.practice.demo.crypto.util;

import cn.eastx.practice.demo.crypto.config.mp.SqlCondOperation;
import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * SQL 处理 相关工具类 测试
 *
 * @author EastX
 * @date 2022/11/12
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SqlUtilTest {

    @Test
    public void test_selectSqlCondOperation() {
        String sql = "SELECT id, name, password, salt, phone, email, create_time, update_time FROM crypto_user "
                + " WHERE  phone = ? AND phone IN (?, ?) OR email LIKE CONCAT(', ?, ') "
                + " GROUP  BY  phone  HAVING  phone = ?  AND  phone IN (?, ?) "
                + " ORDER  BY  id DESC  LIMIT 0, 10";
        test_log(sql);
    }

    /**
     * 测试打印
     *
     * @param sql SQL 字符串
     */
    private void test_log(String sql) {
        Pair<String, List<SqlCondOperation>> sqlPair = SqlUtil.getSqlCondOperationPair(sql);
        sql = sqlPair.getKey();
        log.debug("sql={}, operationList={}", sql, sqlPair.getValue());

        for (SqlCondOperation operation : sqlPair.getValue()) {
            log.debug("operation={}", operation);
            int middleLen = operation.getOriginCond().length();
            int middleEnd = operation.getOriginCondStartIdx() + middleLen;
            log.debug("originCond={}, middleLen={}, middleEnd={}",
                    operation.getOriginCond(), middleLen, middleEnd);
            log.debug("prefix={}\n  middle={}\n suffix={}",
                    sql.substring(0, operation.getOriginCondStartIdx()),
                    sql.substring(operation.getOriginCondStartIdx(), middleEnd),
                    sql.substring(middleEnd));
        }

        int addIdxLen = 0;
        for (SqlCondOperation operation : sqlPair.getValue()) {
            String condStr = operation.getOriginCond();
            sql = operation.replaceSqlCond(sql, addIdxLen,
                    operation.getColumnName(), "crypto_user." + operation.getColumnName());
            int replacedLen = operation.getOriginCond().length() - condStr.length();
            log.debug("replacedLen={}, sql={}", replacedLen, sql);
            addIdxLen += replacedLen;
        }

        log.debug("---------- divider ----------");
    }

    @Test
    public void test_updateSqlCondOperation() {
        String sql = "UPDATE crypto_user  SET phone=?,email=?  \n"
                + " \n"
                + " WHERE (id = ?)";
        test_log(sql);

        sql = "UPDATE crypto_user "
                + " SET phone  =  ? , email  =  '11', "
                + "     id = (select id FROM crypto_user WHERE id = 1)  \n"
                + " \n"
                + " WHERE (id = ?)";
        test_log(sql);
    }

    @Test
    public void test_deleteSqlCondOperation() {
        String sql = "DELETE FROM crypto_user "
                + " WHERE  phone = ? AND phone IN (?, ?) OR email LIKE CONCAT(', ?, ') ";

        test_log(sql);
    }

    @Test
    public void test_selectByJSqlParser() {
        String sql = "SELECT id, name, password, salt, phone, email, create_time, update_time FROM crypto_user "
                + " WHERE  phone = ? AND phone IN (?, ?) OR email LIKE CONCAT(', ?, ') "
                + " GROUP  BY  phone  HAVING  phone = ?  AND  phone IN (?, ?) "
                + " ORDER  BY  id DESC  LIMIT 0, 10";

        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plain = (PlainSelect) select.getSelectBody();
            Expression where = plain.getWhere();
            if (where != null) {
                log.debug("selectByJSqlParser where:{}", where.toString());
            }
            Expression having = plain.getHaving();
            if (having != null) {
                log.debug("selectByJSqlParser having:{}", having.toString());
            }
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_updateByJSqlParser() {
        String sql = "UPDATE crypto_user  SET phone=?,email='11',id=(select id FROM crypto_user " +
                "WHERE id = 1)  \n" +
                " \n" +
                " WHERE (id = ?)";

        try {
            Update update = (Update) CCJSqlParserUtil.parse(sql);
            log.debug("update={}", update);
            for (UpdateSet set : update.getUpdateSets()) {
                log.debug("updateSet={}", set.getExpressions());
            }

            Expression where = update.getWhere();
            if (where != null) {
                log.debug("updateByJSqlParser where:{}", where.toString());
            }
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_deleteByJSqlParser() {
        String sql = "DELETE FROM crypto_user "
                + " WHERE  phone = ? AND phone IN (?, ?) OR email LIKE CONCAT(', ?, ') ";

        try {
            Delete delete = (Delete) CCJSqlParserUtil.parse(sql);
            Expression where = delete.getWhere();
            if (where != null) {
                log.debug("deleteByJSqlParser where:{}", where.toString());
            }
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

}
