package cn.zorcc.common.config;

import cn.zorcc.common.pojo.Peer;
import lombok.Data;

import java.util.List;

/**
 * 微服务或网关连接mint的配置
 */
@Data
public class ToMintConfig {
    /**
     * 重试连接Mint间隔,单位毫秒
     */
    private Integer retryInterval = 5000;
    /**
     * 主动拉取信息间隔,单位毫秒,mint在发生变动时会主动推送消息,该值可尽量大,设置为负数可禁用该功能
     */
    private Integer fetchInterval = 30000;
    /**
     * mint服务器地址
     */
    private List<Peer> mintList;
}
