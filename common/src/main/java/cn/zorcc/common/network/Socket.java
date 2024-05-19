package cn.zorcc.common.network;

public sealed interface Socket permits Socket.IntSocket, Socket.LongSocket{
    /**
     *   Return the int value of current socket implementation
     */
    int intValue();

    /**
     *   Return the int value of current socket implementation
     */
    long longValue();

    static Socket ofInt(int value) {
        return new IntSocket(value);
    }

    static Socket ofLong(long value) {
        return new LongSocket(value);
    }

    /**
     *   Socket used for linux and macOS
     *   TODO value-based class
     */
    record IntSocket(int value) implements Socket {
        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    /**
     *   Socket used for Windows
     *   TODO value-based class
     */
    record LongSocket(long value) implements Socket {
        @Override
        public int intValue() {
            return Math.toIntExact(value);
        }

        @Override
        public long longValue() {
            return value;
        }
    }
}
