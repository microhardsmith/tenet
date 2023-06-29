package cn.zorcc.common.sql;

/**
 *   Define auto filler operation when using Mapper
 */
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
