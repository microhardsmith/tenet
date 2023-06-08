package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Net;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.orm.PgConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 *   The main postgresql manager for handling all incoming msg
 */
@Slf4j
public final class PgManager implements LifeCycle {
    private final Net net;
    private final PgConfig pgConfig;
    private final TransferQueue<PgConn> connPool = new LinkedTransferQueue<>();

    public PgManager(Net net, PgConfig pgConfig) {
        this.net = net;
        this.pgConfig = pgConfig;
    }

    public PgConfig pgConfig() {
        return pgConfig;
    }

    public void registerConn(PgConn conn) {
        if (!connPool.offer(conn)) {
            throw new FrameworkException(ExceptionType.CONTEXT, Constants.UNREACHED);
        }
    }

    public PgConn get() {
        try {
            return connPool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException(ExceptionType.POSTGRESQL, "thread interrupt", e);
        }
    }



    @Override
    public void init() {
        Loc loc = pgConfig.getLoc();
        new PgEncoder();
        new PgDecoder();
        new PgHandler(this);
        // net.connect(loc, );
    }

    @Override
    public void shutdown() {

    }
}
