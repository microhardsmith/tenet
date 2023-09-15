package cn.zorcc.common.beans;

import java.util.List;

public record Alpha(
        int primitiveInt,
        Integer normalInt,
        int[] intArray,
        Integer[] integerArray,
        String str,
        List<String> list,
        List<int[]> intArrayList,
        List<Integer[]> integerArrayList
) {
}
