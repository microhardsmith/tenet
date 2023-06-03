package cn.zorcc.common;

public record Pair<K, V>(K k, V v) {

    public static <T1, T2> Pair<T1, T2> of(T1 t1, T2 t2) {
        return new Pair<>(t1, t2);
    }
}
