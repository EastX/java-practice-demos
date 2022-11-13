package cn.eastx.practice.demo.crypto.util;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import cn.hutool.crypto.symmetric.DESede;
import cn.hutool.crypto.symmetric.RC4;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

/**
 * 加密数据处理 相关工具类 测试
 *
 * @author EastX
 * @date 2022/11/11
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CryptoDataUtilTest {

    @Test
    public void test_default() {
        System.out.println(CryptoKeyUtil.getCryptoKey());

        String phone = "17700000000";
        String phoneEncrypt = CryptoDataUtil.overallEncrypt(phone);
        String phoneDecrypt = CryptoDataUtil.decrypt(phoneEncrypt);
        log.debug("phone={}, overallEncrypt={}, decrypt={}", phone, phoneEncrypt, phoneDecrypt);

        String email = "crypto@test.com";
        String emailEncrypt = CryptoDataUtil.fuzzyEncrypt(email);
        String emailDecrypt = CryptoDataUtil.decrypt(emailEncrypt);
        log.debug("email={}, fuzzyEncrypt={}, decrypt={}", email, emailEncrypt, emailDecrypt);

        String txt = "12345一二三四五,fda./";
        String txtEncrypt = CryptoDataUtil.fuzzyEncrypt(txt);
        String txtDecrypt = CryptoDataUtil.decrypt(txtEncrypt);
        log.debug("txt={}, fuzzyEncrypt={}, decrypt={}", txt, txtEncrypt, txtDecrypt);
    }

    @Test
    public void test_rc4() {
        RC4 rc4 = SecureUtil.rc4(CryptoKeyUtil.getCryptoKey());

        String txt = "123张贝阿打法,fda./";
        String enHex = rc4.encryptHex(txt);
        String enB64 = rc4.encryptBase64(txt);
        log.debug("enHex={}, enB64={}", enHex, enB64);

        for (int i = 0; i < 10000; i++) {
//            String text = String.valueOf(i);
            String text = String.valueOf(RandomUtil.randomString(4));
            String textEnHex = rc4.encryptHex(text);
            String textDeHex = rc4.decrypt(textEnHex);
            String textEnB64 = rc4.encryptBase64(text);
            String textDeB64 = rc4.decrypt(textEnB64);
            log.debug("textEnHex={}, textEnB64={}", textEnHex, textEnB64);
        }
    }

    @Test
    public void test_symmetric() {
        RC4 rc4 = SecureUtil.rc4(CryptoKeyUtil.getCryptoKey());
        AES aes = new AES(KeyUtil.generateKey("AES"));
        DES des = SecureUtil.des(CryptoKeyUtil.getCryptoKey().getBytes(StandardCharsets.UTF_8));
        DESede desede = SecureUtil.desede(CryptoKeyUtil.getCryptoKey().getBytes(StandardCharsets.UTF_8));

        String[] txts = {"张贝", "1234", "123", "12张", ",;;'", ",.张"};
        for (String txt : txts) {
            // rc4 base64 较为均衡，大致为四个字符加密后为8个字符
            log.debug("rc4, enHex={}, enB64={}", rc4.encryptHex(txt), rc4.encryptBase64(txt));
            log.debug("aes, enHex={}, enB64={}", aes.encryptHex(txt), aes.encryptBase64(txt));
            log.debug("des, enHex={}, enB64={}", des.encryptHex(txt), des.encryptBase64(txt));
            log.debug("desede, enHex={}, enB64={}", desede.encryptHex(txt), desede.encryptBase64(txt));
        }
    }

    @Test
    public void test_limitByOverall() {
        String[] txtArr = {
                // varchar(49) -> 12个中文字符
                "一二三四五六七八九十壹贰",
                // varchar(97) -> 24个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四",
                // varchar(197) -> 49个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九",
                // varchar(253) -> 63个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九一二三四五六七八九十壹贰叁肆",

                // varchar(49) -> 36个ascii字符
                "012345678901234567890123456789012345",
                // varchar(97) -> 72个ascii字符
                "012345678901234567890123456789012345678901234567890123456789012345678901",
                "abcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijkl",
                "~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_",
                "0123456789890stabcdefghijklmntabcdef345678901234567}[];':~!@#$%^&*()_8901",
                // varchar(197) -> 145个ascii字符
                "0123456789012345678901234567890123456789012345678901234567890123456789010123456789012345678901234567890123456789012345678901234567890123456789012",
                "abcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklm",
                "~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+",
                "~!@#$%^&*()_+{}[]9012345678901234567890123456789{}[];bcdefghijklmnop qrstabcdefghijkl^&*()_+{}[];'123456{}[];ijklm #$%^&*()_+{}[];':~!@#$%^&*()_+",
                // varchar(253)-> 189个ascii字符
                "012345678901234567890123456789012345678901234567890123456789012345678901012345678901234567890123456789012345678901234567890123456789012345678901201234567890123456789012345678901234567890123",
                "abcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklmnopqrstabcdefghijklmabcdefghijklmnopqrstabcdefghijklmnopqrstabcd",
                "~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#$%^&*()_+~!@#$%^&*()_+{}[];':~!@#$%^&*()_+{}[];':~!@#",
                "~!@#$%^&*()_+{}[]9012345678901234567890123456789{}[];bcdefghijklmnop qrstabcdefghijkl^&*()_+{}[];'123456{}[];ijklm #$%^&*()_+{}[];':~!@#$%^&*()_+8901234567890120123qrstabcdefg':~!@#$%^&*()_",

                // varchar(97) -> 3个ascii字符+23个中文字符 或 68个ascii字符+1个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三a&1",
                "0123456789890stabcdefghijklmntabcdef345678901234567}[];':~!@#$%^&*()_8一",
                // varchar(197) -> 3个ascii字符+48个中文字符 或 142个ascii字符+1个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八a&1",
                "~!@#$%^&*()_+{}[]9012345678901234567890123456789{}[];bcdefghijklmnop qrstabcdefghijkl^&*()_+{}[];'123456{}[];ijklm #$%^&*()_+{}[];':~!@#$%^&*(放",
                // varchar(253) -> 3个ascii字符+62个中文字符 或 186个ascii字符+1个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九一二三四五六七八九十壹贰叁a&1",
                "~!@#$%^&*()_+{}[]9012345678901234567890123456789{}[];bcdefghijklmnop qrstabcdefghijkl^&*()_+{}[];'123456{}[];ijklm #$%^&*()_+{}[];':~!@#$%^&*()_+8901234567890120123qrstabcdefg':~!@#$%^&*放",
        };

        for (String txt : txtArr) {
            String encryptData = CryptoDataUtil.overallEncrypt(txt);
            log.debug("\n明文={}, 明文长度={}\n 密文长度（整体加密）={}, 密文（整体加密）={}\n 差值={}",
                    txt, txt.length(), encryptData.length(), encryptData,
                    encryptData.length() - txt.length());
        }
    }

    @Test
    public void test_limitByFuzzy() {
        // 模糊加密
        String[] txtArr = {
                // varchar(99) -> 12个中文字符
                "一二三四五六七八九十壹贰",
                // varchar(198) -> 23个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三",
                // varchar(252) -> 29个中文字符
                "一二三四五六七八九十壹贰叁肆伍陆柒捌玖拾一二三四五六七八九",

                // varchar(99) -> 14个ascii字符
                "01234567890123",
                "abcdefghijklmn",
                "~!@#$%^&*()_+{",
                "01stabc7}[];':",
                // varchar(198) -> 25个ascii字符
                "0123456789012345678901234",
                "abcdefghijklmnopqrstabcde",
                "~!@#$%^&*()_+{}[];':~!@#$",
                "~!@#$%{}[]9012238defghi#)",
                // varchar(252)-> 31个ascii字符
                "0123456789012345678901234567890",
                "abcdefghijklmnopqrstabcdefghijk",
                "~!@#$%^&*()_+{}[];':~!@#$%^&[];",
                "~!@#$%^]9015456cdefjdeflm #$()_",
        };

        for (String txt : txtArr) {
            String encryptData = CryptoDataUtil.fuzzyEncrypt(txt);
            log.debug("\n明文={}, 明文长度={}\n 密文长度（模糊加密）={}, 密文（模糊加密）={}\n 差值={}",
                    txt, txt.length(), encryptData.length(), encryptData,
                    encryptData.length() - txt.length());
        }
    }

}
