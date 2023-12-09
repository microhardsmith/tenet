package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.api.Sentry;

public record SentryWithCallback(
        Sentry sentry,
        Runnable runnable
) {
    public SentryWithCallback {
        if(sentry == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
