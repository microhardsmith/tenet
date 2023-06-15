package cn.zorcc.orm.common;

public interface Filler {
    /**
     *   Auto filler operation applied when performing insert
     */
    void onInsert();

    /**
     *   Auto filler operation applied when performing update
     */
    void onUpdate();
}
