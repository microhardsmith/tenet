package cn.zorcc.common.pojo;

import cn.zorcc.common.enums.PeerType;

/**
 * 用于描述一个节点
 * Peer中的所有信息在一个节点的生命周期中不应该被改变,如果需要调整必须进行重启
 */
public class Peer {
    /**
     *  节点类型
     */
    private PeerType type;
    /**
     * 名称,app指代微服务appName,其余为固定值
     */
    private String name;
    /**
     * 节点app类型,取值范围为0 ~ 1000,gateway默认使用1010,mint默认使用1020,实际范围为0 ~ 1023
     */
    private Integer appId;
    /**
     * 节点spanId,取值范围为0 ~ 15
     * raft集群本身不建议使用过多的节点,因为读写都会依赖于leader的性能
     * 如果需要给特定类型的微服务采用无状态的、非raft集群的、多节点部署的方式,可以通过给一个微服务使用多个appId的方式实现
     */
    private Integer spanId;
    /**
     * 节点location,指本地rpc服务器使用的ip和端口（对于网关而言是http）
     */
    private Loc loc;
    /**
     * 权重,默认100,仅在app负载均衡时使用,gateway与mint可无视该字段
     */
    private Integer weight = 100;

    public PeerType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public Integer appId() {
        return appId;
    }

    public Integer spanId() {
        return spanId;
    }

    public Loc loc() {
        return loc;
    }

    public Integer weight() {
        return weight;
    }

    public void setType(PeerType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public void setSpanId(Integer spanId) {
        this.spanId = spanId;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Peer peer) {
            return loc.equals(peer.loc());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", appId=" + appId +
                ", spanId=" + spanId +
                ", location=" + loc +
                ", weight=" + weight +
                '}';
    }
}
