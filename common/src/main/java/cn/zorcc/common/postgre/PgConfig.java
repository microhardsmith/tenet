package cn.zorcc.common.postgre;

import cn.zorcc.common.Constants;
import cn.zorcc.common.network.Loc;
import cn.zorcc.common.util.NativeUtil;

public final class PgConfig {
    /**
     *   Postgresql database server location
     */
    private Loc loc;
    /**
     *   Postgresql database username
     */
    private String userName;
    /**
     *   Postgresql database password
     */
    private String password;
    /**
     *   Postgresql database name
     */
    private String databaseName = "postgres";
    /**
     *   Postgresql database schema
     */
    private String currentSchema = "public";
    /**
     *   The default ssl connection mode, could be: prefer, verify-ca, verify-full
     */
    private String sslMode = Constants.PG_SSL_PREFER;
    private Integer maxConn = NativeUtil.getCpuCores();

    public Loc getLoc() {
        return loc;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getCurrentSchema() {
        return currentSchema;
    }

    public void setCurrentSchema(String currentSchema) {
        this.currentSchema = currentSchema;
    }

    public String getSslMode() {
        return sslMode;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public Integer getMaxConn() {
        return maxConn;
    }

    public void setMaxConn(Integer maxConn) {
        this.maxConn = maxConn;
    }
}
