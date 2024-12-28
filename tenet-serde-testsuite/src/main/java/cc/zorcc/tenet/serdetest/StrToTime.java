package cc.zorcc.tenet.serdetest;

import cc.zorcc.tenet.serde.Transform;
import cc.zorcc.tenet.serde.Transformer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Transform("strToTime")
public final class StrToTime implements Transformer<String, LocalDateTime> {

    @Override
    public LocalDateTime from(String s) {
        return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public String to(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
