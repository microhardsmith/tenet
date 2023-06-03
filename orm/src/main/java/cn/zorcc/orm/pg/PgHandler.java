package cn.zorcc.orm.pg;

import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PgHandler implements Handler {
    private static final int INITIAL_STATE = 1;
    private static final int WAITING_START_UP_STATE = 3;
    private static final int WAITING_CLEAR_PASSWORD_STATE = 4;
    private static final int WAITING_MD5_PASSWORD_STATE = 5;
    private static final int WAITING_SASL_INITIAL_STATE = 6;
    private static final int WAITING_SASL_FINAL_STATE = 7;
    private static final int OK_STATE = 8;
    private final AtomicInteger state = new AtomicInteger();
    @Override
    public void onConnected(Channel channel) {
        channel.send(new PgStartUpMsg());
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        if(data instanceof PgMsg pgMsg) {

        }
    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
