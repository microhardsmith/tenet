package cn.zorcc.common.database;

import java.time.LocalDateTime;

/**
 *   Default design specification for database tables
 */
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
     *   Logical delete field, false when the record does exist, true when the record was deleted
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

    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime modifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Boolean deleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
