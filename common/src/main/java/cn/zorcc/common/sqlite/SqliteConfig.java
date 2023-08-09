package cn.zorcc.common.sqlite;

public class SqliteConfig {
    /**
     * The absolute path of the Sqlite database file
     */
    private String path;
    /**
     * Whether or not using WAL mechanism, if using cluster-mode, WAL is encouraged to be enabled
     */
    private Boolean enableWAL;

    public SqliteConfig() {
    }

    public String getPath() {
        return this.path;
    }

    public Boolean getEnableWAL() {
        return this.enableWAL;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setEnableWAL(Boolean enableWAL) {
        this.enableWAL = enableWAL;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SqliteConfig)) return false;
        final SqliteConfig other = (SqliteConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$path = this.getPath();
        final Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final Object this$enableWAL = this.getEnableWAL();
        final Object other$enableWAL = other.getEnableWAL();
        if (this$enableWAL == null ? other$enableWAL != null : !this$enableWAL.equals(other$enableWAL)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SqliteConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final Object $enableWAL = this.getEnableWAL();
        result = result * PRIME + ($enableWAL == null ? 43 : $enableWAL.hashCode());
        return result;
    }

    public String toString() {
        return "SqliteConfig(path=" + this.getPath() + ", enableWAL=" + this.getEnableWAL() + ")";
    }
}
