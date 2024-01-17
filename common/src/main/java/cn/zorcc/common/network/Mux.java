package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.MemorySegment;

/**
 *   Multiplexing fd abstraction on different platforms
 *   On Windows, it is a pointer
 *   On Linux or macOS, it is an int fd
 */
public record Mux (
        MemorySegment winHandle,
        int epfd,
        int kqfd
){
    /**
     *   Create a Windows mux
     */
    public static Mux win(MemorySegment winHandle) {
        return new Mux(winHandle, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    /**
     *   Create a Linux mux
     */
    public static Mux linux(int epfd) {
        return new Mux(MemorySegment.NULL, epfd, Integer.MIN_VALUE);
    }

    /**
     *   Create a macOS mux
     */
    public static Mux mac(int kqfd) {
        return new Mux(MemorySegment.NULL, Integer.MIN_VALUE, kqfd);
    }

    @Override
    public String toString() {
        if(winHandle != MemorySegment.NULL) {
            return String.valueOf(winHandle.address());
        }else if(epfd != Integer.MIN_VALUE) {
            return String.valueOf(epfd);
        }else if(kqfd != Integer.MIN_VALUE) {
            return String.valueOf(kqfd);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
