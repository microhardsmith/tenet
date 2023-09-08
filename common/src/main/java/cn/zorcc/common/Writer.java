package cn.zorcc.common;

public interface Writer {
    void writeByte(byte data);

    void writeBytes(byte... data);

    void writeBytes(byte[] data, int offset, int len);

    String asString();

    ReadBuffer asReadBuffer();
}
