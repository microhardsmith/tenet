package cn.zorcc.log;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDateTime;

/**
 * 用于在日志中记录时间,固定日志时间格式为 yyyy-MM-dd hh:mm:ss.SSS
 * 每超过一分钟重新获取时间进行刷新
 */
public class LogTime {
    /**
     * 基准时间戳
     */
    private long baseline;
    /**
     * baseline对应毫秒数
     */
    private int milli;
    /**
     * baseline对应秒数
     */
    private int second;
    /**
     *  上一次刷新时的时间戳
     */
    private long timestamp;
    /**
     *  时间刷新间隔,默认每个被复用的LogTime每隔30s或分钟数产生变化时刷新一次当前时间
     */
    private static final int refreshIntervalSecond = 30;
    private static final String format = "yyyy-MM-dd hh:mm:ss.SSS ";
    private final MemorySegment segment = MemorySegment.ofArray(new byte[format.length()]);

    public LogTime(LocalDateTime base) {
        this.baseline = base.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
        this.milli = base.getNano() / 1_000_000;
        this.second = base.getSecond();
        fillIrrelevant();
        fillMajor(base.getYear(), base.getMonthValue(), base.getDayOfMonth(), base.getHour(), base.getMinute());
        fillMinor(second, milli);
    }

    /**
     *  获取当前MemorySegment
     */
    public MemorySegment segment() {
        return segment;
    }

    /**
     *  获取当前MemorySegment对应时间戳
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     *  刷新当前时间格式对应的MemorySegment,非线程安全
     */
    public void refresh() {
        this.timestamp = Clock.current();
        int interval = (int) (timestamp - baseline);
        if(interval < 0) {
            // time rollback happens, just use old timestamp
            return ;
        }
        int intervalSecond = interval / 1_000;
        int intervalMilli = interval % 1_000;
        int currentMilli = this.milli + intervalMilli;
        int currentSec = this.second + intervalSecond;
        if (currentMilli >= 1_000) {
            currentMilli -= 1_000;
            currentSec += 1;
        }
        if(intervalSecond >= refreshIntervalSecond || currentSec >= 60) {
            // needs to refresh local timestamp
            LocalDateTime now = LocalDateTime.now();
            this.baseline = now.toInstant(Constants.LOCAL_ZONE_OFFSET).toEpochMilli();
            this.milli = now.getNano() / 1_000_000;
            this.second = now.getSecond();
            fillMajor(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());
            fillMinor(this.second, this.milli);
        }else {
            // if the minute value haven't changed, we can only update the minor
            fillMinor(currentSec, currentMilli);
        }
    }

    /**
     *  填充不相关的字符
     */
    private void fillIrrelevant() {
        segment.set(ValueLayout.JAVA_BYTE, 4, Constants.b1);
        segment.set(ValueLayout.JAVA_BYTE, 7, Constants.b1);
        segment.set(ValueLayout.JAVA_BYTE, 10, Constants.b2);
        segment.set(ValueLayout.JAVA_BYTE, 23, Constants.b2);
        segment.set(ValueLayout.JAVA_BYTE, 13, Constants.b3);
        segment.set(ValueLayout.JAVA_BYTE, 16, Constants.b3);
        segment.set(ValueLayout.JAVA_BYTE, 19, Constants.b4);
        segment.set(ValueLayout.JAVA_BYTE, 23, Constants.b2);
    }

    /**
     *  填充年月日时分数据
     */
    private void fillMajor(int year, int month, int day, int hour, int minute) {
        fill(0, 4, year);
        fill(5, 2, month);
        fill( 8, 2, day);
        fill(11, 2, hour);
        fill(14, 2, minute);
    }

    /**
     *  填充秒级和毫秒级数据
     */
    private void fillMinor(int second, int milli) {
        fill( 17, 2, second);
        fill( 20, 3, milli);
    }

    /**
     * 按照十进制填充MemorySegment各个位
     * @param index 起始索引
     * @param offset 填充长度
     * @param value 填充值
     */
    private void fill(int index, int offset, int value) {
        final int max = index + offset - 1;
        for(int i = max; i >= index; i--) {
            segment.set(ValueLayout.JAVA_BYTE, i, (byte) ((value % 10) + 48));
            value /= 10;
        }
    }

}
