package cn.zorcc.common.log;

import cn.zorcc.common.Constants;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 *   默认为Constants.TIME_FORMAT提供的时间转化类,通过反射加载
 */
@SuppressWarnings("unused")
public class DefaultTimeResolver implements TimeResolver {
    private final byte[] bytes = new byte[Constants.TIME_FORMAT.length()];

    public DefaultTimeResolver() {
        bytes[4] = bytes[7] = Constants.b1;
        bytes[10] = Constants.SPACE;
        bytes[13] = bytes[16] = Constants.COLON;
        bytes[19] = Constants.b4;
    }

    @Override
    public byte[] format(LocalDateTime time) {
        fill(time.getYear(), 0, 4);
        fill(time.getMonthValue(), 5, 2);
        fill(time.getDayOfMonth(), 8, 2);
        fill(time.getHour(), 11, 2);
        fill(time.getMinute(), 14, 2);
        fill(time.getSecond(), 17, 2);
        fill((int) TimeUnit.NANOSECONDS.toMillis(time.getNano()), 20, 3);
        return bytes;
    }

    private void fill(int value, int index, int offset) {
        int i = index + offset - 1;
        while (i >= index) {
            byte b = (byte) ((value % 10) + 48);
            bytes[i] = b;
            value = value / 10;
            i--;
        }
    }

}
