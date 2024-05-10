package cn.zorcc.common.serde;

@Serde
public record RecordBean(
        @Attr(values = {"default:2", "json:str"})
        int a,
        String b
) {
}
