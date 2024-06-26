package cn.zorcc.common.bindings;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.List;

/**
 *   Class that interact with underlying SSL library, currently only openssl and libressl are tested
 *   It's recommended to use openssl in Windows and Linux operating system, and use libressl in macOS operating system
 *   Other SSL library or other operating system were not tested, you are welcome to test it on your own
 */
public final class SslBinding {
    private static final SymbolLookup crypto;
    private static final SymbolLookup ssl;
    private static final MethodHandle tlsMethod;
    private static final MethodHandle sslCtxNewMethod;
    private static final MethodHandle sslCtxUseCertificateFileMethod;
    private static final MethodHandle sslCtxUsePrivateKeyFileMethod;
    private static final MethodHandle sslCtxCheckPrivateKeyMethod;
    private static final MethodHandle sslCtxCtrlMethod;
    private static final MethodHandle sslCtxGetOptions;
    private static final MethodHandle sslCtxSetOptions;
    private static final MethodHandle sslCtxSetVerifyMethod;
    private static final MethodHandle sslCtxSetDefaultVerifyPath;
    private static final MethodHandle sslCtxLoadVerifyLocations;
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
    private static final MethodHandle sslGetVerifyResult;
    private static final MethodHandle sslGetPeerCertificate;
    private static final MethodHandle x509Free;
    private static final MethodHandle errGet;
    private static final MethodHandle errString;

    static {
        crypto = NativeUtil.loadLibrary(Constants.CRYPTO);
        ssl = NativeUtil.loadLibrary(Constants.SSL);
        tlsMethod = NativeUtil.methodHandle(ssl, "TLS_method",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        sslCtxNewMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslCtxUseCertificateFileMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_use_certificate_file",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslCtxUsePrivateKeyFileMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_use_PrivateKey_file",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslCtxCheckPrivateKeyMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_check_private_key",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxCtrlMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_ctrl",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        sslCtxGetOptions = NativeUtil.methodHandle(ssl, "SSL_CTX_get_options",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        sslCtxSetOptions = NativeUtil.methodHandle(ssl, "SSL_CTX_set_options",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        sslCtxSetVerifyMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_set_verify",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxSetDefaultVerifyPath = NativeUtil.methodHandle(ssl, "SSL_CTX_set_default_verify_paths",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxLoadVerifyLocations = NativeUtil.methodHandle(ssl, "SSL_CTX_load_verify_locations",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslNewMethod = NativeUtil.methodHandle(ssl, "SSL_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslSetFdMethod = NativeUtil.methodHandle(ssl, "SSL_set_fd",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslConnectMethod = NativeUtil.methodHandle(ssl, "SSL_connect",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), Linker.Option.critical(false));
        sslAcceptMethod = NativeUtil.methodHandle(ssl, "SSL_accept",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), Linker.Option.critical(false));
        sslReadMethod = NativeUtil.methodHandle(ssl, "SSL_read",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.critical(false));
        sslWriteMethod = NativeUtil.methodHandle(ssl, "SSL_write",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.critical(true));
        sslShutdownMethod = NativeUtil.methodHandle(ssl, "SSL_shutdown",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), Linker.Option.critical(false));
        sslFreeMethod = NativeUtil.methodHandle(ssl, "SSL_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        sslCtxFreeMethod = NativeUtil.methodHandle(ssl, "SSL_CTX_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        sslGetErrMethod = NativeUtil.methodHandle(ssl, "SSL_get_error",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.critical(false));
        sslGetVerifyResult = NativeUtil.methodHandle(ssl, "SSL_get_verify_result",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        sslGetPeerCertificate = NativeUtil.methodHandle(ssl, List.of("SSL_get_peer_certificate", "SSL_get1_peer_certificate"),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        x509Free = NativeUtil.methodHandle(crypto, "X509_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        errGet = NativeUtil.methodHandle(crypto, "ERR_get_error",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        errString = NativeUtil.methodHandle(crypto, "ERR_error_string_n",
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
    }

    private SslBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment tlsMethod() {
        try{
            return (MemorySegment) tlsMethod.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment sslCtxNew(MemorySegment tlsMethod) {
        try{
            return (MemorySegment) sslCtxNewMethod.invokeExact(tlsMethod);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setPublicKey(MemorySegment ctx, MemorySegment file, int type) {
        try{
            return (int) sslCtxUseCertificateFileMethod.invokeExact(ctx, file, type);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }
    public static int setPrivateKey(MemorySegment ctx, MemorySegment file, int type) {
        try{
            return (int) sslCtxUsePrivateKeyFileMethod.invokeExact(ctx, file, type);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int checkPrivateKey(MemorySegment ctx) {
        try{
            return (int) sslCtxCheckPrivateKeyMethod.invokeExact(ctx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long ctxCtrl(MemorySegment ctx, int cmd, long mode, MemorySegment ptr) {
        try{
            return (long) sslCtxCtrlMethod.invokeExact(ctx, cmd, mode, ptr);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long ctxGetOptions(MemorySegment ctx) {
        try{
            return (long) sslCtxGetOptions.invokeExact(ctx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long ctxSetOptions(MemorySegment ctx, long option) {
        try{
            return (long) sslCtxSetOptions.invokeExact(ctx, option);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void setVerify(MemorySegment ctx, int mode, MemorySegment callback) {
        try{
            sslCtxSetVerifyMethod.invokeExact(ctx, mode, callback);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int setDefaultVerifyPath(MemorySegment ctx) {
        try{
            return (int) sslCtxSetDefaultVerifyPath.invokeExact(ctx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int loadVerifyLocations(MemorySegment ctx, MemorySegment caFile, MemorySegment caPath) {
        try{
            return (int) sslCtxLoadVerifyLocations.invokeExact(ctx, caFile, caPath);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment sslNew(MemorySegment ctx) {
        try{
            return (MemorySegment) sslNewMethod.invokeExact(ctx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslSetFd(MemorySegment ssl, int fd) {
        try{
            return (int) sslSetFdMethod.invokeExact(ssl, fd);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslConnect(MemorySegment ssl) {
        try{
            return (int) sslConnectMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslAccept(MemorySegment ssl) {
        try{
            return (int) sslAcceptMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslRead(MemorySegment ssl, MemorySegment buf, int len) {
        try{
            return (int) sslReadMethod.invokeExact(ssl, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslWrite(MemorySegment ssl, MemorySegment buf, int len) {
        try{
            return (int) sslWriteMethod.invokeExact(ssl, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslShutdown(MemorySegment ssl) {
        try{
            return (int) sslShutdownMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void sslFree(MemorySegment ssl) {
        try{
            sslFreeMethod.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void sslCtxFree(MemorySegment sslCtx) {
        try{
            sslCtxFreeMethod.invokeExact(sslCtx);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static int sslGetErr(MemorySegment ssl, int ret) {
        try{
            return (int) sslGetErrMethod.invokeExact(ssl, ret);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long sslGetVerifyResult(MemorySegment ssl) {
        try{
            return (long) sslGetVerifyResult.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static MemorySegment sslGetPeerCertificate(MemorySegment ssl) {
        try{
            return (MemorySegment) sslGetPeerCertificate.invokeExact(ssl);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void x509Free(MemorySegment x509) {
        try{
            x509Free.invokeExact(x509);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static long errGet() {
        try{
            return (long) errGet.invokeExact();
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }

    public static void errString(long err, MemorySegment buf, long len) {
        try{
            errString.invokeExact(err, buf, len);
        }catch (Throwable throwable) {
            throw new FrameworkException(ExceptionType.NATIVE, Constants.UNREACHED, throwable);
        }
    }
}
