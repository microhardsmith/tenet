package cn.zorcc.common.duckdb;

import java.util.Map;

public final class DuckdbConfig {
    private Map<String, String> options;
    private String path;

    public Map<String, String> getOptions() {
        return options;
    }

    public DuckdbConfig setOptions(Map<String, String> options) {
        this.options = options;
        return this;
    }

    public String getPath() {
        return path;
    }

    public DuckdbConfig setPath(String path) {
        this.path = path;
        return this;
    }
}
