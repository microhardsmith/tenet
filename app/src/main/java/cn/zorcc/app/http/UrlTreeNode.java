package cn.zorcc.app.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * url匹配树中的单个节点
 */
public class UrlTreeNode {
    /**
     * 本地methodIndex的值
     */
    private Integer index;
    /**
     * 转化函数
     */
    private Function<AppHttpEvent, Object[]> func;
    /**
     * 占位符Node,以:开头,只能出现在匹配路径的末尾
     */
    private UrlTreeNode wildNode;
    /**
     * 下个结点的map
     */
    private Map<String, UrlTreeNode> next = new HashMap<>();

    public Integer index() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Function<AppHttpEvent, Object[]> func() {
        return func;
    }

    public void setFunc(Function<AppHttpEvent, Object[]> func) {
        this.func = func;
    }

    public UrlTreeNode wildNode() {
        return wildNode;
    }

    public void setWildNode(UrlTreeNode wildNode) {
        this.wildNode = wildNode;
    }

    public Map<String, UrlTreeNode> next() {
        return next;
    }

    /**
     * 将所有子结点的map结构转化为不可变,使其并发安全并提升访问性能
     */
    public void sync() {
        if(wildNode != null) {
            wildNode.sync();
        }
        if(next.isEmpty()) {
            next = Collections.emptyMap();
        }else {
            next = Collections.unmodifiableMap(next);
            for (UrlTreeNode node : next.values()) {
                node.sync();
            }
        }
    }

}
