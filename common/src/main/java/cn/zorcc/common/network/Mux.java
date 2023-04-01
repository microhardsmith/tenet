package cn.zorcc.common.network;

import java.lang.foreign.MemorySegment;

/**
 *   multiplexing fd
 */
public record Mux (
        MemorySegment winHandle,
        int epFd,
        int kqFd
){
    public static Mux win(MemorySegment winHandle) {
        return new Mux(winHandle, -1, -1);
    }

    public static Mux linux(int epfd) {
        return new Mux(null, epfd, -1);
    }

    public static Mux mac(int kq) {
        return new Mux(null, -1, kq);
    }
}
