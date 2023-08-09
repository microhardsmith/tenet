package cn.zorcc.orm.core;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Shutdown;
import cn.zorcc.common.util.ThreadUtil;
import cn.zorcc.orm.PgConfig;
import cn.zorcc.orm.backend.*;
import cn.zorcc.orm.frontend.PgAuthSaslInitialResponseMsg;
import cn.zorcc.orm.frontend.PgAuthSaslResponseMsg;
import cn.zorcc.orm.frontend.PgPasswordMsg;
import cn.zorcc.orm.frontend.PgTerminateMsg;
import com.ongres.scram.client.ScramClient;
import com.ongres.scram.client.ScramSession;
import com.ongres.scram.common.exception.ScramException;
import com.ongres.scram.common.exception.ScramInvalidServerSignatureException;
import com.ongres.scram.common.exception.ScramParseException;
import com.ongres.scram.common.exception.ScramServerErrorException;
import com.ongres.scram.common.stringprep.StringPreparations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Representing a basic postgresql connection
 */
public final class PgConn {
    private static final Logger log = LoggerFactory.getLogger(PgConn.class);
    /**
     *   Global counter for established postgresql connections
     */
    private static final AtomicInteger counter = new AtomicInteger(Constants.ZERO);
    private final int sequence;
    private final PgManager pgManager;
    private final Channel channel;
    private final AtomicBoolean available;
    private final BlockingQueue<Object> msgQueue;
    private final Thread consumerThread;
    private final PgVariable variable = new PgVariable();

    public PgConn(PgManager pgManager, Channel channel, AtomicBoolean available, BlockingQueue<Object> msgQueue) {
        this.sequence = counter.getAndIncrement();
        this.pgManager = pgManager;
        this.channel = channel;
        this.available = available;
        this.msgQueue = msgQueue;
        this.consumerThread = ThreadUtil.virtual("pgConn-" + sequence, () -> {
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

    public boolean available() {
        return available.get();
    }

    private void debug(String s, Object... args) {
        if(sequence == Constants.ZERO) {
            log.debug(s, args);
        }
    }

    private void onMsg(Object msg) {
        switch (msg) {
            case PgAuthOkMsg() -> handleAuthOk();
            case PgAuthClearPwdMsg() -> handleAuthClearPwd();
            case PgAuthMd5Msg(byte[] salt) -> handleAuthMd5(salt);
            case PgAuthSaslPwdMsg(List<String> mechanisms) -> handleAuthSaslPwd(mechanisms);
            case PgAuthSaslContinueMsg(String serverFirstMsg) -> handleAuthSaslContinue(serverFirstMsg);
            case PgAuthSaslFinalMsg(String serverFinalMsg) -> handleAuthSaslFinal(serverFinalMsg);
            case PgParameterStatusMsg(String key, String value) -> handleParameterStatus(key, value);
            case PgBackendKeyDataMsg(int processId, int secretKey) -> handleBackendData(processId, secretKey);
            case PgReadyMsg(PgStatus status) -> handleReady(status);
            case PgTerminateMsg pgTerminateMsg -> handleTerminate(pgTerminateMsg);
            default -> throw new FrameworkException(ExceptionType.SQL, Constants.UNREACHED);
        }
    }

    private void handleReady(PgStatus status) {
        int state = variable.getState();
        if(state == PgVariable.AUTH_OK) {
            pgManager.registerConn(this);
        }
        // TODO
    }

    private void handleParameterStatus(String key, String value) {
        debug("Receiving parameter status, key : {}, value : {}", key, value);
        variable.getParameterStatus().put(key, value);
    }

    private void handleBackendData(int processId, int secretKey) {
        debug("Receiving processId : {}, secretKey : {}", processId, secretKey);
        variable.setProcessId(processId);
        variable.setSecretKey(secretKey);
    }

    private void handleAuthSaslFinal(String serverFinalMsg) {
        try{
            ScramSession.ClientFinalProcessor clientFinalProcessor = variable.getClientFinalProcessor();
            if(clientFinalProcessor == null) {
                throw new FrameworkException(ExceptionType.SQL, "Empty sasl client processor");
            }
            clientFinalProcessor.receiveServerFinalMessage(serverFinalMsg);
        } catch (ScramInvalidServerSignatureException e) {
            channel.send(PgTerminateMsg.INSTANCE);
            throw new FrameworkException(ExceptionType.SQL, "Invalid server signature", e);
        } catch (ScramParseException e) {
            channel.send(PgTerminateMsg.INSTANCE);
            throw new FrameworkException(ExceptionType.SQL, "Can't parse server final msg", e);
        } catch (ScramServerErrorException e) {
            channel.send(PgTerminateMsg.INSTANCE);
            throw new FrameworkException(ExceptionType.SQL, "Scram server err", e);
        }
    }

    private void handleAuthSaslContinue(String serverFirstMsg) {
        try{
            ScramSession scramSession = variable.getScramSession();
            if(scramSession == null) {
                throw new FrameworkException(ExceptionType.SQL, "Empty sasl session");
            }
            debug("Receiving server first msg : {}", serverFirstMsg);
            ScramSession.ServerFirstProcessor serverFirstProcessor = scramSession.receiveServerFirstMessage(serverFirstMsg);
            ScramSession.ClientFinalProcessor clientFinalProcessor = serverFirstProcessor.clientFinalProcessor(pgManager.pgConfig().getPassword());
            String clientFinalMsg = clientFinalProcessor.clientFinalMessage();
            debug("Generating client final msg ： {}", clientFinalMsg);
            variable.setClientFinalProcessor(clientFinalProcessor);
            channel.send(new PgAuthSaslResponseMsg(clientFinalMsg));
        }catch (ScramException e) {
            channel.send(PgTerminateMsg.INSTANCE);
            throw new FrameworkException(ExceptionType.SQL, "Invalid server first SASL message", e);
        }
    }

    private void handleAuthSaslPwd(List<String> mechanisms) {
        if(!mechanisms.isEmpty()) {
            ScramClient scramClient = ScramClient.channelBinding(ScramClient.ChannelBinding.NO)
                    .stringPreparation(StringPreparations.SASL_PREPARATION)
                    .selectMechanismBasedOnServerAdvertised(mechanisms.toArray(new String[0])).setup();
            String mechanism = scramClient.getScramMechanism().getName();
            debug("Using mechanism : {}", mechanism);
            ScramSession scramSession = scramClient.scramSession("*");
            String clientFirstMsg = scramSession.clientFirstMessage();
            debug("Generating client first msg : {}", clientFirstMsg);
            variable.setScramClient(scramClient);
            variable.setScramSession(scramSession);
            channel.send(new PgAuthSaslInitialResponseMsg(mechanism, clientFirstMsg));
        }else {
            throw new FrameworkException(ExceptionType.SQL, "No mechanism provided");
        }
    }

    private void handleAuthMd5(byte[] salt) {
        try{
            PgConfig pgConfig = pgManager.pgConfig();
            MessageDigest digest = MessageDigest.getInstance(Constants.MD_5);
            digest.update(pgConfig.getPassword().getBytes(StandardCharsets.UTF_8));
            digest.update(pgConfig.getUsername().getBytes(StandardCharsets.UTF_8));
            byte[] d = PgUtil.toHexString(digest.digest()).getBytes(StandardCharsets.UTF_8);
            digest.update(d);
            digest.update(salt);
            channel.send(new PgPasswordMsg("md5" + PgUtil.toHexString(digest.digest())));
            variable.setState(PgVariable.WAITING_AUTH_OK);
        }catch (NoSuchAlgorithmException e) {
            throw new FrameworkException(ExceptionType.SQL, Constants.UNREACHED);
        }
    }

    private void handleAuthClearPwd() {
        String password = pgManager.pgConfig().getPassword();
        channel.send(new PgPasswordMsg(password));
        variable.setState(PgVariable.WAITING_AUTH_OK);
    }

    private void handleAuthOk() {
        if(variable.getState() != PgVariable.WAITING_AUTH_OK) {
            throw new FrameworkException(ExceptionType.SQL, PgVariable.ERR);
        }
        variable.setState(PgVariable.AUTH_OK);
    }

    private void handleTerminate(PgTerminateMsg pgTerminateMsg) {
        channel.send(pgTerminateMsg);
        channel.shutdown(new Shutdown(pgManager.pgConfig().getShutdownTimeout(), TimeUnit.MILLISECONDS));
        Thread.currentThread().interrupt();
    }
}
