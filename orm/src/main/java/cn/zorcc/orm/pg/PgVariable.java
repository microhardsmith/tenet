package cn.zorcc.orm.pg;

import com.ongres.scram.client.ScramClient;
import com.ongres.scram.client.ScramSession;
import lombok.Data;

@Data
public class PgVariable {

    private ScramClient scramClient = null;

    private ScramSession scramSession = null;

    private ScramSession.ClientFinalProcessor clientFinalProcessor = null;

}
