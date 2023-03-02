package cn.zorcc.common.wheel;

public interface Job {
    /**
     *   返回当前任务已执行的次数
     */
    long count();

    /**
     *   取消任务执行,返回是否取消成功
     */
    boolean cancel();
}
