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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PgVariable)) return false;
        final PgVariable other = (PgVariable) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getState() != other.getState()) return false;
        final Object this$scramClient = this.getScramClient();
        final Object other$scramClient = other.getScramClient();
        if (this$scramClient == null ? other$scramClient != null : !this$scramClient.equals(other$scramClient))
            return false;
        final Object this$scramSession = this.getScramSession();
        final Object other$scramSession = other.getScramSession();
        if (this$scramSession == null ? other$scramSession != null : !this$scramSession.equals(other$scramSession))
            return false;
        final Object this$clientFinalProcessor = this.getClientFinalProcessor();
        final Object other$clientFinalProcessor = other.getClientFinalProcessor();
        if (this$clientFinalProcessor == null ? other$clientFinalProcessor != null : !this$clientFinalProcessor.equals(other$clientFinalProcessor))
            return false;
        if (this.getProcessId() != other.getProcessId()) return false;
        if (this.getSecretKey() != other.getSecretKey()) return false;
        final Object this$parameterStatus = this.getParameterStatus();
        final Object other$parameterStatus = other.getParameterStatus();
        if (this$parameterStatus == null ? other$parameterStatus != null : !this$parameterStatus.equals(other$parameterStatus))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PgVariable;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getState();
        final Object $scramClient = this.getScramClient();
        result = result * PRIME + ($scramClient == null ? 43 : $scramClient.hashCode());
        final Object $scramSession = this.getScramSession();
        result = result * PRIME + ($scramSession == null ? 43 : $scramSession.hashCode());
        final Object $clientFinalProcessor = this.getClientFinalProcessor();
        result = result * PRIME + ($clientFinalProcessor == null ? 43 : $clientFinalProcessor.hashCode());
        result = result * PRIME + this.getProcessId();
        result = result * PRIME + this.getSecretKey();
        final Object $parameterStatus = this.getParameterStatus();
        result = result * PRIME + ($parameterStatus == null ? 43 : $parameterStatus.hashCode());
        return result;
    }

    public String toString() {
        return "PgVariable(state=" + this.getState() + ", scramClient=" + this.getScramClient() + ", scramSession=" + this.getScramSession() + ", clientFinalProcessor=" + this.getClientFinalProcessor() + ", processId=" + this.getProcessId() + ", secretKey=" + this.getSecretKey() + ", parameterStatus=" + this.getParameterStatus() + ")";
    }
}
