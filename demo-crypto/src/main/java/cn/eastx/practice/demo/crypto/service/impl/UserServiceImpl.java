package cn.eastx.practice.demo.crypto.service.impl;

import cn.eastx.practice.demo.crypto.dao.UserMapper;
import cn.eastx.practice.demo.crypto.pojo.po.User;
import cn.eastx.practice.demo.crypto.pojo.vo.UserVO;
import cn.eastx.practice.demo.crypto.service.IUserService;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户表 Service 层实现
 *
 * @author EastX
 * @date 2022/11/11
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public User getByPhoneEmail(String phone, String email) {
        if (ObjectUtil.isAllEmpty(phone, email)) {
            return null;
        }

        return getBaseMapper().getByPhoneEmail(phone, email);
    }

    @Override
    public User getByPhoneEmailNonPrepare(String phone, String email) {
        if (ObjectUtil.isAllEmpty(phone, email)) {
            return null;
        }

        return getBaseMapper().getByPhoneEmailNonPrepare(phone, email);
    }

    @Override
    public User getByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }

        return getBaseMapper().getByPhone(phone);
    }

    @Override
    public User getByEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }

        return getBaseMapper().getByEmail(email);
    }

    @Override
    public UserVO getVoById(Long id) {
        if (Objects.isNull(id) || id <= 0) {
            return null;
        }

        return getBaseMapper().getVoById(id);
    }
}
