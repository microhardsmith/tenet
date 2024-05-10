package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.structure.WriteBuffer;

import java.nio.charset.StandardCharsets;

/**
 *   Http header abstraction backed by a hashmap
 */
public final class HttpHeader {
    public static final String K_CONTENT_TYPE = "Content-Type";
    public static final String V_JSON_TYPE = "application/json";
    public static final String V_BINARY_TYPE = "application/octet-stream";
    public static final String K_CONTENT_LENGTH = "Content-Length";
    public static final String K_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String K_CONTENT_ENCODING = "Content-Encoding";
    public static final String V_GZIP = "gzip";
    public static final String V_DEFLATE = "deflate";
    public static final String V_BR = "br";
    public static final String V_ZSTD = "zstd";
    public static final String K_CONNECTION = "Connection";
    public static final String K_KEEP_ALIVE = "Keep-Alive";
    public static final String V_KEEP_ALIVE = "keep-alive";
    public static final String V_TIMEOUT = "timeout=";
    public static final String K_DATE = "Date";
    public static final String K_TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String V_CHUNKED = "chunked";
    public static final String K_AUTHORIZATION = "Authorization";

    /**
     *   Fixed http header array size, 8 would be enough for most applications
     */
    private static final int HEADER_SIZE = 8;
    private static final int MASK = HEADER_SIZE - 1;
    private Node[] nodes;
    private static final class Node {
        private final String key;
        private String value;
        private Node next;

        public Node(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public Node(String key, String value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    public String get(String key) {
        if(nodes == null) {
            return null;
        }
        final int index = key.hashCode() & MASK;
        Node ptr = nodes[index];
        while (ptr != null) {
            if(ptr.key.equals(key)) {
                return ptr.value;
            }else {
                ptr = ptr.next;
            }
        }
        return null;
    }

    public void put(String key, String value) {
        if(nodes == null) {
            nodes = new Node[HEADER_SIZE];
        }
        final int index = key.hashCode() & MASK;
        final Node current = nodes[index];
        if(current == null) {
            nodes[index] = new Node(key, value);
        }else {
            Node ptr = current;
            while (ptr != null) {
                if(key.equals(ptr.key)) {
                    ptr.value = value;
                    return ;
                }else {
                    ptr = ptr.next;
                }
            }
            nodes[index] = new Node(key, value, current);
        }
    }


    public void encode(WriteBuffer writeBuffer) {
        if(nodes != null) {
            for (Node node : nodes) {
                Node ptr = node;
                while (ptr != null) {
                    writeBuffer.writeBytes(ptr.key.getBytes(StandardCharsets.UTF_8));
                    writeBuffer.writeByte(Constants.COLON, Constants.SPACE);
                    writeBuffer.writeBytes(ptr.value.getBytes(StandardCharsets.UTF_8));
                    writeBuffer.writeByte(Constants.CR, Constants.LF);
                    ptr = ptr.next;
                }
            }
        }
    }
}
