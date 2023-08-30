package cn.zorcc.common.example;

import java.util.List;

public class Bean {
    private Integer a;
    private String b;
    private List<Long> c;
    private TestEnum testEnum;

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public List<Long> getC() {
        return c;
    }

    public void setC(List<Long> c) {
        this.c = c;
    }

    public TestEnum getTestEnum() {
        return testEnum;
    }

    public void setTestEnum(TestEnum testEnum) {
        this.testEnum = testEnum;
    }
}
