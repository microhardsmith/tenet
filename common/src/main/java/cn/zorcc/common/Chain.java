package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Helper singleton class for initializing multiple components and registering shutdownHooks
 */
public enum Chain {
    INSTANCE;

    private static final Logger log = new Logger(Chain.class);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final List<LifeCycle> list = new ArrayList<>();
    private final Lock lock = new ReentrantLock();

    public static Chain chain() {
        return INSTANCE;
    }

    /**
     *   Add a component to current chain
     */
    public void add(LifeCycle component) {
        if(!started.get()) {
            lock.lock();
            try {
                list.add(component);
            }finally {
                lock.unlock();
            }
        }else {
            throw new FrameworkException(ExceptionType.CONTEXT, "Couldn't add component after started");
        }
    }

    /**
     *   Start all the components in the chain and register their shutdown() methods for exiting
     */
    public void run() {
        if(started.compareAndSet(false, true)) {
            try{
                for(int index = 0; index < list.size(); index++) {
                    try{
                        list.get(index).init();
                    }catch (FrameworkException e) {
                        log.error("Err caught when initializing component, exiting application now", e);
                        for(int i = index; i >= 0; i--) {
                            list.get(index).exit();
                        }
                    }
                }
            }catch (InterruptedException e) {
                throw new FrameworkException(ExceptionType.CONTEXT, "Shutdown interrupted", e);
            }
            Runtime.getRuntime().addShutdownHook(ThreadUtil.virtual("Exit", () -> {
                try{
                    for(int index = list.size(); index > 0; index--) {
                        list.get(index - 1).exit();
                    }
                }catch (InterruptedException e) {
                    throw new FrameworkException(ExceptionType.CONTEXT, "Shutdown interrupted", e);
                }
            }));
        }else {
            throw new FrameworkException(ExceptionType.CONTEXT, "Already started");
        }
    }
}
