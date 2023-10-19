package cn.zorcc.orm;

import cn.zorcc.common.Constants;
import cn.zorcc.common.structure.IpType;
import cn.zorcc.common.structure.Loc;
import cn.zorcc.common.util.NativeUtil;
import cn.zorcc.orm.core.PgConstants;

public class PgConfig {
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
     * ssl客户端证书文件地址,格式为.crt
     */
    private String sslClientCertChainFile = Constants.EMPTY_STRING;
    /**
     * ssl私钥文件地址,格式为.key
     */
    private String sslClientKeyFile = Constants.EMPTY_STRING;
    /**
     * ssl ca证书文件地址,格式为.crt
     */
    private String sslCaFile = Constants.EMPTY_STRING;
    /**
     * 是否允许启用分布式事务
     */
    private Boolean enableDistributedTransaction = false;
    /**
     * 允许分布式事务使用的最大连接数,该值必须小于maximumIdle
     */
    private Integer maximumDistributedIdle = NativeUtil.getCpuCores();
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

    public String getSslClientCertChainFile() {
        return this.sslClientCertChainFile;
    }

    public String getSslClientKeyFile() {
        return this.sslClientKeyFile;
    }

    public String getSslCaFile() {
        return this.sslCaFile;
    }

    public Boolean getEnableDistributedTransaction() {
        return this.enableDistributedTransaction;
    }

    public Integer getMaximumDistributedIdle() {
        return this.maximumDistributedIdle;
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

    public void setSslClientCertChainFile(String sslClientCertChainFile) {
        this.sslClientCertChainFile = sslClientCertChainFile;
    }

    public void setSslClientKeyFile(String sslClientKeyFile) {
        this.sslClientKeyFile = sslClientKeyFile;
    }

    public void setSslCaFile(String sslCaFile) {
        this.sslCaFile = sslCaFile;
    }

    public void setEnableDistributedTransaction(Boolean enableDistributedTransaction) {
        this.enableDistributedTransaction = enableDistributedTransaction;
    }

    public void setMaximumDistributedIdle(Integer maximumDistributedIdle) {
        this.maximumDistributedIdle = maximumDistributedIdle;
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PgConfig)) return false;
        final PgConfig other = (PgConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$loc = this.getLoc();
        final Object other$loc = other.getLoc();
        if (this$loc == null ? other$loc != null : !this$loc.equals(other$loc)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        final Object this$databaseName = this.getDatabaseName();
        final Object other$databaseName = other.getDatabaseName();
        if (this$databaseName == null ? other$databaseName != null : !this$databaseName.equals(other$databaseName))
            return false;
        final Object this$currentSchema = this.getCurrentSchema();
        final Object other$currentSchema = other.getCurrentSchema();
        if (this$currentSchema == null ? other$currentSchema != null : !this$currentSchema.equals(other$currentSchema))
            return false;
        final Object this$maximumIdle = this.getMaximumIdle();
        final Object other$maximumIdle = other.getMaximumIdle();
        if (this$maximumIdle == null ? other$maximumIdle != null : !this$maximumIdle.equals(other$maximumIdle))
            return false;
        final Object this$sslMode = this.getSslMode();
        final Object other$sslMode = other.getSslMode();
        if (this$sslMode == null ? other$sslMode != null : !this$sslMode.equals(other$sslMode)) return false;
        final Object this$sslClientCertChainFile = this.getSslClientCertChainFile();
        final Object other$sslClientCertChainFile = other.getSslClientCertChainFile();
        if (this$sslClientCertChainFile == null ? other$sslClientCertChainFile != null : !this$sslClientCertChainFile.equals(other$sslClientCertChainFile))
            return false;
        final Object this$sslClientKeyFile = this.getSslClientKeyFile();
        final Object other$sslClientKeyFile = other.getSslClientKeyFile();
        if (this$sslClientKeyFile == null ? other$sslClientKeyFile != null : !this$sslClientKeyFile.equals(other$sslClientKeyFile))
            return false;
        final Object this$sslCaFile = this.getSslCaFile();
        final Object other$sslCaFile = other.getSslCaFile();
        if (this$sslCaFile == null ? other$sslCaFile != null : !this$sslCaFile.equals(other$sslCaFile)) return false;
        final Object this$enableDistributedTransaction = this.getEnableDistributedTransaction();
        final Object other$enableDistributedTransaction = other.getEnableDistributedTransaction();
        if (this$enableDistributedTransaction == null ? other$enableDistributedTransaction != null : !this$enableDistributedTransaction.equals(other$enableDistributedTransaction))
            return false;
        final Object this$maximumDistributedIdle = this.getMaximumDistributedIdle();
        final Object other$maximumDistributedIdle = other.getMaximumDistributedIdle();
        if (this$maximumDistributedIdle == null ? other$maximumDistributedIdle != null : !this$maximumDistributedIdle.equals(other$maximumDistributedIdle))
            return false;
        final Object this$idleTimeout = this.getIdleTimeout();
        final Object other$idleTimeout = other.getIdleTimeout();
        if (this$idleTimeout == null ? other$idleTimeout != null : !this$idleTimeout.equals(other$idleTimeout))
            return false;
        final Object this$maxLifetime = this.getMaxLifetime();
        final Object other$maxLifetime = other.getMaxLifetime();
        if (this$maxLifetime == null ? other$maxLifetime != null : !this$maxLifetime.equals(other$maxLifetime))
            return false;
        final Object this$waitingTimeout = this.getWaitingTimeout();
        final Object other$waitingTimeout = other.getWaitingTimeout();
        if (this$waitingTimeout == null ? other$waitingTimeout != null : !this$waitingTimeout.equals(other$waitingTimeout))
            return false;
        final Object this$acquireTimeout = this.getAcquireTimeout();
        final Object other$acquireTimeout = other.getAcquireTimeout();
        if (this$acquireTimeout == null ? other$acquireTimeout != null : !this$acquireTimeout.equals(other$acquireTimeout))
            return false;
        final Object this$parseTimeout = this.getParseTimeout();
        final Object other$parseTimeout = other.getParseTimeout();
        if (this$parseTimeout == null ? other$parseTimeout != null : !this$parseTimeout.equals(other$parseTimeout))
            return false;
        final Object this$executionTimeout = this.getExecutionTimeout();
        final Object other$executionTimeout = other.getExecutionTimeout();
        if (this$executionTimeout == null ? other$executionTimeout != null : !this$executionTimeout.equals(other$executionTimeout))
            return false;
        final Object this$shutdownTimeout = this.getShutdownTimeout();
        final Object other$shutdownTimeout = other.getShutdownTimeout();
        if (this$shutdownTimeout == null ? other$shutdownTimeout != null : !this$shutdownTimeout.equals(other$shutdownTimeout))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PgConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $loc = this.getLoc();
        result = result * PRIME + ($loc == null ? 43 : $loc.hashCode());
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final Object $databaseName = this.getDatabaseName();
        result = result * PRIME + ($databaseName == null ? 43 : $databaseName.hashCode());
        final Object $currentSchema = this.getCurrentSchema();
        result = result * PRIME + ($currentSchema == null ? 43 : $currentSchema.hashCode());
        final Object $maximumIdle = this.getMaximumIdle();
        result = result * PRIME + ($maximumIdle == null ? 43 : $maximumIdle.hashCode());
        final Object $sslMode = this.getSslMode();
        result = result * PRIME + ($sslMode == null ? 43 : $sslMode.hashCode());
        final Object $sslClientCertChainFile = this.getSslClientCertChainFile();
        result = result * PRIME + ($sslClientCertChainFile == null ? 43 : $sslClientCertChainFile.hashCode());
        final Object $sslClientKeyFile = this.getSslClientKeyFile();
        result = result * PRIME + ($sslClientKeyFile == null ? 43 : $sslClientKeyFile.hashCode());
        final Object $sslCaFile = this.getSslCaFile();
        result = result * PRIME + ($sslCaFile == null ? 43 : $sslCaFile.hashCode());
        final Object $enableDistributedTransaction = this.getEnableDistributedTransaction();
        result = result * PRIME + ($enableDistributedTransaction == null ? 43 : $enableDistributedTransaction.hashCode());
        final Object $maximumDistributedIdle = this.getMaximumDistributedIdle();
        result = result * PRIME + ($maximumDistributedIdle == null ? 43 : $maximumDistributedIdle.hashCode());
        final Object $idleTimeout = this.getIdleTimeout();
        result = result * PRIME + ($idleTimeout == null ? 43 : $idleTimeout.hashCode());
        final Object $maxLifetime = this.getMaxLifetime();
        result = result * PRIME + ($maxLifetime == null ? 43 : $maxLifetime.hashCode());
        final Object $waitingTimeout = this.getWaitingTimeout();
        result = result * PRIME + ($waitingTimeout == null ? 43 : $waitingTimeout.hashCode());
        final Object $acquireTimeout = this.getAcquireTimeout();
        result = result * PRIME + ($acquireTimeout == null ? 43 : $acquireTimeout.hashCode());
        final Object $parseTimeout = this.getParseTimeout();
        result = result * PRIME + ($parseTimeout == null ? 43 : $parseTimeout.hashCode());
        final Object $executionTimeout = this.getExecutionTimeout();
        result = result * PRIME + ($executionTimeout == null ? 43 : $executionTimeout.hashCode());
        final Object $shutdownTimeout = this.getShutdownTimeout();
        result = result * PRIME + ($shutdownTimeout == null ? 43 : $shutdownTimeout.hashCode());
        return result;
    }

    public String toString() {
        return "PgConfig(loc=" + this.getLoc() + ", username=" + this.getUsername() + ", password=" + this.getPassword() + ", databaseName=" + this.getDatabaseName() + ", currentSchema=" + this.getCurrentSchema() + ", maximumIdle=" + this.getMaximumIdle() + ", sslMode=" + this.getSslMode() + ", sslClientCertChainFile=" + this.getSslClientCertChainFile() + ", sslClientKeyFile=" + this.getSslClientKeyFile() + ", sslCaFile=" + this.getSslCaFile() + ", enableDistributedTransaction=" + this.getEnableDistributedTransaction() + ", maximumDistributedIdle=" + this.getMaximumDistributedIdle() + ", idleTimeout=" + this.getIdleTimeout() + ", maxLifetime=" + this.getMaxLifetime() + ", waitingTimeout=" + this.getWaitingTimeout() + ", acquireTimeout=" + this.getAcquireTimeout() + ", parseTimeout=" + this.getParseTimeout() + ", executionTimeout=" + this.getExecutionTimeout() + ", shutdownTimeout=" + this.getShutdownTimeout() + ")";
    }
}
