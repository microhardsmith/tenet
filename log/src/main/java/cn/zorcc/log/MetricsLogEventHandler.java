package cn.zorcc.log;

import cn.zorcc.common.event.EventHandler;

/**
 * 用于将日志采集后发送至Metrics进行保存
 */
public class MetricsLogEventHandler implements EventHandler<LogEvent> {
    public MetricsLogEventHandler(LogConfig logConfig) {

    }

    @Override
    public void handle(LogEvent event) {

    }
}
