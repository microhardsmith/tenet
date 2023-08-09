package cn.zorcc.orm.core;

import cn.zorcc.common.Constants;
import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Net;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.orm.PgConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   The main postgresql manager for handling all incoming msg
 */
public final class PgManager implements LifeCycle {
    private static final Logger log = LoggerFactory.getLogger(PgManager.class);
    private final Net net;
    private final PgConfig pgConfig;
    private final TransferQueue<PgConn> connPool = new LinkedTransferQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Lock lock = new ReentrantLock();

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
            throw new FrameworkException(ExceptionType.SQL, "thread interrupt", e);
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
