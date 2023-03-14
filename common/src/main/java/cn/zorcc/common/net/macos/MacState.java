package cn.zorcc.common.net.macos;

import cn.zorcc.common.Constants;
import cn.zorcc.common.IntMap;
import cn.zorcc.common.net.Socket;

public class MacState {
    private static final int SOCKET_MAP_SIZE = 16 * Constants.KB;
    private int kq;
    private short kqErr;
    private short kqEof;
    private Socket serverSocket;
    private int addressLen;
    private int ewouldblock;
    private int einprogress;

    public int kq() {
        return kq;
    }

    public void setKq(int kq) {
        this.kq = kq;
    }

    public short kqErr() {
        return kqErr;
    }

    public void setKqErr(short kqErr) {
        this.kqErr = kqErr;
    }

    public short kqEof() {
        return kqEof;
    }

    public void setKqEof(short kqEof) {
        this.kqEof = kqEof;
    }

    public Socket serverSocket() {
        return serverSocket;
    }

    public void setServerSocket(Socket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public int ewouldblock() {
        return ewouldblock;
    }

    public void setEwouldblock(int ewouldblock) {
        this.ewouldblock = ewouldblock;
    }

    public int einprogress() {
        return einprogress;
    }

    public void setEinprogress(int einprogress) {
        this.einprogress = einprogress;
    }

    public int addressLen() {
        return addressLen;
    }

    public void setAddressLen(int addressLen) {
        this.addressLen = addressLen;
    }
}
