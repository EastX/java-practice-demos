package cn.eastx.practice.demo.crypto.util;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;

/**
 * 加密密钥处理 相关工具类 测试
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CryptoKeyUtilTest {

    @Test
    public void test_defaultKeyStore() {
        String key = CryptoKeyUtil.generateCryptoKey();
        log.debug("[加密密钥]生成密钥文件, key={}", key);
        Assert.isTrue(StrUtil.isNotBlank(key), "生成密钥文件返回密钥为空");

        String key2 = CryptoKeyUtil.getCryptoKey();
        log.debug("[加密密钥]获取密钥, key={}", key2);
        Assert.isTrue(StrUtil.isNotBlank(key2), "获取密钥为空");
        Assert.isTrue(Objects.equals(key, key2), "生成密钥文件返回密钥与获取密钥不一致");
    }

}
