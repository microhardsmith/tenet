package cn.zorcc.common.network;

import java.util.function.Supplier;

public record ListenerTask(
        Supplier<Encoder> encoderSupplier,
        Supplier<Decoder> decoderSupplier,
        Supplier<Handler> handlerSupplier,
        Provider provider,
        Loc loc,
        Socket socket,
        SocketConfig socketConfig
) {

}
