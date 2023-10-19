package cn.zorcc.common.sqlite;

public final class SqliteConfig {
    /**
     *   The absolute path of the sqlite db files
     */
    private String path;
    /**
     *   The number of the reader threads
     */
    private int readers;
    /**
     *   Whether or not using WAL mechanism
     */
    private boolean usingWAL;
    /**
     *  Whether or not enabling remote discovery
     */
    private boolean enableDiscovery;
    /**
     *  Whether or not enabling remote configuration
     */
    private boolean enableConfiguration;
    /**
     *  Whether or not enabling remote distributed lock
     */
    private boolean enableDistributedLock;

    public String getPath() {
        return path;
    }

    public SqliteConfig setPath(String path) {
        this.path = path;
        return this;
    }

    public int getReaders() {
        return readers;
    }

    public SqliteConfig setReaders(int readers) {
        this.readers = readers;
        return this;
    }

    public boolean isUsingWAL() {
        return usingWAL;
    }

    public SqliteConfig setUsingWAL(boolean usingWAL) {
        this.usingWAL = usingWAL;
        return this;
    }

    public boolean isEnableDiscovery() {
        return enableDiscovery;
    }

    public SqliteConfig setEnableDiscovery(boolean enableDiscovery) {
        this.enableDiscovery = enableDiscovery;
        return this;
    }

    public boolean isEnableConfiguration() {
        return enableConfiguration;
    }

    public SqliteConfig setEnableConfiguration(boolean enableConfiguration) {
        this.enableConfiguration = enableConfiguration;
        return this;
    }

    public boolean isEnableDistributedLock() {
        return enableDistributedLock;
    }

    public SqliteConfig setEnableDistributedLock(boolean enableDistributedLock) {
        this.enableDistributedLock = enableDistributedLock;
        return this;
    }
}
