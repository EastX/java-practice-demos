package cn.eastx.practice.demo.crypto.pojo.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据（值对象）
 *
 * @author EastX
 * @date 2023/2/25
 */
@Data
public class UserVO {

    /**
     * 用户表主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 加密后的密码，MD5加盐
     */
    private String password;

    /**
     * 加密密码使用的盐
     */
    private String salt;

    /**
     * 手机号码，整体加密
     */
    private String phone;

    /**
     * 邮箱，模糊加密
     */
    private String email;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
