package cn.zorcc.orm.pg;

import java.util.List;

public record PgDataRowMsg(
        short len,
        List<byte[]> data
) {
}
