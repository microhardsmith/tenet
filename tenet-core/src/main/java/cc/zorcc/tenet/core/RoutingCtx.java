package cc.zorcc.tenet.core;

import java.lang.foreign.MemorySegment;

/**
 *   Representing an RPC message with traceId and spanId information
 */
public record RoutingCtx(
        MemorySegment trace,
        MemorySegment span
) implements Ctx {
}
