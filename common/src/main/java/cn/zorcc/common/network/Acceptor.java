package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Loc;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   Network acceptor abstraction
 *   Unlike most application, tenet has divided a connection establish phase into two separate part: Acceptor and Channel
 *   During the acceptor part, reading and writing will be handled by a unique connector instance
 *   Acceptor can evolve into a Channel, then its reading and writing operations will be take over by Tenet default read-write model, or fail and evicted from the worker
 *   Acceptor and Channel will always use the same worker instance, only one would exist in its socketMap
 *   The acceptor must only be accessed in the worker's reader thread, acceptor will never touch worker's writer thread
 */
public record Acceptor (
        Socket socket,
        Encoder encoder,
        Decoder decoder,
        Handler handler,
        Worker worker,
        Loc loc,
        AtomicInteger state
) {

}
