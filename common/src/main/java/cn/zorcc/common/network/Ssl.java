package cn.zorcc.common.network;

import cn.zorcc.common.Clock;
import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.TimeUnit;

/**
 *   Class that interact with underlying SSL library, currently only openssl and libressl are tested
 *   It's recommended to use openssl in Windows and Linux, use libressl in macOS
 */
public final class Ssl {
    private static final Logger log = LoggerFactory.getLogger(Ssl.class);
    /**
     *   Environment variable that must be configured when launching the application
     */
    public static final String CRYPTO_LIB = "crypto";
    public static final String SSL_LIB = "ssl";
    @SuppressWarnings("unused")
    private static final SymbolLookup crypto;
    private static final SymbolLookup ssl;
    /**
     *   These two variable are hard-coded definition from ssl.h, since SSL_CTX_set_mode and SSL_CTX_clear_mode are macros, we can't directly use it
     */
    private static final int SSL_CTRL_MODE = 33;
    private static final int SSL_CTRL_CLEAR_MODE = 78;
    private static final MethodHandle tlsMethod;
    private static final MethodHandle sslCtxNewMethod;
    private static final MethodHandle sslCtxUseCertificateFileMethod;
    private static final MethodHandle sslCtxUsePrivateKeyFileMethod;
    private static final MethodHandle sslCtxCheckPrivateKeyMethod;
    private static final MethodHandle sslCtxCtrlMethod;
    private static final MethodHandle sslCtxSetVerifyMethod;
    private static final MethodHandle sslNewMethod;
    private static final MethodHandle sslSetFdMethod;
    private static final MethodHandle sslConnectMethod;
    private static final MethodHandle sslAcceptMethod;
    private static final MethodHandle sslReadMethod;
    private static final MethodHandle sslWriteMethod;
    private static final MethodHandle sslShutdownMethod;
    private static final MethodHandle sslFreeMethod;
    private static final MethodHandle sslCtxFreeMethod;
    private static final MethodHandle sslGetErrMethod;

    static {
        long nano = Clock.nano();
        crypto = NativeUtil.loadLibrary(CRYPTO_LIB);
        if(crypto == null) {
            throw new FrameworkException(ExceptionType.NETWORK, "Crypto lib not found");
        }
        ssl = NativeUtil.loadLibrary(SSL_LIB);
        if(ssl == null) {
            throw new FrameworkException(ExceptionType.NETWORK, "Ssl lib not found");
        }
        tlsMethod = NativeUtil.methodHandle(ssl, "TLS_method", FunctionDescriptor.of(ValueLayout.ADDRESS));
        sslCtxNewMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_new", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslCtxUseCertificateFileMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_use_certificate_file", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslCtxUsePrivateKeyFileMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_use_PrivateKey_file", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslCtxCheckPrivateKeyMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_check_private_key", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxCtrlMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_ctrl", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        sslCtxSetVerifyMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_set_verify", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslNewMethod = NativeUtil.methodHandle(ssl, "SSL_new", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslSetFdMethod = NativeUtil.methodHandle(ssl, "SSL_set_fd", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslConnectMethod = NativeUtil.methodHandle(ssl, "SSL_connect", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslAcceptMethod = NativeUtil.methodHandle(ssl, "SSL_accept", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslReadMethod = NativeUtil.methodHandle(ssl, "SSL_read", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslWriteMethod = NativeUtil.methodHandle(ssl, "SSL_write", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslShutdownMethod = NativeUtil.methodHandle(ssl, "SSL_shutdown", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslFreeMethod = NativeUtil.methodHandle(ssl, "SSL_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        sslCtxFreeMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        sslGetErrMethod = NativeUtil.methodHandle(ssl, "SSL_get_error", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        log.info("Initializing SSL library successfully, time consuming : {} ms", TimeUnit.NANOSECONDS.toMillis(Clock.elapsed(nano)));
    }

    private Ssl() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment tlsMethod() {
        try{
            return (MemorySegment) tlsMethod.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking tlsMethod()", throwable);
        }
    }

    public static MemorySegment sslCtxNew(MemorySegment tlsMethod) {
        try{
            return (MemorySegment) sslCtxNewMethod.invokeExact(tlsMethod);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslCtxNew()", throwable);
        }
    }

    public static int setPublicKey(MemorySegment ctx, MemorySegment file, int type) {
        try{
            return (int) sslCtxUseCertificateFileMethod.invokeExact(ctx, file, type);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setPublicKey()", throwable);
        }
    }
    public static int setPrivateKey(MemorySegment ctx, MemorySegment file, int type) {
        try{
            return (int) sslCtxUsePrivateKeyFileMethod.invokeExact(ctx, file, type);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking setPrivateKey()", throwable);
        }
    }

    public static int checkPrivateKey(MemorySegment ctx) {
        try{
            return (int) sslCtxCheckPrivateKeyMethod.invokeExact(ctx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking checkPrivateKey()", throwable);
        }
    }

    public static long ctxCtrl(MemorySegment ctx, int cmd, long mode, MemorySegment ptr) {
        try{
            return (long) sslCtxCtrlMethod.invokeExact(ctx, cmd, mode, ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslCtxSetMode()", throwable);
        }
    }

    public static void setVerify(MemorySegment ctx, int mode, MemorySegment callback) {
        try{
            sslCtxSetVerifyMethod.invokeExact(ctx, mode, callback);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslCtxSetVerify()", throwable);
        }
    }

    public static MemorySegment sslNew(MemorySegment ctx) {
        try{
            return (MemorySegment) sslNewMethod.invokeExact(ctx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslNew()", throwable);
        }
    }

    public static int sslSetFd(MemorySegment ssl, int fd) {
        try{
            return (int) sslSetFdMethod.invokeExact(ssl, fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslSetFd()", throwable);
        }
    }

    public static int sslConnect(MemorySegment ssl) {
        try{
            return (int) sslConnectMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslConnect()", throwable);
        }
    }

    public static int sslAccept(MemorySegment ssl) {
        try{
            return (int) sslAcceptMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslAccept()", throwable);
        }
    }

    public static int sslRead(MemorySegment ssl, MemorySegment buf, int len) {
        try{
            return (int) sslReadMethod.invokeExact(ssl, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslRead()", throwable);
        }
    }

    public static int sslWrite(MemorySegment ssl, MemorySegment buf, int len) {
        try{
            return (int) sslWriteMethod.invokeExact(ssl, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslWrite()", throwable);
        }
    }

    public static int sslShutdown(MemorySegment ssl) {
        try{
            return (int) sslShutdownMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslShutdown()", throwable);
        }
    }

    public static void sslFree(MemorySegment ssl) {
        try{
            sslFreeMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslFree()", throwable);
        }
    }

    public static void sslCtxFree(MemorySegment sslCtx) {
        try{
            sslCtxFreeMethod.invokeExact(sslCtx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslCtxFree()", throwable);
        }
    }

    public static int sslGetErr(MemorySegment ssl, int ret) {
        try{
            return (int) sslGetErrMethod.invokeExact(ssl, ret);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, "Exception caught when invoking sslGetErr()", throwable);
        }
    }

    /**
     *   configure CTX for non-blocking socket with several preassigned options
     */
    public static void configureCtx(MemorySegment ctx) {
        if((ctxCtrl(ctx, SSL_CTRL_MODE, Constants.SSL_MODE_ENABLE_PARTIAL_WRITE, NativeUtil.NULL_POINTER) & Constants.SSL_MODE_ENABLE_PARTIAL_WRITE) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "set SSL_MODE_ENABLE_PARTIAL_WRITE failed for openssl");
        }
        if((ctxCtrl(ctx, SSL_CTRL_MODE, Constants.SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER, NativeUtil.NULL_POINTER) & Constants.SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "set SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER failed for openssl");
        }
        if(((~ctxCtrl(ctx, SSL_CTRL_CLEAR_MODE, Constants.SSL_MODE_AUTO_RETRY, NativeUtil.NULL_POINTER)) & Constants.SSL_MODE_AUTO_RETRY) == 0) {
            throw new FrameworkException(ExceptionType.NETWORK, "unset SSL_MODE_AUTO_RETRY failed for openssl");
        }
    }
}
