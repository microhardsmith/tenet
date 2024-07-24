package cc.zorcc.tenet.core.bindings;

import cc.zorcc.tenet.core.Constants;
import cc.zorcc.tenet.core.Dyn;
import cc.zorcc.tenet.core.ExceptionType;
import cc.zorcc.tenet.core.TenetException;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.List;

/**
 *   Adding support for interaction with underlying SSL library, currently only openssl and libressl have been tested
 *   It's recommended to use openssl in Windows and Linux operating system, and use libressl in macOS operating system
 *   Other SSL library or other operating system were not tested, and developers are welcome to test it on your own
 */
public final class SslBinding {
    private static final MethodHandle tlsMethod;
    private static final MethodHandle sslCtxNewMethod;
    private static final MethodHandle sslCtxUseCertificateFileMethod;
    private static final MethodHandle sslCtxUsePrivateKeyFileMethod;
    private static final MethodHandle sslCtxCheckPrivateKeyMethod;
    private static final MethodHandle sslCtxCtrlMethod;
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
        SymbolLookup crypto = Dyn.loadDynLibrary("libcrypto");
        SymbolLookup ssl = Dyn.loadDynLibrary("libssl");
        tlsMethod = Dyn.mh(ssl, "TLS_method",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        sslCtxNewMethod = Dyn.mh(ssl, "SSL_CTX_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslCtxUseCertificateFileMethod = Dyn.mh(ssl, "SSL_CTX_use_certificate_file",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslCtxUsePrivateKeyFileMethod = Dyn.mh(ssl, "SSL_CTX_use_PrivateKey_file",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslCtxCheckPrivateKeyMethod = Dyn.mh(ssl, "SSL_CTX_check_private_key",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxCtrlMethod = Dyn.mh(ssl, "SSL_CTX_ctrl",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        sslCtxSetVerifyMethod = Dyn.mh(ssl, "SSL_CTX_set_verify",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxSetDefaultVerifyPath = Dyn.mh(ssl, "SSL_CTX_set_default_verify_paths",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        sslCtxLoadVerifyLocations = Dyn.mh(ssl, "SSL_CTX_load_verify_locations",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslNewMethod = Dyn.mh(ssl, "SSL_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        sslSetFdMethod = Dyn.mh(ssl, "SSL_set_fd",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        sslConnectMethod = Dyn.mh(ssl, "SSL_connect",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), Linker.Option.critical(false));
        sslAcceptMethod = Dyn.mh(ssl, "SSL_accept",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), Linker.Option.critical(false));
        sslReadMethod = Dyn.mh(ssl, "SSL_read",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.critical(false));
        sslWriteMethod = Dyn.mh(ssl, "SSL_write",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.critical(true));
        sslShutdownMethod = Dyn.mh(ssl, "SSL_shutdown",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), Linker.Option.critical(false));
        sslFreeMethod = Dyn.mh(ssl, "SSL_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        sslCtxFreeMethod = Dyn.mh(ssl, "SSL_CTX_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        sslGetErrMethod = Dyn.mh(ssl, "SSL_get_error",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT), Linker.Option.critical(false));
        sslGetVerifyResult = Dyn.mh(ssl, "SSL_get_verify_result",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        sslGetPeerCertificate = Dyn.mh(ssl, List.of("SSL_get_peer_certificate", "SSL_get1_peer_certificate"),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        x509Free = Dyn.mh(crypto, "X509_free",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        errGet = Dyn.mh(crypto, "ERR_get_error",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG));
        errString = Dyn.mh(crypto, "ERR_error_string_n",
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Linker.Option.critical(true));
    }

    /**
     *   SslBinding shouldn't be initialized
     */
    private SslBinding() {
        throw new UnsupportedOperationException();
    }

    public static MemorySegment tlsMethod() {
        try{
            return (MemorySegment) tlsMethod.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment sslCtxNew(MemorySegment tlsMethod) {
        try{
            return (MemorySegment) sslCtxNewMethod.invokeExact(tlsMethod);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int setPublicKey(MemorySegment ctx, MemorySegment file, int type) {
        try{
            return (int) sslCtxUseCertificateFileMethod.invokeExact(ctx, file, type);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
    public static int setPrivateKey(MemorySegment ctx, MemorySegment file, int type) {
        try{
            return (int) sslCtxUsePrivateKeyFileMethod.invokeExact(ctx, file, type);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int checkPrivateKey(MemorySegment ctx) {
        try{
            return (int) sslCtxCheckPrivateKeyMethod.invokeExact(ctx);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long ctxCtrl(MemorySegment ctx, int cmd, long mode, MemorySegment ptr) {
        try{
            return (long) sslCtxCtrlMethod.invokeExact(ctx, cmd, mode, ptr);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void setVerify(MemorySegment ctx, int mode, MemorySegment callback) {
        try{
            sslCtxSetVerifyMethod.invokeExact(ctx, mode, callback);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int setDefaultVerifyPath(MemorySegment ctx) {
        try{
            return (int) sslCtxSetDefaultVerifyPath.invokeExact(ctx);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int loadVerifyLocations(MemorySegment ctx, MemorySegment caFile, MemorySegment caPath) {
        try{
            return (int) sslCtxLoadVerifyLocations.invokeExact(ctx, caFile, caPath);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment sslNew(MemorySegment ctx) {
        try{
            return (MemorySegment) sslNewMethod.invokeExact(ctx);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslSetFd(MemorySegment ssl, int fd) {
        try{
            return (int) sslSetFdMethod.invokeExact(ssl, fd);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslConnect(MemorySegment ssl) {
        try{
            return (int) sslConnectMethod.invokeExact(ssl);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslAccept(MemorySegment ssl) {
        try{
            return (int) sslAcceptMethod.invokeExact(ssl);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslRead(MemorySegment ssl, MemorySegment buf, int len) {
        try{
            return (int) sslReadMethod.invokeExact(ssl, buf, len);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslWrite(MemorySegment ssl, MemorySegment buf, int len) {
        try{
            return (int) sslWriteMethod.invokeExact(ssl, buf, len);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslShutdown(MemorySegment ssl) {
        try{
            return (int) sslShutdownMethod.invokeExact(ssl);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void sslFree(MemorySegment ssl) {
        try{
            sslFreeMethod.invokeExact(ssl);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void sslCtxFree(MemorySegment sslCtx) {
        try{
            sslCtxFreeMethod.invokeExact(sslCtx);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static int sslGetErr(MemorySegment ssl, int ret) {
        try{
            return (int) sslGetErrMethod.invokeExact(ssl, ret);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long sslGetVerifyResult(MemorySegment ssl) {
        try{
            return (long) sslGetVerifyResult.invokeExact(ssl);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static MemorySegment sslGetPeerCertificate(MemorySegment ssl) {
        try{
            return (MemorySegment) sslGetPeerCertificate.invokeExact(ssl);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void x509Free(MemorySegment x509) {
        try{
            x509Free.invokeExact(x509);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static long errGet() {
        try{
            return (long) errGet.invokeExact();
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }

    public static void errString(long err, MemorySegment buf, long len) {
        try{
            errString.invokeExact(err, buf, len);
        }catch (Throwable e) {
            throw new TenetException(ExceptionType.NATIVE, Constants.UNREACHED, e);
        }
    }
}
