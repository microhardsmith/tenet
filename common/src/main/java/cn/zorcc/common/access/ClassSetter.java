package cn.zorcc.common.access;

public record ClassSetter<T>(

) implements Setter<T> {
    @Override
    public void setValue(String key, Object value) {

    }

    @Override
    public T construct() {
        return null;
    }
}
