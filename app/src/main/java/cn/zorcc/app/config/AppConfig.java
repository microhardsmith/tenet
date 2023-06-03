package cn.zorcc.app.config;

import cn.zorcc.common.config.CacheConfig;
import cn.zorcc.common.config.ClusterConfig;
import cn.zorcc.common.config.CommonConfig;
import cn.zorcc.common.config.ToMintConfig;
import cn.zorcc.orm.PgConfig;
import lombok.Data;

/**
 * 微服务配置文件加载类
 */
@Data
public class AppConfig {
    /**
     * 通用配置文件
     */
    private CommonConfig common = new CommonConfig();
    /**
     * 数据库配置
     */
    private PgConfig database = new PgConfig();
    /**
     * app连接mint配置
     */
    private ToMintConfig mint = new ToMintConfig();
    /**
     * app缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    /**
     * 集群配置
     */
    private ClusterConfig cluster = new ClusterConfig();
}
