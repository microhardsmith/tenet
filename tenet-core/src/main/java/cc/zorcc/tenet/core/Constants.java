package cc.zorcc.tenet.core;

public final class Constants {
    public static final String UNREACHED = "Should never be reached";

    public static final String TENET_LIBRARY_PATH = "TENET_LIBRARY_PATH";

    /**
     *   State control
     */
    public static final int INITIAL = 0;
    public static final int RUNNING = 1;
    public static final int CLOSING = 2;
    public static final int STOPPED = 3;

    /**
     *  General data-size parameters
     */
    public static final int KB = 1024;
    public static final int MB = 1024 * KB;
    public static final int GB = 1024 * MB;

    /**
     *   Dyn library names
     */
    public static final String BROTLI_COMMON = "libbrotlicommon";
    public static final String BROTLI_ENC = "libbrotlienc";
    public static final String BROTLI_DEC = "libbrotlidec";

    public static final String DEFLATE = "libdeflate";

    public static final String ZSTD = "libzstd";
}
