package cn.zorcc.gateway.config;

import cn.zorcc.common.config.CommonConfig;
import cn.zorcc.common.config.ToMintConfig;
import cn.zorcc.http.HttpConfig;
import lombok.Data;

/**
 * 网关配置文件
 */
@Data
public class GatewayConfig {
    /**
     * 网关唯一id,不同网关之间配置不同项,用于生成traceId,取值范围为0 ~ 63
     */
    private Integer uniqueId;
    /**
     * 通用配置文件
     */
    private CommonConfig common = new CommonConfig();
    /**
     * http配置文件
     */
    private HttpConfig http = new HttpConfig();
    /**
     * gateway连接mint配置
     */
    private ToMintConfig toMint = new ToMintConfig();
}
