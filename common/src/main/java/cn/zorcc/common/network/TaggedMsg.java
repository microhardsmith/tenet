package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

public record TaggedMsg(
        int tag,
        Carrier carrier
) {
    public TaggedMsg {
        if(carrier == null) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    public TaggedMsg(int tag) {
        this(tag, Carrier.create());
    }
}
