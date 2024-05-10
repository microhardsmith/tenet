package cn.zorcc.orm.core;

import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.common.network.TagMsg;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PgHandler implements Handler {
    private static final Logger log = new Logger(PgHandler.class);
    private final PgManager pgManager;
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final BlockingQueue<Object> msgQueue = new LinkedTransferQueue<>();
    public PgHandler(PgManager pgManager) {
        this.pgManager = pgManager;
    }

    @Override
    public void onFailed(Channel channel) {

    }

    @Override
    public void onConnected(Channel channel) {

    }

    @Override
    public Optional<TagMsg> onRecv(Channel channel, Object data) {
        return Optional.empty();
    }

    @Override
    public void onShutdown(Channel channel) {

    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
