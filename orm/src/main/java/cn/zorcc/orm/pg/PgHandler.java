package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.ChannelHandler;
import cn.zorcc.common.network.Msg;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PgHandler implements ChannelHandler {
    private static final int INITIAL_STATE = 1;
    private static final int WAITING_SSL_STATE = 2;
    private static final int WAITING_START_UP_STATE = 3;
    private static final int WAITING_CLEAR_PASSWORD_STATE = 4;
    private static final int WAITING_MD5_PASSWORD_STATE = 5;
    private static final int WAITING_SASL_INITIAL_STATE = 6;
    private static final int WAITING_SASL_FINAL_STATE = 7;
    private static final int OK_STATE = 8;
    private volatile int state = INITIAL_STATE;
    @Override
    public void onConnected(Channel channel) {
        log.debug("Successfully connected to the postgresql server : {}", channel.loc());
        if(state == INITIAL_STATE) {
            channel.send(Msg.of(PgCommand.SSL, () -> state = WAITING_SSL_STATE));
        }else {
            throw new FrameworkException(ExceptionType.ORM, Constants.UNREACHED);
        }
    }

    @Override
    public void onRecv(Channel channel, Object data) {

    }

    @Override
    public void onClose(Channel channel) {

    }
}
