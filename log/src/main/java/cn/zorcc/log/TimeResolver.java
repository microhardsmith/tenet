package cn.zorcc.log;

import java.time.LocalDateTime;

/**
 *   time to bytes resolver, offering a better alternative to DateTimeFormatter
 */
public interface TimeResolver {
    byte[] format(LocalDateTime time);
}
