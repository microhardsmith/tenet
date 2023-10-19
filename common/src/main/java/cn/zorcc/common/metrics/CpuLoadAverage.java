package cn.zorcc.common.metrics;

/**
 *   l1 represents average load for last 1 minute
 *   l2 represents average load for last 5 minute
 *   l3 represents average load for last 15 minute
 */
public record CpuLoadAverage(
        double l1,
        double l2,
        double l3
) {
    public static final CpuLoadAverage UNSUPPORTED = new CpuLoadAverage(0.0d, 0.0d, 0.0d);
}
