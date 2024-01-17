package cn.zorcc.common.metrics;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

public class MetricsTest {
    @Test
    public void testLoadAverage() {
        try(Arena arena = Arena.ofConfined()) {
            System.out.println(Metrics.getLoadAverage(arena));
        }
    }
}
