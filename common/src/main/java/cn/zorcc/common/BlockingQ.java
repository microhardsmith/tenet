package cn.zorcc.common;

/**
 *  单消费者阻塞队列接口
 */
public interface BlockingQ<T> {
    /**
     *  启动消费者线程
     */
    void start();

    /**
     * 获取消费者线程
     */
    Thread thread();

    /**
     *  向阻塞队列中添加任务
     */
    void put(T t);

    /**
     *  停止阻塞队列
     */
    void shutdown();
}
