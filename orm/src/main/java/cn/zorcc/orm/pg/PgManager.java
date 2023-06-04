package cn.zorcc.orm.pg;

import cn.zorcc.common.LifeCycle;
import cn.zorcc.common.network.Net;
import cn.zorcc.common.pojo.Loc;
import cn.zorcc.orm.PgConfig;
import lombok.extern.slf4j.Slf4j;

/**
 *   The main postgresql manager for handling all incoming msg
 */
@Slf4j
public final class PgManager implements LifeCycle {
    private final Net net;
    private final PgConfig pgConfig;
    public PgManager(Net net, PgConfig pgConfig) {
        this.net = net;
        this.pgConfig = pgConfig;
    }

    public void registerConn() {

    }

    @Override
    public void init() {
        Loc loc = pgConfig.getLoc();
        new PgEncoder();
        new PgDecoder();
        new PgHandler();
        // net.connect(loc, );
    }

    @Override
    public void shutdown() {

    }
}
