package cn.zorcc.orm;

import cn.zorcc.common.Constants;
import cn.zorcc.common.util.PlatformUtil;
import cn.zorcc.orm.pg.PgConstants;
import lombok.Data;

@Data
public class DatabaseConfig {
    /**
     * 是否需要连接数据库
     */
    private boolean enabled = true;
    /**
     * 数据库地址
     */
    private String ip = "127.0.0.1";
    /**
     * 数据库端口
     */
    private Integer port = 5432;
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
     * 数据库最小空闲连接数,配置小于0则默认与maximumIdle相同,hikari官方建议与maximumIdle相同
     */
    private Integer minimumIdle = -1;
    /**
     * 数据库最大连接池数量,建议设置数: (核心数 * 2) + 有效硬盘数
     */
    private Integer maximumIdle = PlatformUtil.getCpuCores() * 2 + 4;
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
    private Integer maximumDistributedIdle = PlatformUtil.getCpuCores();
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
}
