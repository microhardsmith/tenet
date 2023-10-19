package cn.zorcc.mint;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.common.rpc.RpcMsg;

public final class MintMonoHandler implements Handler {
    public MintMonoHandler() {

    }

    @Override
    public void onConnected(Channel channel) {

    }

    @Override
    public void onRecv(Channel channel, Object data) {
        if(data instanceof RpcMsg rpcMsg) {

        }else {
            throw new FrameworkException(ExceptionType.MINT, Constants.UNREACHED);
        }
    }

    @Override
    public void onShutdown(Channel channel) {

    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
