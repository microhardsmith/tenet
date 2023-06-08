package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Shutdown;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.orm.PgConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Representing a basic postgresql connection
 */
public final class PgConn {
    /**
     *   Global counter for established postgresql connections
     */
    private static final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
    private final PgManager pgManager;
    private final Channel channel;
    private final AtomicBoolean available;
    private final BlockingQueue<Object> msgQueue;
    private final Thread consumerThread;
    private final PgVariable variable = new PgVariable();

    public PgConn(PgManager pgManager, Channel channel, AtomicBoolean available, BlockingQueue<Object> msgQueue) {
        this.pgManager = pgManager;
        this.channel = channel;
        this.available = available;
        this.msgQueue = msgQueue;
        this.consumerThread = ThreadUtil.virtual("pgConn-" + counter.getAndIncrement(), () -> {
            Thread currentThread = Thread.currentThread();
            while (!currentThread.isInterrupted()) {
                try {
                    onMsg(msgQueue.take());
                } catch (InterruptedException e) {
                    currentThread.interrupt();
                }
            }
        });
        consumerThread.start();
    }

    private void onMsg(Object msg) {
        switch (msg) {
            case PgAuthOkMsg pgAuthOkMsg -> handleAuthOk();
            case PgAuthClearPwdMsg pgAuthClearPwdMsg -> handleAuthClearPwd();
            case PgAuthMd5Msg pgAuthMd5Msg -> handleAuthMd5(pgAuthMd5Msg);
            case PgTerminateMsg pgTerminateMsg -> handleTerminate(pgTerminateMsg);
            default -> throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void handleAuthMd5(PgAuthMd5Msg pgAuthMd5Msg) {
        try{
            PgConfig pgConfig = pgManager.pgConfig();
            int salt = pgAuthMd5Msg.salt();
            MessageDigest digest = MessageDigest.getInstance(Constants.MD_5);
            // digest.update();
        }catch (NoSuchAlgorithmException e) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void handleAuthClearPwd() {
        String password = pgManager.pgConfig().getPassword();
        channel.send(new PgPasswordMsg(password));
    }

    private void handleAuthOk() {

    }

    public void stop() {
        consumerThread.interrupt();
    }

    private void handleTerminate(PgTerminateMsg pgTerminateMsg) {
        channel.send(pgTerminateMsg);
        channel.shutdown(new Shutdown(pgManager.pgConfig().getShutdownTimeout(), TimeUnit.MILLISECONDS));
        Thread.currentThread().interrupt();
    }
}
