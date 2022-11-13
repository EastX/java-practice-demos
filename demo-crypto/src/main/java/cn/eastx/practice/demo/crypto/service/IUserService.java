package cn.eastx.practice.demo.crypto.service;

import cn.eastx.practice.demo.crypto.pojo.po.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户表 Service 层接口
 *
 * @author EastX
 * @date 2022/11/11
 */
public interface IUserService extends IService<User> {

    /**
     * 查询用户通过手机号、邮箱
     *
     * @param phone 手机号
     * @param email 邮箱
     * @return 用户数据
     */
    User getByPhoneEmail(String phone, String email);

    /**
     * 查询用户通过手机号、邮箱（包含非预编译语句）
     *
     * @param phone 手机号
     * @param email 邮箱
     * @return 用户数据
     */
    User getByPhoneEmailNonPrepare(String phone, String email);

    /**
     * 查询用户通过手机号
     *
     * @param phone 手机号
     * @return 用户数据
     */
    User getByPhone(String phone);

    /**
     * 查询用户通过邮箱
     *
     * @param email 邮箱
     * @return 用户数据
     */
    User getByEmail(String email);

}
