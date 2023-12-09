package cn.zorcc.common.network;

import cn.zorcc.common.network.api.Decoder;
import cn.zorcc.common.network.api.Encoder;
import cn.zorcc.common.network.api.Handler;
import cn.zorcc.common.network.api.Provider;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public record Listener (
        Supplier<Encoder> encoderSupplier,
        Supplier<Decoder> decoderSupplier,
        Supplier<Handler> handlerSupplier,
        Provider provider,
        Loc loc,
        AtomicInteger counter,
        Socket socket,
        SocketConfig socketConfig
) {

}
