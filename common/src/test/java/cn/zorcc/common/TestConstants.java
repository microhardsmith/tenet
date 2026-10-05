package cn.zorcc.common;

import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.IpType;
import cn.zorcc.common.network.Loc;
import cn.zorcc.common.util.NativeUtil;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestConstants {
    /**
     *   Certificates ship with the test resources so the suite never depends on /tmp or on a developer's home directory.
     *   SslBinding reads these through a real file path, so the classpath resource is resolved and copied out to a
     *   temporary directory once per JVM. Declared first because the key path fields below depend on it.
     */
    private static final Path CA_DIR_PATH = extractCaDir();

    /**
     *   Ports are deliberately kept in the unprivileged range (>=1024) so the suite can run as a normal user.
     *   Overridable so a developer can move a listener if 18080/18443 is already occupied locally.
     */
    public static final int PORT = port();
    public static final int HTTP_PORT = httpPort();
    public static final int HTTPS_PORT = httpsPort();
    public static final Loc SERVER_IPV4_LOC = new Loc(IpType.IPV4, PORT);
    public static final Loc SERVER_IPV6_LOC = new Loc(IpType.IPV6, PORT);
    public static final Loc CLIENT_IPV4_LOC = new Loc(IpType.IPV4, "127.0.0.1", PORT);
    public static final Loc CLIENT_IPV6_LOC = new Loc(IpType.IPV6, "::1", PORT);
    public static final Loc HTTP_LOC = new Loc(IpType.IPV4, "127.0.0.1", HTTP_PORT);
    public static final Loc HTTPS_LOC = new Loc(IpType.IPV4, "127.0.0.1", HTTPS_PORT);
    public static final String SELF_PUBLIC_KEY_FILE = selfPublicKeyFile();
    public static final String SELF_PRIVATE_KEY_FILE = selfPrivateKeyFile();
    public static final String SERVER_PUBLIC_KEY_FILE = serverPublicKeyFile();
    public static final String SERVER_PRIVATE_KEY_FILE = serverPrivateKeyFile();

    /**
     *   Rerunning program in the same operating system would be fine, but wsl would share the same port between Windows and Linux, so we might want to differ them
     */
    private static int port() {
        return intProperty("tenet.test.port", defaultPort());
    }

    private static int httpPort() {
        return intProperty("tenet.test.http.port", 18080);
    }

    private static int httpsPort() {
        return intProperty("tenet.test.https.port", 18443);
    }

    private static int defaultPort() {
        OsType ostype = NativeUtil.ostype();
        if(ostype == OsType.Windows) {
            return 8001;
        }else if(ostype == OsType.MacOS) {
            return 8002;
        }else if(ostype == OsType.Linux) {
            return 8003;
        }else {
            throw new FrameworkException(ExceptionType.NETWORK, Constants.UNREACHED);
        }
    }

    private static int intProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if(value == null || value.isBlank()) {
            return defaultValue;
        }
        try{
            int parsed = Integer.parseInt(value.trim());
            if(parsed < 1024 || parsed > 65535) {
                throw new FrameworkException(ExceptionType.NETWORK, "Test port must be in the unprivileged range 1024-65535 : " + parsed);
            }
            return parsed;
        }catch (NumberFormatException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Illegal test port property : " + key + "=" + value, e);
        }
    }

    private static String selfPublicKeyFile() {
        return copyCaResource("localhost.crt", "self.crt");
    }

    private static String selfPrivateKeyFile() {
        return copyCaResource("localhost.key", "self.key");
    }

    private static String serverPublicKeyFile() {
        return copyCaResource("localhost.crt", "server.crt");
    }

    private static String serverPrivateKeyFile() {
        return copyCaResource("localhost.key", "server.key");
    }

    private static Path extractCaDir() {
        try{
            return Path.of(TestConstants.class.getResource("/ca/").toURI());
        }catch (URISyntaxException | NullPointerException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Test CA resources are missing from the classpath", e);
        }
    }

    private static String copyCaResource(String resourceName, String targetName) {
        Path source = CA_DIR_PATH.resolve(resourceName);
        if(!Files.exists(source)) {
            throw new FrameworkException(ExceptionType.NETWORK, "Test CA resource is missing : " + source);
        }
        try{
            Path targetDir = Files.createTempDirectory("tenet-test-ca");
            targetDir.toFile().deleteOnExit();
            Path target = targetDir.resolve(targetName);
            target.toFile().deleteOnExit();
            Files.copy(source, target);
            return target.toAbsolutePath().toString();
        }catch (java.io.IOException e) {
            throw new FrameworkException(ExceptionType.NETWORK, "Can't stage test CA resource : " + source, e);
        }
    }
}
