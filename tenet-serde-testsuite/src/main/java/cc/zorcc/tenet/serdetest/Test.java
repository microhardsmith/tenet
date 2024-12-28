package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Test {

    private static final Integer intValue = 123;
    private static final List<String> list = List.of("hello");
    private static final List<List<String>> doubleList = List.of(List.of("test1"));
    private static final Map<Integer, String> map = Map.of(123, "abc");

    private static final String JSON = "json";
    private static final String STR = "str";
    private static final String RAW = "raw";

    public static void main() {
        long current = System.currentTimeMillis();
        testNormalBean();
        testRecord();
        testEnum();
        testGBean();
        testEmpty();
        long after = System.currentTimeMillis();
        System.out.printf("Test finished, cost : %d ms, jvm started for %d ms%n", after - current, ManagementFactory.getRuntimeMXBean().getUptime());
    }

    private static void testNormalBean() {
        Refer<Bean> refer = Objects.requireNonNull(SerdeContext.refer(Bean.class));
        Builder<Bean> builder = refer.builder();

        Col<Bean> intValueCol = Objects.requireNonNull(refer.col("intValue"));
        intValueCol.assign(builder, intValue);

        Col<Bean> listCol = Objects.requireNonNull(refer.col("list"));
        listCol.assign(builder, list);

        Col<Bean> doubleListCol = Objects.requireNonNull(refer.col("doubleList"));
        doubleListCol.assign(builder, doubleList);

        Col<Bean> mapCol = Objects.requireNonNull(refer.col("map"));
        mapCol.assign(builder, map);

        // check assign
        Bean bean = Objects.requireNonNull(builder.build());
        assertEquals(bean.getIntValue(), intValue);
        assertEquals(bean.getList(), list);
        assertEquals(bean.getDoubleList(), doubleList);
        assertEquals(bean.getMap(), map);

        // check empty fetch
        Bean empty = new Bean();
        assertNull(intValueCol.fetch(empty));
        assertNull(listCol.fetch(empty));
        assertNull(doubleListCol.fetch(empty));
        assertNull(mapCol.fetch(empty));

        // check fetch
        assertEquals(Objects.requireNonNull(intValueCol.fetch(bean)), intValue);
        assertEquals(Objects.requireNonNull(listCol.fetch(bean)), list);
        assertEquals(Objects.requireNonNull(doubleListCol.fetch(bean)), doubleList);
        assertEquals(Objects.requireNonNull(mapCol.fetch(bean)), map);

        // check tags
        assertEquals(Objects.requireNonNull(intValueCol.tag(JSON)), STR);
        assertEquals(Objects.requireNonNull(listCol.tag(JSON)), RAW);
    }

    private static void testRecord() {
        Refer<RBean> refer = Objects.requireNonNull(SerdeContext.refer(RBean.class));
        Builder<RBean> builder = refer.builder();

        Col<RBean> intValueCol = Objects.requireNonNull(refer.col("intValue"));
        intValueCol.assign(builder, intValue);

        Col<RBean> listCol = Objects.requireNonNull(refer.col("list"));
        listCol.assign(builder, list);

        Col<RBean> doubleListCol = Objects.requireNonNull(refer.col("doubleList"));
        doubleListCol.assign(builder, doubleList);

        Col<RBean> mapCol = Objects.requireNonNull(refer.col("map"));
        mapCol.assign(builder, map);

        // check assign
        RBean bean = Objects.requireNonNull(builder.build());
        assertEquals(bean.intValue(), intValue);
        assertEquals(bean.list(), list);
        assertEquals(bean.doubleList(), doubleList);
        assertEquals(bean.map(), map);

        // check empty fetch
        RBean empty = new RBean(null, null, null, null);
        assertNull(intValueCol.fetch(empty));
        assertNull(listCol.fetch(empty));
        assertNull(doubleListCol.fetch(empty));
        assertNull(mapCol.fetch(empty));

        // check fetch
        assertEquals(Objects.requireNonNull(intValueCol.fetch(bean)), intValue);
        assertEquals(Objects.requireNonNull(listCol.fetch(bean)), list);
        assertEquals(Objects.requireNonNull(doubleListCol.fetch(bean)), doubleList);
        assertEquals(Objects.requireNonNull(mapCol.fetch(bean)), map);

        // check tags
        assertEquals(Objects.requireNonNull(intValueCol.tag(JSON)), STR);
        assertEquals(Objects.requireNonNull(listCol.tag(JSON)), RAW);
    }

    private static void testEnum() {
        Refer<EBean> refer = Objects.requireNonNull(SerdeContext.refer(EBean.class));
        Builder<EBean> builder = refer.builder();

        Col<EBean> intValueCol = Objects.requireNonNull(refer.col("intValue"));
        intValueCol.assign(builder, intValue);

        Col<EBean> listCol = Objects.requireNonNull(refer.col("list"));
        listCol.assign(builder, list);

        Col<EBean> doubleListCol = Objects.requireNonNull(refer.col("doubleList"));
        doubleListCol.assign(builder, doubleList);

        Col<EBean> mapCol = Objects.requireNonNull(refer.col("map"));
        mapCol.assign(builder, map);

        // check assign
        EBean bean = Objects.requireNonNull(builder.build());
        assertEquals(bean.getIntValue(), intValue);
        assertEquals(bean.getList(), list);
        assertEquals(bean.getDoubleList(), doubleList);
        assertEquals(bean.getMap(), map);
        assertEquals(bean, EBean.Test1);

        // check fetch
        assertEquals(Objects.requireNonNull(intValueCol.fetch(bean)), intValue);
        assertEquals(Objects.requireNonNull(listCol.fetch(bean)), list);
        assertEquals(Objects.requireNonNull(doubleListCol.fetch(bean)), doubleList);
        assertEquals(Objects.requireNonNull(mapCol.fetch(bean)), map);

        // check tags
        assertEquals(Objects.requireNonNull(intValueCol.tag(JSON)), STR);
        assertEquals(Objects.requireNonNull(listCol.tag(JSON)), RAW);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void testGBean() {
        Refer<GBean> refer = Objects.requireNonNull(SerdeContext.refer(GBean.class));
        Builder<GBean> builder = refer.builder();

        Col<GBean> intValueCol = Objects.requireNonNull(refer.col("intValue"));
        intValueCol.assign(builder, intValue);

        Col<GBean> listCol = Objects.requireNonNull(refer.col("list"));
        listCol.assign(builder, list);

        Col<GBean> aCol = Objects.requireNonNull(refer.col("a"));
        aCol.assign(builder, intValue);

        Col<GBean> bCol = Objects.requireNonNull(refer.col("b"));
        bCol.assign(builder, JSON);

        Col<GBean> cCol = Objects.requireNonNull(refer.col("c"));
        cCol.assign(builder, list);

        Col<GBean> dCol = Objects.requireNonNull(refer.col("d"));
        dCol.assign(builder, map);

        // check assign
        GBean<Integer, String> gBean = (GBean<Integer, String>) Objects.requireNonNull(builder.build());
        assertEquals(gBean.getIntValue(), intValue);
        assertEquals(gBean.getList(), list);
        assertEquals(gBean.getA(), intValue);
        assertEquals(gBean.getB(), JSON);
        assertEquals(gBean.getC(), list);
        assertEquals(gBean.getD(), map);

        // check empty fetch
        GBean<Integer, String> empty = new GBean<>();
        assertNull(intValueCol.fetch(empty));
        assertNull(listCol.fetch(empty));
        assertNull(aCol.fetch(empty));
        assertNull(bCol.fetch(empty));
        assertNull(cCol.fetch(empty));
        assertNull(dCol.fetch(empty));

        // check fetch
        assertEquals(Objects.requireNonNull(intValueCol.fetch(gBean)), intValue);
        assertEquals(Objects.requireNonNull(listCol.fetch(gBean)), list);
        assertEquals(Objects.requireNonNull(aCol.fetch(gBean)), intValue);
        assertEquals(Objects.requireNonNull(bCol.fetch(gBean)), JSON);
        assertEquals(Objects.requireNonNull(cCol.fetch(gBean)), list);
        assertEquals(Objects.requireNonNull(dCol.fetch(gBean)), map);

        // check tags
        assertEquals(Objects.requireNonNull(intValueCol.tag(JSON)), STR);
        assertEquals(Objects.requireNonNull(listCol.tag(JSON)), RAW);
    }

    private static void testEmpty() {
        Refer<Empty> refer = Objects.requireNonNull(SerdeContext.refer(Empty.class));
        assertNull(refer.col(""));
        assertEquals(Objects.requireNonNull(refer.byName("T1")), Empty.T1);
    }

    /**
     *   Simple assertion test, there is no need for importing JUnit here
     */
    private static void assertNull(@Nullable Object a) {
        if(a != null) {
            throw new AssertionError();
        }
    }

    /**
     *   Simple assertion test, there is no need for importing JUnit here
     */
    private static void assertEquals(@NonNull Object a, @NonNull Object b) {
        if(!a.equals(b)) {
            throw new AssertionError();
        }
    }
}
