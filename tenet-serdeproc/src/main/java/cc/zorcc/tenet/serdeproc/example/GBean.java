package cc.zorcc.tenet.serdeproc.example;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.Serde;

import java.util.List;
import java.util.Map;

@Serde
public class GBean<A extends Number, B> {
    @Attr({"json:str"})
    private Integer intValue;

    @Attr("json:raw")
    private List<String> list;

    private A a;

    private B b;

    private List<B> c;

    private Map<A, B> d;

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    public List<B> getC() {
        return c;
    }

    public void setC(List<B> c) {
        this.c = c;
    }

    public Map<A, B> getD() {
        return d;
    }

    public void setD(Map<A, B> d) {
        this.d = d;
    }
}
