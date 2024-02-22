package cn.zorcc.common.network;

/**
 *   MuxEvent is a data representation for MuxWait result
 */
public record MuxEvent(
        int socket,
        long event
) {
}
