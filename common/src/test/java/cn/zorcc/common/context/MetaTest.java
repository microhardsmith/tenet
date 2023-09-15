package cn.zorcc.common.context;

import cn.zorcc.common.Meta;
import cn.zorcc.common.beans.Beta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MetaTest {
    private static final int a1 = Integer.MAX_VALUE;
    private static final Integer a2 = Integer.MIN_VALUE;
    private static final int[] a3 = new int[]{1,2,3};
    private static final Integer[] a4 = new Integer[]{2,3,4};
    private static final String a5 = "hello world";
    private static final List<String> a6 = List.of("abc", "lxc");
    private static final List<int[]> a7 = List.of(new int[]{1,2,3}, new int[]{3,4,5});
    private static final List<Integer[]> a8 = List.of(new Integer[]{1,2,3}, new Integer[]{3,4,5});
    private static final Beta beta = new Beta();

    static {
        beta.setA1(a1);
        beta.setA2(a2);
        beta.setA3(a3);
        beta.setA4(a4);
        beta.setA5(a5);
        beta.setA6(a6);
        beta.setA7(a7);
        beta.setA8(a8);
    }
    @Test
    public void testMetaGetter() {
        Meta<Beta> betaMeta = Meta.of(Beta.class);
        Assertions.assertEquals(betaMeta.metaInfo("a1").invokeGetter(beta), a1);
        Assertions.assertEquals(betaMeta.metaInfo("a2").invokeGetter(beta), a2);
        Assertions.assertEquals(betaMeta.metaInfo("a3").invokeGetter(beta), a3);
        Assertions.assertEquals(betaMeta.metaInfo("a4").invokeGetter(beta), a4);
        Assertions.assertEquals(betaMeta.metaInfo("a5").invokeGetter(beta), a5);
        Assertions.assertEquals(betaMeta.metaInfo("a6").invokeGetter(beta), a6);
        Assertions.assertEquals(betaMeta.metaInfo("a7").invokeGetter(beta), a7);
        Assertions.assertEquals(betaMeta.metaInfo("a8").invokeGetter(beta), a8);
    }

    @Test
    public void testMetaSetter() {
        Meta<Beta> betaMeta = Meta.of(Beta.class);
        Beta b = new Beta();
        betaMeta.metaInfo("a1").setter().accept(b, a1);
        Assertions.assertEquals(b.getA1(), a1);
        betaMeta.metaInfo("a2").setter().accept(b, a2);
        Assertions.assertEquals(b.getA2(), a2);
        betaMeta.metaInfo("a3").setter().accept(b, a3);
        Assertions.assertEquals(b.getA3(), a3);
        betaMeta.metaInfo("a4").setter().accept(b, a4);
        Assertions.assertEquals(b.getA4(), a4);
        betaMeta.metaInfo("a5").setter().accept(b, a5);
        Assertions.assertEquals(b.getA5(), a5);
        betaMeta.metaInfo("a6").setter().accept(b, a6);
        Assertions.assertEquals(b.getA6(), a6);
        betaMeta.metaInfo("a7").setter().accept(b, a7);
        Assertions.assertEquals(b.getA7(), a7);
        betaMeta.metaInfo("a8").setter().accept(b, a8);
        Assertions.assertEquals(b.getA8(), a8);
    }

    @Test
    public void testMetaConstructor() {
        Meta<Beta> betaMeta = Meta.of(Beta.class);
        Beta beta = betaMeta.constructor().get();
        Assertions.assertNotNull(beta);
    }
}
