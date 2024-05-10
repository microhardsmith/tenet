package cn.zorcc.common.network;

/**
 *   MuxEvent is a data representation for MuxWait result
 *   TODO value-based record
 */
public record MuxEvent(
        int socket,
        int event
) {
}
