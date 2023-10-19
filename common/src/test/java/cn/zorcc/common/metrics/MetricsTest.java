package cn.zorcc.common.metrics;

import org.junit.jupiter.api.Test;

public class MetricsTest {
    @Test
    public void testLoadAverage() {
        System.out.println(Metrics.getLoadAverage());
    }
}
