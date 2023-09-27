package cn.zorcc.common.log;

import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;

/**
 *   Log time to bytes resolver, offering a better alternative to DateTimeFormatter
 *   Implement this interface to provide a time-resolver with better performance, since the dateFormat is usually fixed
 *   The instance will be loaded by reflection
 */
@FunctionalInterface
public interface TimeResolver {
    MemorySegment format(LocalDateTime time);
}
