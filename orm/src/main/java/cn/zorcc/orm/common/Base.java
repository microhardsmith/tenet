package cn.zorcc.orm.common;

import cn.zorcc.common.anno.Col;
import cn.zorcc.common.anno.Del;
import cn.zorcc.common.anno.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Base implements Filler {

    @Id()
    private Long id;

    @Col(value = "created_at")
    private LocalDateTime createdAt;

    @Col(value = "modified_at")
    private LocalDateTime modifiedAt;

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
