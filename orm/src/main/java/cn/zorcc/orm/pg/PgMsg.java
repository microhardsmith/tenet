package cn.zorcc.orm.pg;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
