package cn.zorcc.common.serde;

@Serde
public enum EnumBean {

    Test1(123, "hello"),
    Test2(234, "world");

    @Attr(values = {"default:3", "json:str"})
    private final int a;
    private final String b;

    EnumBean(int a, String b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    public String getB() {
        return b;
    }
}
