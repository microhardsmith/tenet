package cn.zorcc.common.beans;

import java.util.List;
import java.util.Map;

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
}
