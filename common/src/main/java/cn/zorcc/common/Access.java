package cn.zorcc.common;

import java.lang.invoke.MethodHandle;

public record Access(String name,
                     Class<?> type,
                     MethodHandle getter,
                     MethodHandle setter) {
}
