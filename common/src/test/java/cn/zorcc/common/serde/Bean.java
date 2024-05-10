package cn.zorcc.common.serde;

@Serde
public class Bean {
    @Attr(values = {"default:1", "json:str"})
    private int a;

    // test without attr
    private String b;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }
}
