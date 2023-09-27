package cn.zorcc.orm.core;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.orm.frontend.PgStartUpMsg;
import cn.zorcc.orm.frontend.PgTerminateMsg;

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
    public void onConnected(Channel channel) {
        pgManager.registerConn(new PgConn(pgManager, channel, available, msgQueue));
        channel.send(new PgStartUpMsg(pgManager.pgConfig()));
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        if (!msgQueue.offer(data)) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    @Override
    public void onShutdown(Channel channel) {
        channel.send(PgTerminateMsg.INSTANCE);
    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
