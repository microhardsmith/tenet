package cc.zorcc.tenet.serde;

/**
 *   We could review each field in a class as a column, where we could modify it as we want
 *   This class is not a record, because we don't want to expose the entity within it
 */
public final class Col<T> {
    private final String name;
    private final RecursiveType recursiveType;
    private final TagMappingFunc tagMappingFunc;
    private final AssignerFunc<T> assignerFunc;
    private final FetcherFunc<T> fetcherFunc;

    public Col(String name, RecursiveType recursiveType, TagMappingFunc tagMappingFunc, AssignerFunc<T> assignerFunc, FetcherFunc<T> fetcherFunc) {
        this.name = name;
        this.recursiveType = recursiveType;
        this.tagMappingFunc = tagMappingFunc;
        this.assignerFunc = assignerFunc;
        this.fetcherFunc = fetcherFunc;
    }

    public String name() {
        return name;
    }

    public RecursiveType recursiveType() {
        return recursiveType;
    }

    public String tag(String tagName) {
        return tagMappingFunc.map(tagName);
    }

    public void assign(Builder<T> builder, Object value) {
        assignerFunc.assign(builder, value);
    }

    public Object fetch(T instance) {
        return fetcherFunc.fetch(instance);
    }
}
