package cn.zorcc.common.access;

public record RecordSetter<T>(

) implements Setter<T> {
    @Override
    public void setValue(String key, Object value) {

    }

    @Override
    public T construct() {
        return null;
    }
}
