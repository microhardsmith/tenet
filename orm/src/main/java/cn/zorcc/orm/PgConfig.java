package cn.zorcc.orm;

import cn.zorcc.common.network.IpType;
import cn.zorcc.common.network.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.orm.core.PgConstants;

public final class PgConfig {
    private static final Loc DEFAULT_LOC = new Loc(IpType.IPV4, "127.0.0.1", 5432);
    /**
     * 数据库地址
     */
    private Loc loc = DEFAULT_LOC;
    /**
     * 数据库用户名
     */
    private String username = "postgres";
    /**
     * 数据库密码
     */
    private String password = "";
    /**
     * 数据库名
     */
    private String databaseName = "postgres";
    /**
     * 数据库模式
     */
    private String currentSchema = "public";
    /**
     * 数据库连接池数量,建议设置数: (核心数 * 2) + 有效硬盘数
     */
    private Integer maximumIdle = NativeUtil.getCpuCores() * 2 + 4;
    /**
     * postgresql连接加密模式,默认为preferred,可配置为verify-ca（验证服务端证书）或verify-full（验证服务端证书与hostname）
     */
    private String sslMode = PgConstants.SSL_PREFERRED;
    /**
     * 数据库空闲连接存活最大时间,单位毫秒,当使用固定大小的连接池时,设置该参数无意义
     */
    private Long idleTimeout = 300000L;
    /**
     * 数据库连接最长生命周期,单位毫秒
     */
    private Long maxLifetime = 1800000L;
    /**
     * 线程等待获取数据库连接的最大时间,单位毫秒,超出会抛出SQLException
     */
    private Long waitingTimeout = 30000L;
    /**
     * 数据库建立连接超时时间,单位毫秒
     */
    private Integer acquireTimeout = 3000;
    /**
     * 单次sql解析超时时间,单位毫秒
     */
    private Integer parseTimeout = 1000;
    /**
     * 单次sql执行超时时间,单位毫秒
     */
    private Integer executionTimeout = 4000;

    private Long shutdownTimeout = 3000L;

    public PgConfig() {
    }

    public Loc getLoc() {
        return this.loc;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public String getCurrentSchema() {
        return this.currentSchema;
    }

    public Integer getMaximumIdle() {
        return this.maximumIdle;
    }

    public String getSslMode() {
        return this.sslMode;
    }

    public Long getIdleTimeout() {
        return this.idleTimeout;
    }

    public Long getMaxLifetime() {
        return this.maxLifetime;
    }

    public Long getWaitingTimeout() {
        return this.waitingTimeout;
    }

    public Integer getAcquireTimeout() {
        return this.acquireTimeout;
    }

    public Integer getParseTimeout() {
        return this.parseTimeout;
    }

    public Integer getExecutionTimeout() {
        return this.executionTimeout;
    }

    public Long getShutdownTimeout() {
        return this.shutdownTimeout;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setCurrentSchema(String currentSchema) {
        this.currentSchema = currentSchema;
    }

    public void setMaximumIdle(Integer maximumIdle) {
        this.maximumIdle = maximumIdle;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public void setWaitingTimeout(Long waitingTimeout) {
        this.waitingTimeout = waitingTimeout;
    }

    public void setAcquireTimeout(Integer acquireTimeout) {
        this.acquireTimeout = acquireTimeout;
    }

    public void setParseTimeout(Integer parseTimeout) {
        this.parseTimeout = parseTimeout;
    }

    public void setExecutionTimeout(Integer executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public void setShutdownTimeout(Long shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }
}
