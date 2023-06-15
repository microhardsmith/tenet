package cn.zorcc.orm.backend;

import java.util.List;

public record PgDataRowMsg(
        short len,
        List<byte[]> data
) {
}
