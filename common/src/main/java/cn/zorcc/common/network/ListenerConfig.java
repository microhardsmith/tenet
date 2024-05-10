package cn.zorcc.common.network;

import cn.zorcc.common.serde.Serde;

import java.util.function.Supplier;

@Serde
public final class ListenerConfig {

    /**
     *   How master should provide encoder for server-side application, must be non-null
     */
    private Supplier<Encoder> encoderSupplier;

    /**
     *   How master should provide decoder for server-side application, must be non-null
     */
    private Supplier<Decoder> decoderSupplier;

    /**
     *   How master should provide handler for server-side application, must be non-null
     */
    private Supplier<Handler> handlerSupplier;

    /**
     *   Target provider for server-side application, Net.tcpProvider() and Net.sslProvider() are recommended to use
     */
    private Provider provider;
    /**
     *   Target host:port to bind and listen
     *   For Ipv6 server, it's recommended to listen on "::"
     *   For Ipv4 server, it's recommended to listen on "0.0.0.0"
     *   You could always set loc's ip to null or an empty string to use system default ip address, which is best for most applications
     */
    private Loc loc;

    /**
     * Default socket options for server-side socket
     */
    private SocketConfig socketConfig = new SocketConfig();

    public Supplier<Encoder> getEncoderSupplier() {
        return encoderSupplier;
    }

    public void setEncoderSupplier(Supplier<Encoder> encoderSupplier) {
        this.encoderSupplier = encoderSupplier;
    }

    public Supplier<Decoder> getDecoderSupplier() {
        return decoderSupplier;
    }

    public void setDecoderSupplier(Supplier<Decoder> decoderSupplier) {
        this.decoderSupplier = decoderSupplier;
    }

    public Supplier<Handler> getHandlerSupplier() {
        return handlerSupplier;
    }

    public void setHandlerSupplier(Supplier<Handler> handlerSupplier) {
        this.handlerSupplier = handlerSupplier;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Loc getLoc() {
        return loc;
    }

    public void setLoc(Loc loc) {
        this.loc = loc;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }
}
