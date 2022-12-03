package cn.eastx.practice.demo.crypto.service;

import cn.eastx.practice.demo.crypto.config.mp.CryptoCondInterceptor;
import cn.eastx.practice.demo.crypto.pojo.po.User;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户表 Service 层 测试
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
@SpringBootTest
public class UserServiceTest {

    @Resource
    private IUserService userService;

    /**
     * 测试插入单条数据
     */
    @Test
    public void test_insert() {
        User user = new User();
        user.setName("test_insert");
        user.setSalt(IdUtil.fastSimpleUUID());
        user.setPassword(SecureUtil.md5("123456" + user.getSalt()));
        user.setPhone("17300000000");
        user.setEmail("test_insert@test.com");
        Assert.isTrue(userService.save(user), "单个新增失败");
        log.debug("inserted id={}", user.getId());
    }

    /**
     * 测试批量插入数据
     */
    @Test
    public void test_insertBatch() {
        List<User> userList = Lists.newArrayList();
        for (int i = 1; i <= 2; i++) {
            User user = new User();
            userList.add(user);

            user.setName("test_insertBatch_" + i);
            user.setSalt(IdUtil.fastSimpleUUID());
            user.setPassword(SecureUtil.md5("123456" + user.getSalt()));
            user.setPhone("1730000000" + i);
            user.setEmail("test_insertBatch_" + i + "@test.com");
        }

        Assert.isTrue(userService.saveBatch(userList), "批量新增失败");
        List<Long> ids = userList.stream().map(User::getId).collect(Collectors.toList());
        log.debug("inserted ids={}", ids);
    }

    @Test
    public void test_update() {
        User user = new User();
        user.setName("test_update");
        user.setSalt(IdUtil.fastSimpleUUID());
        user.setPassword(SecureUtil.md5("1234563" + user.getSalt()));
        user.setPhone("17700000000");
        user.setEmail("test_update@test.com");
        userService.save(user);
        log.debug("inserted user={}", JSONUtil.toJsonStr(user));
        log.debug("inserted user={}", JSONUtil.toJsonStr(userService.getById(user.getId())));

        userService.lambdaUpdate().set(User::getPhone, "17600000000")
                .eq(User::getEmail, user.getEmail()).update();
        log.debug("updated user={}", JSONUtil.toJsonStr(userService.getById(user.getId())));

        // 通过实体类设定值更新会走 TypeHandler 导致实体类中的数据被加密
        user.setName("test_update2");
        user.setPhone("17800000000");
        user.setEmail("test_update2@test.com");
        userService.updateById(user);
        log.debug("update user2={}", JSONUtil.toJsonStr(user));

        User user2 = userService.getById(user.getId());
        log.debug("select user2={}", JSONUtil.toJsonStr(user2));

        User user3 = userService.lambdaQuery()
                .eq(User::getPhone, user.getPhone())
                .eq(User::getEmail, user.getEmail())
                .orderByDesc(User::getId).last("LIMIT 1").one();
        log.debug("select user3={}", JSONUtil.toJsonStr(user3));

        Assert.isTrue(Objects.equals(user.getName(), user2.getName()), "更新数据与查询数据不一致：用户名");
        Assert.isTrue(Objects.equals(user.getPhone(), user2.getPhone()), "更新数据与查询数据不一致：手机号");
        Assert.isTrue(Objects.equals(user.getEmail(), user2.getEmail()), "更新数据与查询数据不一致：邮箱");
        Assert.isTrue(Objects.nonNull(user3), "更新数据后通过手机号邮箱查询不到数据");
        Assert.isTrue(Objects.equals(user.getId(), user3.getId()), "通过手机号邮箱查询的数据ID不一致");
    }

    /**
     * 测试使用 MyBatis-Plus Lambda 查询
     */
    @Test
    public void test_selectNormal() {
        List<User> addUserList = new ArrayList<>();
        User addUser = new User();
        addUserList.add(addUser);
        addUser.setName("test_selectNormal");
        addUser.setSalt(IdUtil.fastSimpleUUID());
        addUser.setPassword(SecureUtil.md5("123456" + addUser.getSalt()));
        addUser.setPhone("17711000000");
        addUser.setEmail("test_selectNormal@test.com");

        User addUser1 = new User();
        addUserList.add(addUser1);
        addUser1.setName("test_selectNormal2");
        addUser1.setSalt(IdUtil.fastSimpleUUID());
        addUser1.setPassword(SecureUtil.md5("123456" + addUser1.getSalt()));
        addUser1.setPhone("17712000000");
        addUser1.setEmail("test_selectNormal2@test.com");
        Assert.isTrue(userService.saveBatch(addUserList), "批量新增失败");

        List<String> phones = addUserList.stream().map(User::getPhone).collect(Collectors.toList());
        List<User> theUserList1 = userService.lambdaQuery()
                .eq(User::getEmail, addUser.getEmail())
                .like(User::getEmail, addUser.getEmail().substring(0, 5))
                .in(User::getPhone, phones).list();
        log.debug("theUserList1={}", theUserList1);

        List<String> emails = addUserList.stream().map(User::getEmail).collect(Collectors.toList());
        List<User> theUserList2 = userService.lambdaQuery().in(User::getEmail, emails).list();
        log.debug("theUserList2={}", theUserList2);

        User user = null;
        // 手机号整体加密，支持整体查询，不支持模糊查询
        user = userService.lambdaQuery().eq(User::getPhone, addUser.getPhone()).last("LIMIT 1").one();
        log.debug("overall by phone, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据手机号整体查不到用户");

        user = userService.lambdaQuery()
                .like(User::getPhone, addUser.getPhone().substring(0, 5))
                .last("LIMIT 1").one();
        log.debug("fuzzy by phone, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.isNull(user), "根据部分手机号模糊查询得到用户");

        // 邮箱模糊加密，支持整体查询、模糊查询
        user = userService.lambdaQuery().eq(User::getEmail, addUser.getEmail()).last("LIMIT 1").one();
        log.debug("overall by email, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据邮箱整体查不到用户");

        user = userService.lambdaQuery()
                .like(User::getEmail, addUser.getEmail().substring(0, 4))
                .last("LIMIT 1").one();
        log.debug("fuzzy by email, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据邮箱模糊查不到用户");

        long count = userService.lambdaQuery().like(User::getEmail, addUser.getEmail().substring(0, 4)).count();
        log.debug("fuzzy by email, count={}", count);
        Assert.isTrue(count >= 1, "根据邮箱模糊查询小于1个");
    }

    /**
     * 测试特殊查询（通过 Mapper.xml、@Select ）
     */
    @Test
    public void test_selectSpecial() {
        User addUser = new User();
        addUser.setName("test_selectSpecial");
        addUser.setSalt(IdUtil.fastSimpleUUID());
        addUser.setPassword(SecureUtil.md5("123456" + addUser.getSalt()));
        addUser.setPhone("17722000000");
        addUser.setEmail("test_selectNormal@test.com");
        Assert.isTrue(userService.save(addUser), "单个新增失败");

        String emailPart = addUser.getEmail().substring(0, 6);
        User user = null;

        user = userService.getByPhoneEmail(addUser.getPhone(), emailPart);
        log.debug("getByPhoneEmail, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据手机号、部分邮箱查不到用户");

        user = userService.getByPhoneEmailNonPrepare(addUser.getPhone(), emailPart);
        log.debug("getByPhoneEmailNonPrepare, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据手机号、部分邮箱（包含非预编译语句）查不到用户");

        user = userService.getByPhone(addUser.getPhone());
        log.debug("getByPhone, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据手机号查不到用户");

        user = userService.getByEmail(emailPart);
        log.debug("getByEmail, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.nonNull(user), "根据部分邮箱查不到用户");
    }

    /**
     * 测试不使用拦截器，注意需要每次查询进行设置（执行判断后将重置）
     */
    @Test
    public void test_noUseInterceptor() {
        User addUser = new User();
        addUser.setName("test_noUseInterceptor");
        addUser.setSalt(IdUtil.fastSimpleUUID());
        addUser.setPassword(SecureUtil.md5("123456" + addUser.getSalt()));
        addUser.setPhone("17733000000");
        addUser.setEmail("test_noUseInterceptor@test.com");
        Assert.isTrue(userService.save(addUser), "单个新增失败");

        String emailPart = addUser.getEmail().substring(0, 5);
        User user = null;

        // 手机号整体加密，支持整体查询，不支持模糊查询
        CryptoCondInterceptor.setIntercept(false);
        user = userService.lambdaQuery().eq(User::getPhone, addUser.getPhone()).last("LIMIT 1").one();
        log.debug("overall by phone, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.isNull(user), "不使用拦截器根据手机号整体查得到用户");

        CryptoCondInterceptor.setIntercept(false);
        user = userService.getByPhoneEmail(addUser.getPhone(), emailPart);
        log.debug("getByPhoneEmail, user={}", JSONUtil.toJsonStr(user));
        Assert.isTrue(Objects.isNull(user), "不使用拦截器根据手机号、部分邮箱查得到用户");
    }

}
