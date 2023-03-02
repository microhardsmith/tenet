package cn.zorcc.log;

import java.time.LocalDateTime;

/**
 *   时间格式转化接口,为日志时间转化提供 可选的 比DateTimeFormatter更快的 解决方案
 */
public interface TimeResolver {
    byte[] format(LocalDateTime time);
}
