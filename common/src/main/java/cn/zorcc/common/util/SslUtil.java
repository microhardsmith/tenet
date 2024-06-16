package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.bindings.SslBinding;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.structure.Allocator;
import cn.zorcc.common.structure.MemApi;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

/**
 *   Helper class with OPENSSL library
 */
public final class SslUtil {
    /**
     *   OPENSSL err description requires a string must be as least 256 bytes, thus we will be using a KB
     */
    private static final int ERR_STRING_LENGTH = Constants.KB;
    /**
     *   These two variable are hard-coded definition from ssl.h, since SSL_CTX_set_mode and SSL_CTX_clear_mode are macros, we can't directly use it
     */
    private static final int SSL_CTRL_MODE = 33;
    private static final int SSL_CTRL_CLEAR_MODE = 78;
    /**
     *   Some default SSL_CTX options
     */
    private static final long SSL_OP_DISABLE_TLSEXT_CA_NAMES = 1L << 9;
    private static final long SSL_OP_ENABLE_KTLS = 1 << 3;


    private SslUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     *   configure CTX for non-blocking socket with several preassigned options
     */
    public static void configureCtx(MemorySegment ctx) {
        // overwriting CTX mode
        if((SslBinding.ctxCtrl(ctx, SSL_CTRL_MODE, Constants.SSL_MODE_ENABLE_PARTIAL_WRITE, MemorySegment.NULL) & Constants.SSL_MODE_ENABLE_PARTIAL_WRITE) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        if((SslBinding.ctxCtrl(ctx, SSL_CTRL_MODE, Constants.SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER, MemorySegment.NULL) & Constants.SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        if(((~SslBinding.ctxCtrl(ctx, SSL_CTRL_CLEAR_MODE, Constants.SSL_MODE_AUTO_RETRY, MemorySegment.NULL)) & Constants.SSL_MODE_AUTO_RETRY) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
        // overwriting CTX options
        long currentOptions = SslBinding.ctxGetOptions(ctx);
        long newOptions = currentOptions | SSL_OP_DISABLE_TLSEXT_CA_NAMES | SSL_OP_ENABLE_KTLS;
        if(currentOptions != newOptions && SslBinding.ctxSetOptions(ctx, newOptions) != newOptions) {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static String getErrDescription(MemApi memApi) {
        try(Allocator allocator = Allocator.newDirectAllocator(memApi)) {
            MemorySegment buf = allocator.allocate(ERR_STRING_LENGTH);
            SslBinding.errString(SslBinding.errGet(), buf, buf.byteSize());
            return buf.getString(0L, StandardCharsets.UTF_8);
        }
    }

    /**
     *   Throw an exception based on err code and operation
     */
    public static int throwException(int err, String operation, MemApi memApi) {
        if(err == Constants.SSL_ERROR_SSL){
            throw new FrameworkException(ExceptionType.NETWORK, STR."\{operation} failed with SSL_ERROR_SSL, message : \{getErrDescription(memApi)}");
        }else if(err == Constants.SSL_ERROR_SYSCALL) {
            throw new FrameworkException(ExceptionType.NETWORK, STR."\{operation} failed with SSL_ERROR_SYSCALL, message : \{getErrDescription(memApi)}");
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }
}
