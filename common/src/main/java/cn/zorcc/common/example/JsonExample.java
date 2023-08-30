package cn.zorcc.common.example;

import cn.zorcc.common.Gt;
import cn.zorcc.common.GtInfo;
import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.json.JsonParser;

import java.util.List;

public class JsonExample {

    public static void main(String[] args) throws Throwable {
        testObjectGt();
        testPrimitiveGt();
        testSerialize();
    }

    private static void testSerialize() {
        try(ResizableByteArray resizableByteArray = new ResizableByteArray()) {
            Bean bean = new Bean();
            bean.setA(123);
            bean.setB("hello");
            bean.setC(List.of(1L,2L,3L));
            bean.setTestEnum(TestEnum.A);
            JsonParser.serializeAsObject(resizableByteArray, bean);
            System.out.println(new String(resizableByteArray.toArray()));
        }
    }

    private static void testPrimitiveGt() {
        Gt<PrimitiveBean> gt = Gt.of(PrimitiveBean.class);
        PrimitiveBean primitiveBean = new PrimitiveBean();
        GtInfo gtInfo = gt.metaInfo("a");
        gtInfo.setter().accept(primitiveBean, 42);
        System.out.println(gtInfo.getter().apply(primitiveBean));
    }

    private static void testObjectGt() {
        Gt<Bean> gt = Gt.of(Bean.class);
        Bean bean = new Bean();
        GtInfo gtInfo = gt.metaInfo("b");
        gtInfo.setter().accept(bean, "hello");
        System.out.println(gtInfo.getter().apply(bean));
    }
}
