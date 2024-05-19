package cc.zorcc.core;

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
}
