package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.Builder;
import cc.zorcc.tenet.serde.Col;
import cc.zorcc.tenet.serde.Refer;
import cc.zorcc.tenet.serde.SerdeContext;

public final class SerdeTest {
    private static final Integer intValue = 123;
    private static final String strValue = "hello";

    void main() {
        testBean();
        testRBean();
        testEBean();
        testEmpty();
        System.out.println("Test passed.");
    }

    /**
     *   Test simple bean serialization and deserialization
     */
    private static void testBean() {
        Refer<Bean> refer = SerdeContext.refer(Bean.class);
        Builder<Bean> builder = refer.builder();
        Col<Bean> intValueCol = refer.col("intValue");
        intValueCol.assign(builder, intValue);
        Col<Bean> strValueCol = refer.col("strValue");
        strValueCol.assign(builder, strValue);
        Bean b1 = builder.build();
        if(!b1.getIntValue().equals(intValue) || !b1.getStrValue().equals(strValue)) {
            throw new AssertionError();
        }
        Bean b2 = new Bean();
        b2.setIntValue(intValue);
        b2.setStrValue(strValue);
        if (!intValueCol.fetch(b2).equals(intValue) || !strValueCol.fetch(b2).equals(strValue)) {
            throw new AssertionError();
        }
        if (!intValueCol.tag("json").equals("str")) {
            throw new AssertionError();
        }
    }

    /**
     *   Test record bean serialization and deserialization
     */
    private static void testRBean() {
        Refer<RBean> refer = SerdeContext.refer(RBean.class);
        Builder<RBean> builder = refer.builder();
        Col<RBean> intValueCol = refer.col("intValue");
        intValueCol.assign(builder, intValue);
        Col<RBean> strValueCol = refer.col("strValue");
        strValueCol.assign(builder, strValue);
        RBean b1 = builder.build();
        if(!b1.intValue().equals(intValue) || !b1.strValue().equals(strValue)) {
            throw new AssertionError();
        }
        RBean b2 = new RBean(intValue, strValue);
        if (!intValueCol.fetch(b2).equals(intValue) || !strValueCol.fetch(b2).equals(strValue)) {
            throw new AssertionError();
        }
        if (!intValueCol.tag("json").equals("str")) {
            throw new AssertionError();
        }
    }

    /**
     *   Test enum bean serialization and deserialization
     */
    private static void testEBean() {
        Refer<EBean> refer = SerdeContext.refer(EBean.class);
        Builder<EBean> builder = refer.builder();
        Col<EBean> intValueCol = refer.col("intValue");
        intValueCol.assign(builder, intValue);
        Col<EBean> strValueCol = refer.col("strValue");
        strValueCol.assign(builder, strValue);
        EBean b1 = builder.build();
        if(b1 != EBean.Test1) {
            throw new AssertionError();
        }
        EBean b2 = EBean.Test1;
        if (!intValueCol.fetch(b2).equals(intValue) || !strValueCol.fetch(b2).equals(strValue)) {
            throw new AssertionError();
        }
        if (!intValueCol.tag("json").equals("str")) {
            throw new AssertionError();
        }
        EBean test2 = refer.byName("Test2");
        if(test2 != EBean.Test2) {
            throw new AssertionError();
        }
    }

    /**
     *   Test empty enum
     */
    private static void testEmpty() {
        Refer<Empty> refer = SerdeContext.refer(Empty.class);
        Builder<Empty> builder = refer.builder();
        if(builder != null) {
            throw new AssertionError();
        }
        Empty three = refer.byName("Three");
        if(three != Empty.Three) {
            throw new AssertionError();
        }
    }
}
