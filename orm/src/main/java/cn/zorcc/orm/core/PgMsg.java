package cn.zorcc.orm.core;


public class PgMsg {
    /**
     * 消息类型
     */
    private byte type;
    /**
     * 消息长度,发送消息时该值会在PgEncoder中自行计算出来
     */
    private int length;
    /**
     * 消息内容
     */
    private byte[] data;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
