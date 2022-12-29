package cn.zorcc.app.http;

import cn.zorcc.common.event.Event;
import cn.zorcc.http.HttpReq;
import io.netty.channel.Channel;

public class AppHttpEvent extends Event {
    /**
     *  Http请求体
     */
    private HttpReq httpReq;
    /**
     *  Http请求通配字符串
     */
    private String wildStr;
    /**
     *  MethodInvoker中的方法调用索引
     */
    private int methodIndex;
    /**
     *  参数索引
     */
    private int argIndex;
    /**
     *  MethodInvoker中的方法调用参数
     */
    private Object[] args;
    /**
     *  Http通信Channel
     */
    private Channel channel;

    public HttpReq httpReq() {
        return httpReq;
    }

    public void setHttpReq(HttpReq httpReq) {
        this.httpReq = httpReq;
    }

    public String wildStr() {
        return wildStr;
    }

    public void setWildStr(String wildStr) {
        this.wildStr = wildStr;
    }

    public int methodIndex() {
        return methodIndex;
    }

    public void setMethodIndex(int methodIndex) {
        this.methodIndex = methodIndex;
    }

    public int argIndex() {
        return argIndex;
    }

    public void setArgIndex(int argIndex) {
        this.argIndex = argIndex;
    }

    public Object[] args() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Channel channel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
