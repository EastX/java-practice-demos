package cn.eastx.practice.demo.cache.util;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间工具类
 *
 * @author EastX
 * @date 2022/10/22
 */
@Slf4j
public class TimeUtil {

    /**
     * 时间字符串匹配模式
     */
    private static final Pattern TIME_PATTERN = Pattern.compile("(([0-9]+?)((d|h|m|s)))+?");

    private TimeUtil() {}

    /**
     * 解析时间字符串
     *
     * @param timeStr 3h, 2m, 7s 、组合 2d4h10s
     * @return 时长对象
     */
    @Nullable
    public static Duration parseDuration(String timeStr) {
        if (StrUtil.isBlank(timeStr)) {
            return null;
        }

        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        if (!matcher.matches()) {
            log.error("Invalid duration pattern : {}", timeStr);
            return null;
        }

        Duration duration = Duration.ZERO;
        matcher.reset();
        while (matcher.find()) {
            int val = Optional.of(matcher.group(2)).filter(StrUtil::isNumeric)
                    .map(Integer::parseInt).orElse(0);
            StrTimeUnitEnum strTimeUnitEnum = StrTimeUnitEnum.getByCode(matcher.group(3));
            if (strTimeUnitEnum != null) {
                duration = duration.plusSeconds(strTimeUnitEnum.getTimeUnit().toSeconds(val));
            }
        }

        return duration;
    }

    /**
     * 字符串时间单位关联枚举
     */
    @Getter
    @AllArgsConstructor
    enum StrTimeUnitEnum {
        /** 天 */
        DAY("d", "天", TimeUnit.DAYS),
        /** 小时 */
        HOUR("h", "时", TimeUnit.HOURS),
        /** 分钟 */
        SECOND("s", "秒", TimeUnit.SECONDS),
        /** 秒钟 */
        MINUTE("m", "分", TimeUnit.MINUTES),
        ;
        private final String code;
        private final String info;
        private final TimeUnit timeUnit;

        /**
         * 获取枚举通过标识码
         *
         * @param code 标识码
         * @return 枚举
         */
        @Nullable
        public static StrTimeUnitEnum getByCode(String code) {
            return Arrays.stream(StrTimeUnitEnum.values())
                    .filter(item -> Objects.equals(item.getCode(), code))
                    .findFirst().orElse(null);
        }
    }

}
