package cn.eastx.practice.demo.crypto.dao;

import cn.eastx.practice.demo.crypto.pojo.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 用户表 Dao 层接口
 *
 * @author EastX
 * @date 2022/11/11
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询用户通过手机号、邮箱
     *
     * @param phone 手机号
     * @param email 邮箱
     * @return 用户数据
     */
    User getByPhoneEmail(@Param("phone") String phone, @Param("email") String email);

    /**
     * 查询用户通过手机号、邮箱（包含非预编译语句）
     *
     * @param phone 手机号
     * @param email 邮箱
     * @return 用户数据
     */
    User getByPhoneEmailNonPrepare(@Param("phone") String phone, @Param("email") String email);

    /**
     * 查询用户通过手机号
     *
     * @param phone 手机号
     * @return 用户数据
     */
    @Select("SELECT id, name, password, salt, phone, email, create_time, update_time FROM crypto_user WHERE phone = #{phone} LIMIT 1")
    @ResultMap("BaseResultMap")
    User getByPhone(@Param("phone") String phone);

    /**
     * 查询用户通过邮箱
     *
     * @param email 邮箱
     * @return 用户数据
     */
    @Select("SELECT id, name, password, salt, phone, email, create_time, update_time FROM crypto_user WHERE email LIKE CONCAT('%', #{email}, '%') LIMIT 1")
    @ResultMap("BaseResultMap")
    User getByEmail(@Param("email") String email);

}

