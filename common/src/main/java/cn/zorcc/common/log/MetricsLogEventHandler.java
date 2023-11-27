package cn.zorcc.common.log;

import java.util.function.Consumer;

/**
 *   Print log to the remote metrics server
 *   TODO not implemented yet
 */
public final class MetricsLogEventHandler implements Consumer<LogEvent> {
    public MetricsLogEventHandler(LogConfig logConfig) {

    }

    @Override
    public void accept(LogEvent event) {

    }
}
