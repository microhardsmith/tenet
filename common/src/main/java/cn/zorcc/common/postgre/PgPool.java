package cn.zorcc.common.postgre;

import cn.zorcc.common.network.api.Channel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class PgPool {
    private final Lock lock = new ReentrantLock();
    private final Deque<Channel> deque = new ArrayDeque<>();
}
