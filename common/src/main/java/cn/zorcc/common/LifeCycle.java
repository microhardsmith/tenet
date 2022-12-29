package cn.zorcc.common;

/**
 *  容器生命周期接口
 */
public interface LifeCycle {
    /**
     *  容器初始化
     */
    void init();
    /**
     *  容器销毁
     */
    void shutdown();
}
