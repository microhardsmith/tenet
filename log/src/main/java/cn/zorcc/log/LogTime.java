package cn.zorcc.log;

import cn.zorcc.common.Clock;

import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * 用于在日志中记录时间
 * 每超过一分钟重新获取时间进行刷新
 */
public class LogTime {
    /**
     * 时间戳
     */
    private long timestamp;
    /**
     * 时间戳对应毫秒数
     */
    private long milli;
    /**
     * 时间戳对应秒数
     */
    private long second;

    /**
     *  时间戳格式,该项可根据项目具体情况进行调整
     */
    private static final String defaultFormat = "yyyy-MM-dd hh:mm:ss.SSS";
    private static final ZoneOffset localZoneOffset = OffsetTime.now().getOffset();
    /**
     *  时间刷新间隔,默认每个被复用的LogTime每隔30s或分钟数产生变化时刷新一次当前时间
     */
    private static final long refreshIntervalSecond = 30L;
    private final char[] timeArray = new char[defaultFormat.length()];

    public LogTime() {
        timeArray[4] = timeArray[7] = '-';
        timeArray[10] = ' ';
        timeArray[13] = timeArray[16] = ':';
        timeArray[19] = '.';
        refreshTime();
    }

    /**
     * 向指定StringBuilder中添加当前时间的字符串,格式如yyyy-MM-dd hh:mm:ss.SSS
     * 该方法并不是线程安全的,日志模型保证了LogEvent始终被单线程处理
     */
    public char[] timeArray() {
        long interval = Clock.current() - timestamp;
        if (interval < 0) {
            // 出现时间回滚的情况,直接返回当前时间戳
            return timeArray;
        }
        long intervalSecond = interval / 1000;
        long intervalMilli = interval % 1000;
        long currentMilli = milli + intervalMilli;
        long currentSec = second + intervalSecond;
        if (currentMilli >= 1000) {
            currentMilli -= 1000;
            currentSec += 1;
        }
        if (intervalSecond >= refreshIntervalSecond || currentSec >= 60) {
            // 刷新时间并返回新的计时
            refreshTime();
            return timeArray();
        } else {
            fillArray((int) currentSec, 2, timeArray, 17);
            fillArray((int) currentMilli, 3, timeArray, 20);
            return timeArray;
        }
    }

    /**
     * 更新当前时间参数
     */
    private void refreshTime() {
        LocalDateTime now = LocalDateTime.now();
        this.timestamp = now.toInstant(localZoneOffset).toEpochMilli();
        this.milli = TimeUnit.NANOSECONDS.toMillis(now.getNano());
        this.second = now.getSecond();
        fillArray(now.getYear(), 4, timeArray, 0);
        fillArray(now.getMonthValue(), 2, timeArray, 5);
        fillArray(now.getDayOfMonth(), 2, timeArray, 8);
        fillArray(now.getHour(), 2, timeArray, 11);
        fillArray(now.getMinute(), 2, timeArray, 14);
    }

    /**
     * 根据数值给char数组填充具体字符
     */
    private void fillArray(int value, int length, char[] target, int index) {
        for (int i = 1; i <= length; i++) {
            target[index + length - i] = Character.forDigit(value % 10, 10);
            value = value / 10;
        }
    }

}
