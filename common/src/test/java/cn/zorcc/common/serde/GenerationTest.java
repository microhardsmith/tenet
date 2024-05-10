package cn.zorcc.common.serde;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

public final class GenerationTest {

    private static final int TEST_INT = 9999;
    private static final String TEST_STRING = "test function integrity";

    @Test
    public void testBean() {
        // test initialize
        Handle<Bean> handle = GenerationContext.getHandle(Bean.class);
        Column<Bean> aCol = handle.col("a");
        Column<Bean> bCol = handle.col("b");
        Assertions.assertNotNull(aCol);
        Assertions.assertNotNull(bCol);

        // test fetch
        Bean bean = new Bean();
        bean.setA(TEST_INT);
        bean.setB(TEST_STRING);
        Assertions.assertEquals(TEST_INT, aCol.fetch(bean));
        Assertions.assertEquals(TEST_STRING, bCol.fetch(bean));

        // test assign
        Supplier<Bean> assigner = handle.createAssigner();
        aCol.assign(assigner, TEST_INT);
        bCol.assign(assigner, TEST_STRING);
        Bean b = assigner.get();
        Assertions.assertEquals(TEST_INT, b.getA());
        Assertions.assertEquals(TEST_STRING, b.getB());
    }

    @Test
    public void testEnum() {
        // test initialize
        Handle<EnumBean> handle = GenerationContext.getHandle(EnumBean.class);
        Column<EnumBean> aCol = handle.col("a");
        Column<EnumBean> bCol = handle.col("b");
        Assertions.assertNotNull(aCol);
        Assertions.assertNotNull(bCol);

        // test fetch
        EnumBean bean = EnumBean.Test1;
        Assertions.assertEquals(bean.getA(), aCol.fetch(bean));
        Assertions.assertEquals(bean.getB(), bCol.fetch(bean));

        // test assign
        Supplier<EnumBean> assigner = handle.createAssigner();
        aCol.assign(assigner, EnumBean.Test2.getA());
        bCol.assign(assigner, String.copyValueOf(EnumBean.Test2.getB().toCharArray())); // copy so two string are different
        EnumBean b = assigner.get();
        Assertions.assertEquals(b, EnumBean.Test2);

        // test by name
        Assertions.assertEquals(handle.byName("Test1"), EnumBean.Test1);
    }

    @Test
    public void testRecord() {
        // test initialize
        Handle<RecordBean> handle = GenerationContext.getHandle(RecordBean.class);
        Column<RecordBean> aCol = handle.col("a");
        Column<RecordBean> bCol = handle.col("b");
        Assertions.assertNotNull(aCol);
        Assertions.assertNotNull(bCol);

        // test fetch
        RecordBean bean = new RecordBean(TEST_INT, TEST_STRING);
        Assertions.assertEquals(TEST_INT, aCol.fetch(bean));
        Assertions.assertEquals(TEST_STRING, bCol.fetch(bean));

        // test assign
        Supplier<RecordBean> assigner = handle.createAssigner();
        aCol.assign(assigner, TEST_INT);
        bCol.assign(assigner, TEST_STRING);
        RecordBean b = assigner.get();
        Assertions.assertEquals(TEST_INT, b.a());
        Assertions.assertEquals(TEST_STRING, b.b());
    }
}
