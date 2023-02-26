package cn.zorcc.log;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.util.NativeUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *   可扩容native byte数组,非线程安全
 *   初始分配的内存为全局作用域，在jvm生命周期中不会释放，扩容的内存可被gc
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
     *  初始分配的segment的bytesize
     */
    private final long initialSize;
    /**
     *  当前segment
     */
    private MemorySegment segment;
    /**
     *  当前segment的bytesize
     */
    private long size;
    /**
     *  当前segment的索引值
     */
    private long index;

    public SegmentBuilder(Arena arena, int size) {
        this.arena = arena;
        this.initialSize = this.size = size;
        this.initialSegment = this.segment = arena.allocateArray(ValueLayout.JAVA_BYTE, size);
        this.index = 0;
    }

    /**
     *  向当前segment中添加单个字节
     */
    public SegmentBuilder append(byte b) {
        if(index + 1 >= size) {
            resize();
        }
        segment.set(ValueLayout.JAVA_BYTE, index, b);
        index = index + 1;
        return this;
    }

    /**
     *  向当前segment中添加内容
     */
    public SegmentBuilder append(MemorySegment target) {
        long len = target.byteSize();
        long nextIndex = index + len;
        if(nextIndex >= size) {
            resize(nextIndex);
        }
        MemorySegment.copy(target, 0, segment, index, len);
        index = nextIndex;
        // DEBUG
        NativeUtil.test(segment, index, "Append: ");
        return this;
    }

    /**
     *  向当前segment中添加内容,使用ansi color格式包裹填充内容
     */
    public SegmentBuilder append(MemorySegment segment, MemorySegment color) {
        return append(Constants.ANSI_PREFIX).append(color).append(segment).append(Constants.ANSI_SUFFIX);
    }

    /**
     *  向当前segment中添加内容，如果长度小于width则填充空格，长度小于width则截断
     */
    public SegmentBuilder append(MemorySegment target, long width) {
        NativeUtil.test(segment, index, "segment: ");
        NativeUtil.test(target, target.byteSize(), "target: ");
        long len = target.byteSize();
        if(width == len) {
            return append(target);
        }
        long nextIndex = index + Math.min(len, width);
        if(nextIndex >= size) {
            resize(nextIndex);
        }
        MemorySegment.copy(target, 0, segment, index, width);
        index = nextIndex;
        if(width > len) {
            long l = width - len;
            for(long i = 0; i < l; i++) {
                this.segment.set(ValueLayout.JAVA_BYTE, index + i, Constants.b2);
            }
            index = index + l;
        }
        return this;
    }

    /**
     *  向当前segment中添加内容,使用ansi color格式包裹填充内容，如果长度小于width则填充空格，长度小于width则截断
     */
    public SegmentBuilder append(MemorySegment target, MemorySegment color, long width) {
        if(width <= 0) {
            return append(target, color);
        }else {
            return append(Constants.ANSI_PREFIX).append(color).append(target, width).append(Constants.ANSI_SUFFIX);
        }
    }

    /**
     *  向当前segment中添加可格式化内容，将{}替换为list中的元素，返回添加的信息段
     */
    public MemorySegment append(MemorySegment target, List<MemorySegment> list) {
        if(list == null || list.isEmpty()) {
            final long currentIndex = index;
            append(target);
            return segment.asSlice(currentIndex, index - currentIndex);
        }else {
            long len = target.byteSize();
            long roughIndex = index + len + list.stream().map(MemorySegment::byteSize).reduce(0L, Long::sum);
            if(roughIndex >= size) {
                resize(roughIndex);
            }
            int argIndex = 0;
            long segmentIndex = index;
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
            MemorySegment result = segment.asSlice(index, segmentIndex);
            index = segmentIndex;
            return result;
        }
    }

    /**
     *  获取当前索引值
     */
    public long index() {
        return index;
    }

    /**
     *  返回当前segment
     */
    public MemorySegment segment() {
        return segment;
    }

    /**
     *  默认扩容至当前长度的2倍
     */
    private void resize() {
        resize(size + 1);
    }

    /**
     *  对当前memorySegment进行扩容,新分配的内存段scope为auto
     */
    private void resize(long nextIndex) {
        long newSize = size;
        while (newSize > 0 && newSize < nextIndex) {
            // 按照每次2倍的基数进行扩容
            newSize = newSize << 1;
        }
        if(newSize < 0) {
            throw new FrameworkException(ExceptionType.NATIVE, "MemorySize overflow");
        }
        MemorySegment newSegment = arena.allocateArray(ValueLayout.JAVA_BYTE, newSize);
        newSegment.copyFrom(segment);
        size = newSegment.byteSize();
        segment =  newSegment;
    }

    /**
     *  重置当前memorySegment为初始分配的内存段,不会清除内存中已存在的数据
     */
    public void reset() {
        if(!segment.equals(initialSegment)) {
            segment = initialSegment;
            size = initialSize;
        }
        index = 0L;
    }
}
