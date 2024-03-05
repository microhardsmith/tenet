package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.time.Duration;

public record DurationWithCallback(
        Duration duration,
        Runnable callback
) {
    public DurationWithCallback {
        if(duration == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
