package cc.zorcc.tenet.core;

/**
 *   SearchedResult is a result typing for ReadBuffer search mechanism
 */
public sealed interface SearchedResult {
    /**
     *   Empty searched result
     */
    SearchedResult EMPTY = new Empty();

    /**
     *   Creating a searchedResult with value present
     */
    static SearchedResult of(byte value) {
        return new Value(value);
    }

    /**
     *   Creating a searchedResult with empty value
     */
    static SearchedResult empty() {
        return EMPTY;
    }

    record Value(byte value) implements SearchedResult {

    }

    record Empty() implements SearchedResult {

    }
}
