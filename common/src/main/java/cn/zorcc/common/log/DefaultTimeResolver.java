package cn.zorcc.common.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;

/**
 *  A default time-resolver for TIME_FORMAT
 */
public final class DefaultTimeResolver implements TimeResolver {
    @Override
    public MemorySegment format(LocalDateTime time) {
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer(Constants.DEFAULT_TIME_FORMAT.length())) {
            int year = time.getYear();
            int month = time.getMonthValue();
            int day = time.getDayOfMonth();
            int hour = time.getHour();
            int minute = time.getMinute();
            int second = time.getSecond();
            int milli = time.getNano() / 1_000_000;
            writeBuffer.writeByte(NativeUtil.toAsciiByte(year / 1000));
            writeBuffer.writeByte(NativeUtil.toAsciiByte((year / 100) % 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte((year / 10) % 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(year % 10));
            writeBuffer.writeByte(Constants.HYPHEN);
            writeBuffer.writeByte(NativeUtil.toAsciiByte(month / 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(month % 10));
            writeBuffer.writeByte(Constants.HYPHEN);
            writeBuffer.writeByte(NativeUtil.toAsciiByte(day / 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(day % 10));
            writeBuffer.writeByte(Constants.SPACE);
            writeBuffer.writeByte(NativeUtil.toAsciiByte(hour / 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(hour % 10));
            writeBuffer.writeByte(Constants.COLON);
            writeBuffer.writeByte(NativeUtil.toAsciiByte(minute / 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(minute % 10));
            writeBuffer.writeByte(Constants.COLON);
            writeBuffer.writeByte(NativeUtil.toAsciiByte(second / 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(second % 10));
            writeBuffer.writeByte(Constants.PERIOD);
            writeBuffer.writeByte(NativeUtil.toAsciiByte(milli / 100));
            writeBuffer.writeByte(NativeUtil.toAsciiByte((milli / 10) % 10));
            writeBuffer.writeByte(NativeUtil.toAsciiByte(milli % 10));
            return writeBuffer.asSegment();
        }
    }
}
