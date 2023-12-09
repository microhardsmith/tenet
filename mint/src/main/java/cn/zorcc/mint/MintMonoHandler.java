package cn.zorcc.mint;

import cn.zorcc.common.network.TaggedResult;
import cn.zorcc.common.network.api.Channel;
import cn.zorcc.common.network.api.Handler;

public final class MintMonoHandler implements Handler {
    @Override
    public void onConnected(Channel channel) {

    }

    @Override
    public TaggedResult onRecv(Channel channel, Object data) {
        return null;
    }

    @Override
    public void onShutdown(Channel channel) {

    }

    @Override
    public void onRemoved(Channel channel) {

    }
}
