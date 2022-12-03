package cn.eastx.practice.demo.crypto.pojo.po;

import cn.eastx.practice.demo.crypto.config.mp.CryptoCond;
import cn.eastx.practice.demo.crypto.config.mp.FuzzyCryptoTypeHandler;
import cn.eastx.practice.demo.crypto.config.mp.OverallCryptoTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

import static com.baomidou.mybatisplus.annotation.FieldFill.INSERT;
import static com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE;

/**
 * 用户实体类
 *
 * @author EastX
 * @date 2022/11/11
 */
@Data
@TableName(value = "crypto_user", autoResultMap = true)
public class User {

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
    @TableField(typeHandler = OverallCryptoTypeHandler.class)
    @CryptoCond(encryption = CryptoCond.EncryptionEnum.DEFAULT_OVERALL)
    private String phone;

    /**
     * 邮箱，模糊加密
     */
    @TableField(typeHandler = FuzzyCryptoTypeHandler.class)
    @CryptoCond(encryption = CryptoCond.EncryptionEnum.DEFAULT_FUZZY)
    private String email;

    /**
     * 创建时间
     */
    @TableField(fill = INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = INSERT_UPDATE)
    private LocalDateTime updateTime;

}
