package cn.zorcc.common.network;

import cn.zorcc.common.structure.Loc;

import java.util.function.Supplier;

public final class MasterConfig {
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
     *   You could always set loc's ip to null or a empty string to use system default ip address, which is best for most applications
     */
    private Loc loc;
    /**
     * Max length for a single mux call
     * Note that for each mux, maxEvents * readBufferSize native memory were pre-allocated
     */
    private int maxEvents = 16;
    /**
     * Max blocking time in milliseconds for a mux call
     */
    private int muxTimeout = 25;
    /**
     * Backlog parameter
     */
    private int backlog = 64;
    /**
     * Default socket options for server-side socket
     */
    private SocketOptions socketOptions = new SocketOptions();

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

    public int getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(int maxEvents) {
        this.maxEvents = maxEvents;
    }

    public int getMuxTimeout() {
        return muxTimeout;
    }

    public void setMuxTimeout(int muxTimeout) {
        this.muxTimeout = muxTimeout;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public SocketOptions getSocketOptions() {
        return socketOptions;
    }

    public void setSocketOptions(SocketOptions socketOptions) {
        this.socketOptions = socketOptions;
    }
}
