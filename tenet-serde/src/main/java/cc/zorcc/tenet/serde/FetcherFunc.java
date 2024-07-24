package cc.zorcc.tenet.serde;

@FunctionalInterface
public interface FetcherFunc<T> {
    /**
     *   Fetching value from the target instance
     */
    Object fetch(T t);
}
