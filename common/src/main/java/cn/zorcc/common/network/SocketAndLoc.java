package cn.zorcc.common.network;

/**
 *   Used as message for accept() return
 */
public record SocketAndLoc(
        Socket socket,
        Loc loc
) {
}
