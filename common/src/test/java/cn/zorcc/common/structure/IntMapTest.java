package cn.zorcc.common.structure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class IntMapTest {

    private static final int COUNT = 100000;
    private static final int ROUNDS = 100;

    private static void testMap(IntMap<Integer> map) {
        Set<Integer> filter = new HashSet<>(COUNT);
        List<Integer> keys = new ArrayList<>(COUNT);
        List<Integer> values = new ArrayList<>(COUNT);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // test put
        for(int i = 0; i < COUNT; i++) {
            int k;
            do {
                k = random.nextInt();
            } while (!filter.add(k));
            int v = random.nextInt();
            keys.add(k);
            values.add(v);
            map.put(k, v);
        }
        Assertions.assertEquals(map.count(), COUNT);

        // test asList
        List<Integer> original = map.asList();
        Assertions.assertEquals(original.size(), COUNT);

        // test get and replace
        for(int i = 0; i < COUNT; i++) {
            Integer k = keys.get(i);
            Integer v = values.get(i);
            Integer current = map.get(k);
            Assertions.assertEquals(current, v);
            Integer newValue = current + 1;
            map.replace(k, current, newValue);
            Integer next = map.get(k);
            Assertions.assertEquals(next, newValue);
        }
        Assertions.assertEquals(map.count(), COUNT);

        // test asList
        List<Integer> l1 = map.asList();
        Assertions.assertEquals(l1.size(), COUNT);
        // test remove
        for(int i = 0; i < COUNT; i++) {
            Integer k = keys.get(i);
            Integer v = map.get(k);
            Assertions.assertTrue(map.remove(k, v));
        }
        Assertions.assertEquals(map.count(), 0);

        // test asList empty
        List<Integer> l2 = map.asList();
        Assertions.assertEquals(l2.size(), 0);
    }

    @Test
    public void testLinkedMap() {
        for(int i = 0; i < ROUNDS; i++) {
            IntMap<Integer> map = IntMap.newLinkedMap(16);
            testMap(map);
        }
    }

    @Test
    public void testTreeMap() {
        for(int i = 0; i < ROUNDS; i++) {
            IntMap<Integer> map = IntMap.newTreeMap(16);
            testMap(map);
        }
    }
}
