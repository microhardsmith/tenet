package cn.zorcc.common.network;

public record SentryWithCallback(
        Sentry sentry,
        Runnable runnable
) {

}
