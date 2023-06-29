package cn.zorcc.common.sql;

import cn.zorcc.common.anno.Col;
import cn.zorcc.common.anno.Del;
import cn.zorcc.common.anno.Id;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *   Default design specification for database tables
 */
@Data
public abstract class Base implements Filler {

    /**
     *   Auto increment id column
     */
    @Id()
    private Long id;

    /**
     *   Create time column, will be auto-generated when performing insert operation
     */
    @Col(value = "created_at")
    private LocalDateTime createdAt;

    /**
     *   Modify time column, will be auto-generated when performing update operation
     *   Note that if using logical-delete, then this column will also be updated when performing delete operation
     */
    @Col(value = "modified_at")
    private LocalDateTime modifiedAt;

    /**
     *   Logical delete field, false when exist, true when deleted
     */
    @Del(value = "deleted")
    private Boolean deleted;

    @Override
    public void onInsert() {
        if (deleted == null) {
            setDeleted(Boolean.FALSE);
        }
        if (createdAt == null) {
            setCreatedAt(LocalDateTime.now());
        }
    }

    @Override
    public void onUpdate() {
        setModifiedAt(LocalDateTime.now());
    }
}
