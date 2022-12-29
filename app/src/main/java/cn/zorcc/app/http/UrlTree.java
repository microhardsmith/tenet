package cn.zorcc.app.http;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 路由必须以 / 作为开头
 * 使用 <> 框住的部分表示通配,且只能出现在路由的末尾
 * 路由之间不能产生冲突,一个url不能被映射到多个方法
 * 在一个HttpMethod下只构建一个UrlTree
 */
public class UrlTree {
    /**
     * 按照/划分的路径树头结点
     */
    private final UrlTreeNode head = new UrlTreeNode();
    /**
     *  通配符标识
     */
    private static final String WILD_STRING = "<>";

    /**
     *  初始化路径树所有节点
     */
    public void init() {
        head.sync();
    }

    /**
     * 添加可匹配的路由
     * @param path  路由路径
     * @param methodIndex 在MethodEventHandler中可执行的方法索引
     * @param func 将Http事件转化为参数列表的匿名函数
     */
    public void addPath(String path, int methodIndex, Function<AppHttpEvent, Object[]> func) {
        List<String> segmentList = trimSplit(path);
        int size = segmentList.size();
        UrlTreeNode current = head;
        for(int i = 0; i < size; i++) {
            String segment = segmentList.get(i);
            if(!segment.isBlank()) {
                if(WILD_STRING.equals(segment)) {
                    if(i + 1 != size) {
                        throw new FrameworkException(ExceptionType.HTTP, "Wild matching is only permitted in final segment");
                    }
                    if(current.wildNode() != null) {
                        throw new FrameworkException(ExceptionType.HTTP, "Wild matching has conflict");
                    }
                    current.setWildNode(new UrlTreeNode());
                    current = current.wildNode();
                }else {
                    current = current.next().computeIfAbsent(segment, k -> new UrlTreeNode());
                }
            }
        }
        if(current.index() != null) {
            throw new FrameworkException(ExceptionType.HTTP, "Index matching has conflict");
        }
        current.setIndex(methodIndex);
        current.setFunc(func);
    }

    /**
     * 搜索指定HttpEvent在路由匹配树中的节点
     * @return 是否存在匹配
     */
    public boolean searchPath(AppHttpEvent appHttpEvent) {
        List<String> segmentList = trimSplit(appHttpEvent.httpReq().path());
        int size = segmentList.size();
        UrlTreeNode current = head;
        for(int i = 0; i < size; i++) {
            String segment = segmentList.get(i);
            UrlTreeNode nextNode = current.next().get(segment);
            if(nextNode == null) {
                UrlTreeNode wildNode = current.wildNode();
                if(i != size - 1 || wildNode == null) {
                    return false;
                }else {
                    appHttpEvent.setWildStr(segment);
                    appHttpEvent.setMethodIndex(wildNode.index());
                    appHttpEvent.setArgs(wildNode.func().apply(appHttpEvent));
                    return true;
                }
            }
            current = nextNode;
        }
        appHttpEvent.setMethodIndex(current.index());
        appHttpEvent.setArgs(current.func().apply(appHttpEvent));
        return true;
    }

    /**
     *  按照“/"分隔路径，去除多余的空格和分隔符
     */
    public List<String> trimSplit(String path) {
        char[] chars = path.trim().toCharArray();
        List<String> result = new ArrayList<>();
        int begin = -1;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(c == '/' && begin != -1) {
                result.add(new String(chars, begin, i - begin));
                begin = -1;
            }else if(c != '/' && begin == -1) {
                begin = i;
            }
        }
        if(begin != -1) {
            result.add(new String(chars, begin, chars.length - begin));
        }
        return result;
    }


}
