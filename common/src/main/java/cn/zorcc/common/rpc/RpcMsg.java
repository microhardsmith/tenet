package cn.zorcc.common.rpc;

import java.lang.foreign.MemorySegment;

public record RpcMsg(
        int msgType,
        int len,
        MemorySegment data
) {

}
