package cn.zorcc.common.network;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import lombok.Data;

@Data
public class MuxConfig {
    /**
     *  Backlog parameter
     */
    private Integer backlog = 64;
    /**
     *  Max length for a single mux call
     *  Note that for each mux, maxEvents * readBufferSize native memory were pre-allocated
     */
    private Integer maxEvents = 16;
    /**
     *  Max blocking time in milliseconds for a mux call
     */
    private Integer muxTimeout = 25;

    /**
     *   Validate current MuxConfig settings
     */
    public void validate() {
        if(backlog < 16) {
            throw new FrameworkException(ExceptionType.NETWORK, "BackLog parameter too small");
        }
        if(maxEvents < 16) {
            throw new FrameworkException(ExceptionType.NETWORK, "MaxEvents parameter too small");
        }
        if(muxTimeout > 200) {
            throw new FrameworkException(ExceptionType.NETWORK, "MuxTimeout parameter too large");
        }
    }
}
