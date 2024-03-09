package cn.zorcc.common.structure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IntMapTest {
    private static final int COUNT = 100000;
    private static void testMap(IntMap<Integer> map) {
        for(int i = 0; i < COUNT; i++) {
            map.put(i, i);
        }
        Assertions.assertEquals(map.count(), COUNT);
        for(int i = 0; i < COUNT; i++) {
            Integer current = map.get(i);
            Assertions.assertEquals(current, i);
            map.replace(i, current, current + 1);
            Integer next = map.get(i);
            Assertions.assertEquals(next, current + 1);
        }
        Assertions.assertEquals(map.count(), COUNT);
        List<Integer> l1 = map.asList();
        Assertions.assertEquals(l1.size(), COUNT);
        for(int i = 0; i < COUNT; i++) {
            Integer current = map.get(i);
            Assertions.assertTrue(map.remove(i, current));
        }
        Assertions.assertEquals(map.count(), 0);
        List<Integer> l2 = map.asList();
        Assertions.assertEquals(l2.size(), 0);
    }

    @Test
    public void testLinkedMap() {
        IntMap<Integer> map = IntMap.newLinkedMap(16);
        testMap(map);
    }

    @Test
    public void testTreeMap() {
        IntMap<Integer> map = IntMap.newTreeMap(16);
        testMap(map);
    }
}
