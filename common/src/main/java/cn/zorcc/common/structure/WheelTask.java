package cn.zorcc.common.structure;

public record WheelTask(
        long execMilli,
        long period,
        Runnable mission
) {
}
