package cn.zorcc.log;

import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.common.network.http.HttpReq;
import cn.zorcc.common.network.http.HttpRes;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class HttpTestHandler implements Handler {
    private static final byte[] body = """
            {
                "hello" : "world"
            }
            """.getBytes(StandardCharsets.UTF_8);
    private static final ZoneId gmt = ZoneId.of("GMT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(gmt);
    @Override
    public void onConnected(Channel channel) {
        log.info("Http connection established");
    }

    @Override
    public void onRecv(Channel channel, Object data) {
        if(data instanceof HttpReq httpReq) {
            HttpRes httpRes = new HttpRes();
            Map<String, String> headers = httpRes.getHeaders();
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("Content-Length", String.valueOf(body.length));
            headers.put("Date", formatter.format(ZonedDateTime.now(gmt)));
            httpRes.setData(body);
            channel.send(httpRes);
        }
    }

    @Override
    public void onClose(Channel channel) {
        log.info("Http connection closed");
    }
}
