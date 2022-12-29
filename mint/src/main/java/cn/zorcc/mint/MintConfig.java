package cn.zorcc.mint;

import cn.zorcc.common.config.CacheConfig;
import cn.zorcc.common.config.ClusterConfig;
import cn.zorcc.common.config.CommonConfig;
import lombok.Data;

@Data
public class MintConfig {
    /**
     * jwt秘钥
     */
    private String secret = "blVm4EPe55kpAcZz";
    /**
     * 默认登录的用户名
     */
    private String rootUserName = "root";
    /**
     * 默认登录密码
     */
    private String rootPassword = "blVm4EPe55kpAcZz";
    /**
     * 通用配置文件
     */
    private CommonConfig common = new CommonConfig();
    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    /**
     * 集群配置
     */
    private ClusterConfig cluster = new ClusterConfig();
}
