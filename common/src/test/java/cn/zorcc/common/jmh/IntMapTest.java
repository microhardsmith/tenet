package cn.zorcc.common.jmh;

import cn.zorcc.common.Constants;
import cn.zorcc.common.network.Socket;
import cn.zorcc.common.structure.IntMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.HashMap;
import java.util.Map;

public class IntMapTest extends JmhTest {
    private static final Object o = new Object();
    @Param({"5", "20", "100", "1000", "10000"})
    private int size;

    @Benchmark
    public void testSocketMap(Blackhole bh) {
        Map<Socket, Object> m = new HashMap<>(Constants.KB);
        for(int i = 0; i < size; i++) {
            Socket socket = new Socket(i);
            m.put(socket, o);
            bh.consume(m.get(socket));
        }
    }

    @Benchmark
    public void testHashMap(Blackhole bh) {
        Map<Integer, Object> m = new HashMap<>(Constants.KB);
        for(int i = 0; i < size; i++) {
            m.put(i, o);
            bh.consume(m.get(i));
        }
    }

    @Benchmark
    public void testIntMap(Blackhole bh) {
        IntMap<Object> m = new IntMap<>(Constants.KB);
        for(int i = 0; i < size; i++) {
            m.put(i, o);
            bh.consume(m.get(i));
        }
    }

    public static void main(String[] args) throws RunnerException {
        runTest(IntMapTest.class);
    }
}
