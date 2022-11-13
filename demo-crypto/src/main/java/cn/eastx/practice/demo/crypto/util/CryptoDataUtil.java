package cn.eastx.practice.demo.crypto.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.RC4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 加密数据处理 相关工具类
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
public class CryptoDataUtil {

    /**
     * 分割后每块普通字符最大数量
     *  ascii 字符占一个字符数，非 ascii 字符占两个字符数
     */
    public static final int DEFAULT_SPLIT_NORMAL_CHAR_NUM = 4;
    /**
     * 不分割数量处理
     */
    public static final int NOT_SPLIT_NUM = 0;
    /**
     * 分割加密串符号
     *  注意：
     *      1. 采用 Base64 编码不能使用 '+' '/' '=' 作为分隔符号
     *      2. 采用 ’ " 做分隔符时 SQL 查询可能出现冲突（非预编译语句）
     *      3. 采用 ‘%’ '_' 可能在模糊查询时出现冲突
     */
    private static final String SPLIT_SYMBOL = ".";
    /**
     * null、空字符串对应的加密原文
     */
    private static final String CRYPTO_EMPTY_DATA = "CRYPTO_EMPTY_DATA";

    /**
     * 默认加密密钥
     */
    public static final String STORE_KEY = CryptoKeyUtil.getCryptoKey();

    private CryptoDataUtil() {}

    /**
     * 获取对称加密算法
     *
     * @param key 密钥
     * @return 算法对象
     */
    public static RC4 symmetric(String key) {
        return SecureUtil.rc4(key);
    }

    /**
     * 加密数据处理（使用配置密钥、默认分割块字符数量）
     *
     * @param data 明文字符串
     * @return 加密后密文字符串
     */
    public static String encrypt(String data) {
        return encrypt(data, STORE_KEY);
    }

    /**
     * 加密数据处理（使用默认分割块字符数量）
     *
     * @param data 明文字符串
     * @param key  密钥
     * @return 加密后密文字符串
     */
    public static String encrypt(String data, String key) {
        return encrypt(data, key, DEFAULT_SPLIT_NORMAL_CHAR_NUM);
    }

    /**
     * 加密数据处理（使用默认密钥）
     *
     * @param data 明文字符串
     * @param splitNum 分割后每块最大字符数量（传入用以支持模糊加密），{@link NOT_SPLIT_NUM} 表示不进行分割
     * @return 加密后密文字符串
     */
    public static String encrypt(String data, int splitNum) {
        return encrypt(data, STORE_KEY, splitNum);
    }

    /**
     * 加密数据处理
     *
     * @param data 明文字符串
     * @param key  密钥
     * @param splitNum 分割后每块最大字符数量（传入用以支持模糊加密），{@link NOT_SPLIT_NUM} 表示不进行分割
     * @return 加密后密文字符串
     */
    public static String encrypt(String data, String key, int splitNum) {
        Assert.isTrue(StrUtil.isNotBlank(key), "[加密]密钥为空");

        RC4 symmetric = symmetric(key);
        // null与空字符串特殊处理
        if (data == null || data.length() == 0) {
            return symmetric.encryptBase64(CRYPTO_EMPTY_DATA) + SPLIT_SYMBOL;
        }

        StringBuilder resSb = new StringBuilder();
        List<String> txtList = splitNum <= NOT_SPLIT_NUM || StrUtil.isBlank(data)
                ? Collections.singletonList(data) : splitPlaintext(data, splitNum);
        for (String txt : txtList) {
            resSb.append(symmetric.encryptBase64(txt)).append(SPLIT_SYMBOL);
        }

        return resSb.toString();
    }

    /**
     * 分割字符串
     *  ascii 字符占一个字节数，非 ascii 字符占两个字节数
     *  根据 splitNum 进行分割以支持模糊查询
     *
     * @param txt 字符串内容
     * @param splitNum 分割后每块最大字符数量
     * @return 分割后的字符串集合
     */
    public static List<String> splitPlaintext(String txt, int splitNum) {
        Assert.isTrue(StrUtil.isNotBlank(txt), "[分割字符串]字符串为空");
        Assert.isTrue(splitNum > NOT_SPLIT_NUM, String.format("[分割字符串]分割数量不正确, splitNum=%s", splitNum));

        // 根据字符进行分割
        char[] theArr = txt.toCharArray();
        int arrLen = theArr.length;
        List<String> res = new ArrayList<>(arrLen);
        for (int i = 0; i < arrLen; i++) {
            StringBuilder tmpSb = new StringBuilder();
            int j = i;
            int bt = 0;
            for (; j < arrLen; j++) {
                // ascii 视为1个字符，其它为两个字符
                bt += CharUtil.isAscii(theArr[j]) ? 1 : 2;
                if (bt <= splitNum) {
                    tmpSb.append(theArr[j]);
                } else {
                    break;
                }
            }

            res.add(tmpSb.toString());
            if (j == arrLen) {
                break;
            }
        }

        return res;
    }

    /**
     * 解密数据处理
     *
     * @param data 加密的文字符串
     * @return 解密后数据
     */
    public static String decrypt(String data) {
        return decrypt(data, STORE_KEY);
    }

    /**
     * 解密数据处理
     *
     * @param data 加密的文字符串
     * @param key  密钥
     * @return 解密后数据
     */
    public static String decrypt(String data, String key) {
        Assert.isTrue(StrUtil.isNotBlank(data), "[解密]数据为空");
        Assert.isTrue(StrUtil.isNotBlank(key), "[解密]密钥为空");

        RC4 symmetric = symmetric(key);
        String[] theArr = data.split("[" + SPLIT_SYMBOL + "]");
        StringBuilder resSb = new StringBuilder();
        for (int i = 0, arrLen = theArr.length, lastIdx = arrLen - 1; i < arrLen; i++) {
            if (i < lastIdx) {
                // 非最后一次循环只取解密后第一个字符，依据原本分割规则逆处理 splitPlaintext
                resSb.append(symmetric.decrypt(theArr[i]).substring(0, 1));
            } else {
                resSb.append(symmetric.decrypt(theArr[i]));
            }
        }

        String result = resSb.toString();
        // null或空字符串特殊处理
        if (Objects.equals(result, CRYPTO_EMPTY_DATA)) {
            return "";
        }

        return result;
    }

    /**
     * 校验是已加密的数据
     *
     * @param data 数据
     * @return 是否是已加密的数据
     */
    public static boolean checkEncrypted(String data) {
        if (StrUtil.isBlank(data) || !data.endsWith(SPLIT_SYMBOL)) {
            return false;
        }

        try {
            decrypt(data);
            return true;
        } catch (Exception e) {
            log.debug("校验是已加密的数据, e={}", ExceptionUtil.stacktraceToString(e));
        }

        return false;
    }

    /**
     * 加密数据处理（模糊加密）
     *
     * @param data 明文字符串
     * @return 加密后密文字符串
     */
    public static String fuzzyEncrypt(String data) {
        return checkEncrypted(data) ? data : encrypt(data);
    }

    /**
     * 加密数据处理（整体加密）
     *
     * @param data 明文字符串
     * @return 加密后密文字符串
     */
    public static String overallEncrypt(String data) {
        return checkEncrypted(data) ? data : encrypt(data, NOT_SPLIT_NUM);
    }

}
