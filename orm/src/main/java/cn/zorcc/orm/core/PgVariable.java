package cn.zorcc.orm.core;

import com.ongres.scram.client.ScramClient;
import com.ongres.scram.client.ScramSession;

import java.util.LinkedHashMap;
import java.util.Map;

public class PgVariable {
    public static final String ERR = "Err state detected";
    public static final int INITIAL = 0;
    public static final int WAITING_AUTH_OK = 1;
    public static final int AUTH_OK = 2;

    private int state = INITIAL;

    private ScramClient scramClient = null;

    private ScramSession scramSession = null;

    private ScramSession.ClientFinalProcessor clientFinalProcessor = null;

    private int processId;

    private int secretKey;

    private Map<String, String> parameterStatus = new LinkedHashMap<>();

    public PgVariable() {
    }

    public int getState() {
        return this.state;
    }

    public ScramClient getScramClient() {
        return this.scramClient;
    }

    public ScramSession getScramSession() {
        return this.scramSession;
    }

    public ScramSession.ClientFinalProcessor getClientFinalProcessor() {
        return this.clientFinalProcessor;
    }

    public int getProcessId() {
        return this.processId;
    }

    public int getSecretKey() {
        return this.secretKey;
    }

    public Map<String, String> getParameterStatus() {
        return this.parameterStatus;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setScramClient(ScramClient scramClient) {
        this.scramClient = scramClient;
    }

    public void setScramSession(ScramSession scramSession) {
        this.scramSession = scramSession;
    }

    public void setClientFinalProcessor(ScramSession.ClientFinalProcessor clientFinalProcessor) {
        this.clientFinalProcessor = clientFinalProcessor;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public void setSecretKey(int secretKey) {
        this.secretKey = secretKey;
    }

    public void setParameterStatus(Map<String, String> parameterStatus) {
        this.parameterStatus = parameterStatus;
    }
}
