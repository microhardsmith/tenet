package cn.zorcc.common.jmh;

import cn.zorcc.common.structure.MpscArrayQueue;
import cn.zorcc.common.structure.MpscLinkedQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Group)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
public class MpscQueueTest {
    private static final int PRODUCERS = 10;
    private static final int SIZE = 256;
    private static final MpscArrayQueue<Integer> q1 = MpscArrayQueue.create(SIZE);
    private static final MpscLinkedQueue<Integer> q2 = new MpscLinkedQueue<>();
    private static final MpscUnboundedAtomicArrayQueue<Integer> q3 = new MpscUnboundedAtomicArrayQueue<>(SIZE);
    private static final MpscLinkedAtomicQueue<Integer> q4 = new MpscLinkedAtomicQueue<>();
    private static final ConcurrentLinkedQueue<Integer> q5 = new ConcurrentLinkedQueue<>();

    @Benchmark
    @Group("q1")
    @GroupThreads(PRODUCERS)
    public void testQ1Offer(Control control) {
        while (!control.stopMeasurement) {
            q1.offer(1);
        }
    }

    @Benchmark
    @Group("q1")
    @GroupThreads()
    public void testQ1Poll(Control control) {
        while (!control.stopMeasurement) {
            if(q1.poll() != null) {
                break ;
            }
        }
    }

    @Benchmark
    @Group("q2")
    @GroupThreads(PRODUCERS)
    public void testQ2Offer(Control control) {
        while (!control.stopMeasurement) {
            q2.offer(1);
        }
    }

    @Benchmark
    @Group("q2")
    @GroupThreads()
    public void testQ2Poll(Control control) {
        while (!control.stopMeasurement) {
            if(q2.poll() != null) {
                break ;
            }
        }
    }

    @Benchmark
    @Group("q3")
    @GroupThreads(PRODUCERS)
    public void testQ3Offer(Control control) {
        while (!control.stopMeasurement) {
            q3.offer(1);
        }
    }

    @Benchmark
    @Group("q3")
    @GroupThreads()
    public void testQ3Poll(Control control) {
        while (!control.stopMeasurement) {
            if(q3.poll() != null) {
                break ;
            }
        }
    }

    @Benchmark
    @Group("q4")
    @GroupThreads(PRODUCERS)
    public void testQ4Offer(Control control) {
        while (!control.stopMeasurement) {
            q4.offer(1);
        }
    }

    @Benchmark
    @Group("q4")
    @GroupThreads()
    public void testQ4Poll(Control control) {
        while (!control.stopMeasurement) {
            if(q4.poll() != null) {
                break ;
            }
        }
    }

    @Benchmark
    @Group("q5")
    @GroupThreads(PRODUCERS)
    public void testQ5Offer(Control control) {
        while (!control.stopMeasurement) {
            q5.offer(1);
        }
    }

    @Benchmark
    @Group("q5")
    @GroupThreads()
    public void testQ5Poll(Control control) {
        while (!control.stopMeasurement) {
            if(q5.poll() != null) {
                break ;
            }
        }
    }


    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder().include(MpscQueueTest.class.getSimpleName()).build();
        new Runner(options).run();
    }
}
