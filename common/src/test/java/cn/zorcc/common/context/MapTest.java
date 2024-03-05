package cn.zorcc.common.context;

import cn.zorcc.common.structure.IntMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MapTest {
    @Test
    public void testLinkedMap() {
        IntMap<Integer> map = IntMap.newLinkedMap(16);
        for(int i = 0; i < 1000; i++) {
            map.put(i, i);
        }
        for(int i = 0; i < 1000; i++) {
            Integer current = map.get(i);
            map.replace(i, current, current + 1);
            Integer next = map.get(i);
            System.out.println(STR."current : \{current}, next : \{next}");
        }
    }

    @Test
    public void testTreeMap() {
        IntMap<Integer> map = IntMap.newTreeMap(16);
        for(int i = 0; i < 1000; i++) {
            map.put(i, i);
        }
        for(int i = 0; i < 1000; i++) {
            Integer current = map.get(i);
            map.replace(i, current, current + 1);
            Integer next = map.get(i);
            System.out.println(STR."current : \{current}, next : \{next}");
        }
    }

    @Test
    public void testHashMap() {
        HashMap<Integer, Integer> map = new HashMap<>(64);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < 10000; i++) {
            map.put(i, random.nextInt());
        }
        System.out.println(map.size());
        Integer value = map.get(1);
        System.out.println(value);
    }
}
