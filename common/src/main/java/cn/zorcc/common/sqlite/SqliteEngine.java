package cn.zorcc.common.sqlite;

import cn.zorcc.common.AbstractLifeCycle;
import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Consumer;

public final class SqliteEngine extends AbstractLifeCycle {
    private final List<Thread> readers ;
    private final Thread writer;
    private final TransferQueue<SqliteMsg> readerQueue = new LinkedTransferQueue<>();
    private final TransferQueue<SqliteMsg> writerQueue = new LinkedTransferQueue<>();
    public SqliteEngine(SqliteConfig config) {
        List<Thread> readers = new ArrayList<>();
        for(int i = 0; i < config.getReaders(); i++) {
            readers.add(createReaderThread(config, readers.size()));
        }
        this.readers = Collections.unmodifiableList(readers);
        this.writer = createWriterThread(config);
    }

    private Thread createReaderThread(SqliteConfig config, int sequence) {
        return Thread.ofPlatform().name("sqlite-reader-" + sequence).unstarted(() -> {
            try(SqliteConn conn = new SqliteConn(config.getPath())) {
                List<Consumer<SqliteMsg>> consumers = new ArrayList<>();
                if(config.isEnableDiscovery()) {

                }
                loop(consumers, readerQueue);
            }catch (InterruptedException e) {
                throw new FrameworkException(ExceptionType.MINT, Constants.UNREACHED);
            }
        });
    }

    private Thread createWriterThread(SqliteConfig config) {
        return Thread.ofPlatform().name("sqlite-writer").unstarted(() -> {
            try(SqliteConn conn = new SqliteConn(config.getPath())) {
                List<Consumer<SqliteMsg>> consumers = new ArrayList<>();
                if(config.isEnableDiscovery()) {

                }
                loop(consumers, writerQueue);
            }catch (InterruptedException e) {
                throw new FrameworkException(ExceptionType.MINT, Constants.UNREACHED);
            }
        });
    }

    private void loop(List<Consumer<SqliteMsg>> consumers, TransferQueue<SqliteMsg> queue) throws InterruptedException {
        for( ; ; ) {
            final SqliteMsg msg = queue.take();
            consumers.forEach(sqliteMsgConsumer -> sqliteMsgConsumer.accept(msg));

        }
    }

    @Override
    protected void doInit() {
        writer.start();
        for (Thread reader : readers) {
            reader.start();
        }
    }

    @Override
    protected void doExit() throws InterruptedException {
        SqliteMsg shutdownMsg = new SqliteMsg(SqliteMsgType.Shutdown, null, null);
        if (!writerQueue.offer(shutdownMsg)) {
            throw new FrameworkException(ExceptionType.SQLITE, Constants.UNREACHED);
        }
        for(int i = 0; i < readers.size(); i++) {
            if (!readerQueue.offer(shutdownMsg)) {
                throw new FrameworkException(ExceptionType.SQLITE, Constants.UNREACHED);
            }
        }
        writer.join();
        for (Thread reader : readers) {
            reader.join();
        }
    }

}
