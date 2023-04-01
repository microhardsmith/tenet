package cn.zorcc.common.pojo;

/**
 * 标识一台特定机器在网络中所处的位置
 * @param ip 机器网卡ipv4地址
 * @param port 机器暴露给外部的端口号
 */
public record Loc (
        String ip,
        Integer port
) {

}
