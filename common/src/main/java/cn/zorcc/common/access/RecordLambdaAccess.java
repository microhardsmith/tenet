package cn.zorcc.common.access;

public class RecordLambdaAccess<T> implements Access<T> {
    @Override
    public Setter<T> createSetter() {
        return null;
    }

    @Override
    public Getter<T> createGetter() {
        return null;
    }
}
