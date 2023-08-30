package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Gt;

import java.util.ArrayList;
import java.util.List;

public final class JsonNode<T> {
    private final JsonReader<?> reader;
    private final Gt<T> gt;
    private final T objField;
    private final List<T> listField;
    private int index;
    private JsonReaderState state;
    private String key;

    public JsonNode(JsonReader<?> reader, Class<T> clazz, boolean asList) {
        this.reader = reader;
        this.gt = Gt.of(clazz);
        if(asList) {
            this.index = Constants.ZERO;
            this.objField = null;
            this.listField = new ArrayList<>();
        }else {
            this.index = Integer.MIN_VALUE;
            this.objField = gt.constructor().get();
            this.listField = null;
        }
    }

    public JsonReader<?> getReader() {
        return reader;
    }

    public Gt<T> getGt() {
        return gt;
    }

    public T getObjField() {
        return objField;
    }

    public List<T> getListField() {
        return listField;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public JsonReaderState getState() {
        return state;
    }

    public void setState(JsonReaderState state) {
        this.state = state;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
