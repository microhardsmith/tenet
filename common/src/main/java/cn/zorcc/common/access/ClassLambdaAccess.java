package cn.zorcc.common.access;

public class ClassLambdaAccess<T> implements Access<T> {
    public ClassLambdaAccess(Class<T> clazz) {

    }

    @Override
    public Setter<T> createSetter() {
        return null;
    }

    @Override
    public Getter<T> createGetter() {
        return null;
    }
}
