package cn.eastx.practice.demo.crypto.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.crypto.KeyUtil;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyGenerator;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;

/**
 * 加密密钥处理 相关工具类
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
public class CryptoKeyUtil {

    /**
     * 密钥库文件密码
     */
    private static final String KEYSTORE_PWD = "KEYSTORE_PWD";
    /**
     * 密钥库文件存储路径
     */
    private static final String KEYSTORE_PATH = "/crypto/symmetric_key.keystore";

    /**
     * 密钥库中密钥别名
     */
    private static final String KEY_ENTRY_ALIAS = "KEY_ENTRY_ALIAS";
    /**
     * 密钥库中密钥密码
     */
    private static final String KEY_ENTRY_PWD = "KEY_ENTRY_PWD";

    private CryptoKeyUtil() {}

    /**
     * 生成数据加密密钥文件
     *
     * @return 数据加密密钥
     */
    public static String generateCryptoKey() {
        try {
            // 通过指定算法,指定提供者来构造KeyGenerator对象
            KeyGenerator keyGen = KeyGenerator.getInstance("RC4");
            // 初始化KeyGenerator对象,通过指定大小和随机源的方式产生
            keyGen.init(1024);
            // 生成秘钥
            Key key = keyGen.generateKey();

            // 存储到 resource 目录下
            KeyStore keyStore = KeyStore.getInstance(KeyUtil.KEY_TYPE_JCEKS);
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEY_ENTRY_ALIAS, key, KEY_ENTRY_PWD.toCharArray(), null);
            String actualPath = CryptoKeyUtil.class.getResource(KEYSTORE_PATH).getPath();
            keyStore.store(new FileOutputStream(actualPath), KEYSTORE_PWD.toCharArray());
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            log.error("[加密密钥]生成数据加密密钥文件出现异常, e={}", ExceptionUtil.stacktraceToString(e));
        }

        return null;
    }

    /**
     * 获取数据加密密钥
     *
     * @return 数据加密密钥
     */
    public static String getCryptoKey() {
        try {
            KeyStore keyStore = KeyUtil.readKeyStore(KeyUtil.KEY_TYPE_JCEKS,
                    CryptoKeyUtil.class.getResourceAsStream(KEYSTORE_PATH),
                    KEYSTORE_PWD.toCharArray());
            Key key = keyStore.getKey(KEY_ENTRY_ALIAS, KEY_ENTRY_PWD.toCharArray());
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            log.error("[加密密钥]获取数据加密密钥出现异常, e={}", ExceptionUtil.stacktraceToString(e));
        }

        return null;
    }

}
