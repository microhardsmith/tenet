package cn.zorcc.common;

/**
 *   TODO refactor
 */
public record OldPair<K, V>(K k, V v) {

    public static <T1, T2> OldPair<T1, T2> of(T1 t1, T2 t2) {
        return new OldPair<>(t1, t2);
    }
}
