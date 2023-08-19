package cn.eastx.practice.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 压缩处理工具类
 *
 * @author EastX
 * @date 2023/08/19
 */
public class CompressUtil {

    private static final Logger logger = LoggerFactory.getLogger(CompressUtil.class);

    private CompressUtil() {}

    /**
     * 使用gzip压缩字符串
     * GZip压缩 256字节以上才有压缩效果
     *
     * @param str 要压缩的字符串
     * @return 压缩后的字符串
     */
    public static String compress(String str) {
        if (str == null || str.length() <= 0) {
            return str;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            logger.error(GeneralUtil.formatMsg("[CompressUtil]compress error, str={}", str), e);
        }

        return null;
    }

    /**
     * 压缩对象为字符串
     *
     * @param obj 对象
     * @return 压缩后字符串
     */
    public static String compressObj(Object obj) {
        String jsonStr = JsonUtil.defFacade().toJsonStr(obj);
        return compress(jsonStr);
    }

    /**
     * 字符串解压缩
     *
     * @param compressedStr 压缩字符串
     * @return 解压后的字符串
     */
    public static String uncompress(String compressedStr) {
        if (compressedStr == null || compressedStr.length() <= 0) {
            return compressedStr;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(compressedStr));
             GZIPInputStream gzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            int offset;
            while ((offset = gzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }

            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            logger.error(GeneralUtil.formatMsg(
                    "[CompressUtil]uncompress error, str={}", compressedStr), e);
        }

        return null;
    }

    /**
     * 字符串解压缩为对象
     *
     * @param compressedStr 被压缩的字符串
     * @return 解压后的对象
     */
    public static <T> T uncompressObj(String compressedStr, Class<T> clazz) {
        String jsonStr = uncompress(compressedStr);
        return JsonUtil.defFacade().parseObject(jsonStr, clazz);
    }

}
