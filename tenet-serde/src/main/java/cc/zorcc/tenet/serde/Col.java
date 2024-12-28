package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 *   We could review each field in a class as a column, where we could modify it as we want
 *   This class is not a record, because we don't want to expose the entity within it
 */
public final class Col<T> {
    private final String name;
    private final List<Form> forms;
    private final TagMappingFunc tagMappingFunc;
    private final AssignerFunc<T> assignerFunc;
    private final FetcherFunc<T> fetcherFunc;

    public Col(@NonNull String name,
               @NonNull List<Form> forms,
               @NonNull TagMappingFunc tagMappingFunc,
               @NonNull AssignerFunc<T> assignerFunc,
               @NonNull FetcherFunc<T> fetcherFunc) {
        this.name = name;
        this.forms = forms;
        this.tagMappingFunc = tagMappingFunc;
        this.assignerFunc = assignerFunc;
        this.fetcherFunc = fetcherFunc;
    }

    public @NonNull String name() {
        return name;
    }

    public @NonNull List<Form> forms() {
        return forms;
    }

    public @Nullable String tag(@NonNull String tagName) {
        return tagMappingFunc.map(tagName);
    }

    public void assign(@NonNull Builder<T> builder, @Nullable Object value) {
        assignerFunc.assign(builder, value);
    }

    public @Nullable Object fetch(@NonNull T instance) {
        return fetcherFunc.fetch(instance);
    }
}
