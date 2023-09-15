package cn.zorcc.common.beans;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Book {
    private Long id;
    private List<String> names;
    private Map<String, List<Integer>> map;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public Map<String, List<Integer>> getMap() {
        return map;
    }

    public void setMap(Map<String, List<Integer>> map) {
        this.map = map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id) && Objects.equals(names, book.names) && Objects.equals(map, book.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, names, map);
    }
}
