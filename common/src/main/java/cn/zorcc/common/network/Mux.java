package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.MemorySegment;

/**
 *   multiplexing fd
 */
public record Mux (
        MemorySegment winHandle,
        int epfd,
        int kqfd
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

    @Override
    public String toString() {
        if(winHandle != null) {
            return String.valueOf(winHandle.address());
        }else if(epfd != -1) {
            return String.valueOf(epfd);
        }else if(kqfd != -1) {
            return String.valueOf(kqfd);
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
