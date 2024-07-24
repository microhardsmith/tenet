package cc.zorcc.tenet.serde;

@FunctionalInterface
public interface AssignerFunc<T> {
    /**
     *   Assigning value to the builder instance
     */
    void assign(Builder<T> builder, Object value);
}
