package cn.zorcc.common.json;

public class JsonStartMsg {
    private Class<?> type;
    private boolean array;

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }
}
