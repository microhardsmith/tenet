package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.common.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PgHandler implements Handler {
    private final PgManager pgManager;
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final BlockingQueue<Object> msgQueue = new LinkedTransferQueue<>();
    public PgHandler(PgManager pgManager) {
        this.pgManager = pgManager;
    }

    @Override
    public void onConnected(Channel channel) {
        pgManager.registerConn(new PgConn(available, msgQueue));
        channel.send(new PgStartUpMsg(pgManager.pgConfig()));
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        if (!msgQueue.offer(data)) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
