package cn.zorcc.common.context;

import cn.zorcc.common.Record;
import cn.zorcc.common.RecordInfo;
import cn.zorcc.common.beans.Alpha;
import cn.zorcc.common.util.ReflectUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

/**
 * Please note that the use of generic arrays in Java can lead to potential issues and is generally discouraged. Due to type erasure, creating arrays of generic types can result in compiler warnings and may cause runtime exceptions.
 * It is recommended to use collections (such as List) instead of generic arrays whenever possible. Collections provide more flexibility and type safety compared to generic arrays.
 * By using collections, you can benefit from the built-in methods and features provided by the collection classes, and avoid the limitations and risks associated with generic arrays.
 */
public class RecordTest {
    private static final int a1 = Integer.MAX_VALUE;
    private static final Integer a2 = Integer.MIN_VALUE;
    private static final int[] a3 = new int[]{1,2,3};
    private static final Integer[] a4 = new Integer[]{2,3,4};
    private static final String a5 = "hello world";
    private static final List<String> a6 = List.of("abc", "lxc");
    private static final List<int[]> a7 = List.of(new int[]{1,2,3}, new int[]{3,4,5});
    private static final List<Integer[]> a8 = List.of(new Integer[]{1,2,3}, new Integer[]{3,4,5});
    private static final Alpha alpha = new Alpha(a1, a2, a3, a4, a5, a6, a7, a8);

    @Test
    public void testNull() {
        Function<Object[], Alpha> c = ReflectUtil.createRecordConstructor(Alpha.class, List.of(int.class, Integer.class, int[].class, Integer[].class, String.class, List.class, List.class, List.class));
        Object[] objects = new Object[8];
        objects[0] = a1;
        Alpha a = c.apply(objects);
        Assertions.assertNull(a.str());
    }

    @Test
    public void testRecordConstructor() {
        Function<Object[], Alpha> c = ReflectUtil.createRecordConstructor(Alpha.class, List.of(int.class, Integer.class, int[].class, Integer[].class, String.class, List.class, List.class, List.class));
        Object[] objects = new Object[8];
        objects[0] = a1;
        objects[1] = a2;
        objects[2] = a3;
        objects[3] = a4;
        objects[4] = a5;
        objects[5] = a6;
        objects[6] = a7;
        objects[7] = a8;
        Alpha a = c.apply(objects);
        Assertions.assertEquals(a.primitiveInt(), a1);
    }

    @Test
    public void testRecordGetter() {
        Record<Alpha> r = Record.of(Alpha.class);
        RecordInfo primitiveInt = r.recordInfo("primitiveInt");
        Assertions.assertEquals(primitiveInt.getter().apply(alpha), a1);
        RecordInfo normalInt = r.recordInfo("normalInt");
        Assertions.assertEquals(normalInt.getter().apply(alpha), a2);
        RecordInfo intArray = r.recordInfo("intArray");
        Assertions.assertEquals(intArray.getter().apply(alpha), a3);
        RecordInfo integerArray = r.recordInfo("integerArray");
        Assertions.assertEquals(integerArray.getter().apply(alpha), a4);
        RecordInfo str = r.recordInfo("str");
        Assertions.assertEquals(str.getter().apply(alpha), a5);
        RecordInfo list = r.recordInfo("list");
        Assertions.assertEquals(list.getter().apply(alpha), a6);
        RecordInfo intArrayList = r.recordInfo("intArrayList");
        Assertions.assertEquals(intArrayList.getter().apply(alpha), a7);
        RecordInfo integerArrayList = r.recordInfo("integerArrayList");
        Assertions.assertEquals(integerArrayList.getter().apply(alpha), a8);
    }

    @Test
    public void testRecord() {
        Record<Alpha> r = Record.of(Alpha.class);
        Object[] objects = new Object[r.elementSize()];
        Assertions.assertEquals(objects.length, 8);
        objects[0] = a1;
        objects[1] = a2;
        objects[2] = a3;
        objects[3] = a4;
        objects[4] = a5;
        objects[5] = a6;
        objects[6] = a7;
        objects[7] = a8;
        Alpha al = r.construct(objects);
        Assertions.assertEquals(al, alpha);
    }
}
