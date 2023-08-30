package cn.zorcc.common.json;

public enum JsonReaderState {
    /**
     *   Object means current JsonReader has found a LCB in the readBuffer
     */
    Object,
    /**
     *   Line means current JsonReader has parsed a single key-value pair and reaching a COMMA, there should be more data to be processed
     */
    Line,
    /**
     *   Array means current JsonReader has found a LSB in the readBuffer
     */
    Array,
    /**
     *   Line means current JsonReader has parsed a single key-value pair and reaching a RCB, the object has been completely parsed
     */
    LineEnd,
    /**
     *   Array means current JsonReader has found a RSB in the readBuffer, the array has been completely parsed
     */
    ArrayEnd,

//    OBJECT,
//    KEY,
//    KEY_END,
//    COLON,
//    STRING,
//    STRING_END,
//    COMMA,
//    ARRAY,
//    ARRAY_END,
//    END,
//    ESCAPE
}
