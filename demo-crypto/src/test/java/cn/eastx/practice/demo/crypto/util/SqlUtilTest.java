package cn.eastx.practice.demo.crypto.util;

import cn.eastx.practice.demo.crypto.config.mp.SqlCondOperation;
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
    public void test_selectListSqlCondPair() {
        String sql = "SELECT id, name, password, salt, phone, email, create_time, update_time FROM crypto_user "
                + " WHERE  phone = ? AND phone IN (?, ?) OR email LIKE CONCAT(', ?, ') "
                + " GROUP  BY  phone  HAVING  phone = ?  AND  phone IN (?, ?) "
                + " ORDER  BY  id DESC  LIMIT 0, 10";
        List<SqlCondOperation> operationList = SqlUtil.listSqlCondOperation(sql);
        operationList.forEach(operation -> {
            log.debug("operation={}, v={}", operation, sql.substring(0, operation.getOriginCondStartIdx()));
        });
    }

    @Test
    public void test_updateListSqlCondPair() {
        String sql = "UPDATE crypto_user  SET phone=?,email=?  \n" +
                " \n" +
                " WHERE (id = ?)";
        List<SqlCondOperation> operationList = SqlUtil.listSqlCondOperation(sql);
        operationList.forEach(operation -> {
            log.debug("operation={}", operation);
        });
    }

    @Test
    public void test_deleteListSqlCondPair() {
        String sql = "DELETE FROM crypto_user "
                + " WHERE  phone = ? AND phone IN (?, ?) OR email LIKE CONCAT(', ?, ') ";
        List<SqlCondOperation> operationList = SqlUtil.listSqlCondOperation(sql);
        operationList.forEach(operation -> {
            log.debug("operation={}", operation);
        });
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
