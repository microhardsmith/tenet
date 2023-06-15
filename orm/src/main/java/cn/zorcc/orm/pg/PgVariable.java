package cn.zorcc.orm.pg;

import com.ongres.scram.client.ScramClient;
import com.ongres.scram.client.ScramSession;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
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

}
