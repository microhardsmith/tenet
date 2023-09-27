package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 *  A default time-resolver for Constants.TIME_FORMAT
 */
@SuppressWarnings("unused")
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
        fill(time.getYear(), 0, 4);
        fill(time.getMonthValue(), 5, 2);
        fill(time.getDayOfMonth(), 8, 2);
        fill(time.getHour(), 11, 2);
        fill(time.getMinute(), 14, 2);
        fill(time.getSecond(), 17, 2);
        fill((int) TimeUnit.NANOSECONDS.toMillis(time.getNano()), 20, 3);
        return segment;
    }

    private void fill(int value, int index, int offset) {
        for(int i = index + offset- Constants.ONE; i >= index; i--) {
            byte b = (byte) ((value % 10) + 48);
            NativeUtil.setByte(segment, i, b);
            value = value / 10;
        }
    }

}
