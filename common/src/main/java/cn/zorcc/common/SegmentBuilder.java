package cn.zorcc.common;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.util.List;

/**
 *   可扩容native byte数组,非线程安全
 *   初始分配的内存为全局作用域,在jvm生命周期中不会释放,扩容的内存可被gc
 */
public final class SegmentBuilder {
    /**
     *  当前内存作用域
     */
    private final Arena arena;
    /**
     *  初始分配的segment
     */
    private final MemorySegment initialSegment;
    /**
     *  当前segment
     */
    private MemorySegment segment;
    /**
     *  当前segment的写索引值
     */
    private long writeIndex;
    /**
     *  当前segment的读索引值
     */
    private long readIndex;

    /**
     *   构建生命周期确定的SegmentBuilder
     */
    public SegmentBuilder(Arena arena, int size) {
        this.arena = arena;
        this.initialSegment = this.segment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        this.writeIndex = this.readIndex = 0L;
    }

    /**
     *   构建生命周期与jvm绑定的SegmentBuilder
     */
    public SegmentBuilder(int size) {
        this.arena = null;
        this.initialSegment = this.segment = MemorySegment.allocateNative(size, SegmentScope.global());
        this.writeIndex = this.readIndex = 0L;
    }

    /**
     *   获取当前SegmentBuilder内存作用域
     */
    public Arena arena() {
        return arena;
    }

    /**
     *  向当前segment中添加单个字节
     */
    public SegmentBuilder append(byte b) {
        long nextIndex = writeIndex + 1;
        if(nextIndex >= segment.byteSize()) {
            resize(nextIndex);
        }
        segment.set(ValueLayout.JAVA_BYTE, writeIndex, b);
        writeIndex = nextIndex;
        return this;
    }

    /**
     *  向当前segment中添加int
     */
    public SegmentBuilder append(int i) {
        long nextIndex = writeIndex + 4;
        if(nextIndex >= segment.byteSize()) {
            resize(nextIndex);
        }
        segment.set(ValueLayout.JAVA_INT, writeIndex, i);
        writeIndex = nextIndex;
        return this;
    }

    /**
     *  向当前segment中添加long
     */
    public SegmentBuilder append(long l) {
        long nextIndex = writeIndex + 8;
        if(nextIndex >= segment.byteSize()) {
            resize(nextIndex);
        }
        segment.set(ValueLayout.JAVA_LONG, writeIndex, l);
        writeIndex = nextIndex;
        return this;
    }

    /**
     *  向当前segment中添加target中的内存内容
     */
    public SegmentBuilder append(MemorySegment target) {
        long len = target.byteSize();
        long nextIndex = writeIndex + len;
        if(nextIndex >= segment.byteSize()) {
            resize(nextIndex);
        }
        MemorySegment.copy(target, 0, segment, writeIndex, len);
        writeIndex = nextIndex;
        return this;
    }

    /**
     *  向当前segment中添加内容,使用ansi color格式包裹填充内容
     */
    public SegmentBuilder append(MemorySegment segment, MemorySegment color) {
        if(color != null) {
            return append(Constants.ANSI_PREFIX).append(color).append(segment).append(Constants.ANSI_SUFFIX);
        }else {
            return append(segment);
        }
    }

    /**
     *  向当前segment中添加内容,如果长度小于width则填充空格,长度小于width则截断
     */
    public SegmentBuilder append(MemorySegment target, long width) {
        long len = Math.min(target.byteSize(), width);
        long nextIndex = writeIndex + width;
        if(nextIndex >= segment.byteSize()) {
            resize(nextIndex);
        }
        MemorySegment.copy(target, 0, segment, writeIndex, len);
        if(width > len) {
            long l = width - len;
            for(long i = 0; i < l; i++) {
                this.segment.set(ValueLayout.JAVA_BYTE, writeIndex + len + i, Constants.b2);
            }
        }
        writeIndex = writeIndex + width;
        return this;
    }

    /**
     *  向当前segment中添加内容,使用ansi color格式包裹填充内容,如果长度小于width则填充空格,长度小于width则截断
     */
    public SegmentBuilder append(MemorySegment target, MemorySegment color, long width) {
        if(width <= 0) {
            return append(target, color);
        }else {
            return append(Constants.ANSI_PREFIX).append(color).append(target, width).append(Constants.ANSI_SUFFIX);
        }
    }

    /**
     *  向当前segment中添加可格式化内容,将{}替换为list中的元素,返回添加的信息段
     */
    public MemorySegment appendWithArgs(MemorySegment target, List<MemorySegment> list) {
        if(list == null || list.isEmpty()) {
            final long currentIndex = writeIndex;
            append(target);
            return segment.asSlice(currentIndex, writeIndex - currentIndex);
        }else {
            long len = target.byteSize();
            long roughIndex = writeIndex + len + list.stream().map(MemorySegment::byteSize).reduce(0L, Long::sum);
            if(roughIndex >= segment.byteSize()) {
                resize(roughIndex);
            }
            int argIndex = 0;
            long segmentIndex = writeIndex;
            for(long i = 0; i < len; i++) {
                byte b = target.get(ValueLayout.JAVA_BYTE, i);
                if(b == Constants.b5 && i + 1 < len && target.get(ValueLayout.JAVA_BYTE, i + 1) == Constants.b6) {
                    // reaching {}, need to parse the arg
                    MemorySegment m = list.get(argIndex++);
                    long argSize = m.byteSize();
                    MemorySegment.copy(m, 0, segment, segmentIndex, argSize);
                    segmentIndex += argSize;
                    i++; // escape "{}"
                }else {
                    segment.set(ValueLayout.JAVA_BYTE, segmentIndex++, b);
                }
            }
            MemorySegment result = segment.asSlice(writeIndex, segmentIndex);
            writeIndex = segmentIndex;
            return result;
        }
    }

    /**
     *   手动设置当前readIndex
     */
    public void setReadIndex(long readIndex) {
        this.readIndex = readIndex;
    }

    /**
     *  返回当前segment,按照readIndex和writeIndex进行划分
     */
    public MemorySegment segment() {
        if(readIndex == 0L && writeIndex == segment.byteSize()) {
            return segment;
        }else {
            return segment.asSlice(readIndex, writeIndex);
        }
    }

    /**
     *  对当前memorySegment进行扩容,新分配的内存段scope为auto
     */
    private void resize(long nextIndex) {
        long newSize = segment.byteSize();
        while (newSize > 0 && newSize < nextIndex) {
            // 按照每次2倍的基数进行扩容
            newSize = newSize << 1;
        }
        if(newSize < 0) {
            throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
        }
        MemorySegment newSegment = arena == null ? MemorySegment.allocateNative(newSize, SegmentScope.auto()) : arena.allocateArray(ValueLayout.JAVA_BYTE, newSize);
        newSegment.copyFrom(segment);
        segment =  newSegment;
    }

    /**
     *  重置当前memorySegment为初始分配的内存段,不会清除内存中已存在的数据
     */
    public void reset() {
        if(!segment.equals(initialSegment)) {
            segment = initialSegment;
        }
        writeIndex = 0L;
    }
}
