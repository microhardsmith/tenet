package cn.zorcc.common.example;

import cn.zorcc.common.Gt;
import cn.zorcc.common.MetaInfo;
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
        MetaInfo metaInfo = gt.metaInfo("a");
        metaInfo.setter().accept(primitiveBean, 42);
        System.out.println(metaInfo.getter().apply(primitiveBean));
    }

    private static void testObjectGt() {
        Gt<Bean> gt = Gt.of(Bean.class);
        Bean bean = new Bean();
        MetaInfo metaInfo = gt.metaInfo("b");
        metaInfo.setter().accept(bean, "hello");
        System.out.println(metaInfo.getter().apply(bean));
    }
}
