package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;

/**
 *  A default time-resolver for Constants.TIME_FORMAT
 */
public final class DefaultTimeResolver implements TimeResolver {
    private final MemorySegment segment = MemorySegment.ofArray(new byte[Constants.TIME_FORMAT.length()]);

    public DefaultTimeResolver() {
        NativeUtil.setByte(segment, 4, Constants.HYPHEN);
        NativeUtil.setByte(segment, 7, Constants.HYPHEN);
        NativeUtil.setByte(segment, 10, Constants.SPACE);
        NativeUtil.setByte(segment, 13, Constants.COLON);
        NativeUtil.setByte(segment, 16, Constants.COLON);
        NativeUtil.setByte(segment, 19, Constants.PERIOD);
    }

    @Override
    public MemorySegment format(LocalDateTime time) {
        int year = time.getYear();
        NativeUtil.setByte(segment, 0L, NativeUtil.toAsciiByte(year / 1000));
        NativeUtil.setByte(segment, 1L, NativeUtil.toAsciiByte((year / 100) % 10));
        NativeUtil.setByte(segment, 2L, NativeUtil.toAsciiByte((year / 10) % 10));
        NativeUtil.setByte(segment, 3L, NativeUtil.toAsciiByte(year % 10));
        int month = time.getMonthValue();
        NativeUtil.setByte(segment, 5L, NativeUtil.toAsciiByte(month / 10));
        NativeUtil.setByte(segment, 6L, NativeUtil.toAsciiByte(month % 10));
        int day = time.getDayOfMonth();
        NativeUtil.setByte(segment, 8L, NativeUtil.toAsciiByte(day / 10));
        NativeUtil.setByte(segment, 9L, NativeUtil.toAsciiByte(day % 10));
        int hour = time.getHour();
        NativeUtil.setByte(segment, 11L, NativeUtil.toAsciiByte(hour / 10));
        NativeUtil.setByte(segment, 12L, NativeUtil.toAsciiByte(hour % 10));
        int minute = time.getMinute();
        NativeUtil.setByte(segment, 14L, NativeUtil.toAsciiByte(minute / 10));
        NativeUtil.setByte(segment, 15L, NativeUtil.toAsciiByte(minute % 10));
        int second = time.getSecond();
        NativeUtil.setByte(segment, 17L, NativeUtil.toAsciiByte(second / 10));
        NativeUtil.setByte(segment, 18L, NativeUtil.toAsciiByte(second % 10));
        int milli = time.getNano() / 1000000;
        NativeUtil.setByte(segment, 20L, NativeUtil.toAsciiByte(milli / 100));
        NativeUtil.setByte(segment, 21L, NativeUtil.toAsciiByte((milli / 10) % 10));
        NativeUtil.setByte(segment, 22L, NativeUtil.toAsciiByte(milli % 10));
        return segment;
    }
}
