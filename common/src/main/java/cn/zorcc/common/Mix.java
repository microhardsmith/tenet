package cn.zorcc.common;

/**
 *   Representing a mix of some objects
 */
public record Mix(
        Object[] objects
) {
    public static Mix of(Object... someObjects) {
        return new Mix(someObjects);
    }
}
