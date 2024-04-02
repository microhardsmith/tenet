package cn.zorcc.common.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReflectionTest extends JmhTest {
//    @Param({"10", "100", "1000", "10000"})
//    private int size = 1;

    private static final MethodHandle constructorHandle;
    private static final MethodHandle iSetter;
    private static final MethodHandle iGetter;
    static {
        try{
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType constructorType = MethodType.methodType(void.class);
            constructorHandle = lookup.findConstructor(Test.class, constructorType);
            iSetter = lookup.findSetter(Test.class, "integer", int.class);
            iGetter = lookup.findGetter(Test.class, "integer", int.class);
        }catch (Throwable e) {
            throw new UnknownError();
        }
    }

    private Supplier<Test> constructor;
    private BiConsumer<Object, Object> setConsumer;
    private Function<Test, Integer> getFunction;
    private Constructor<Test> c;
    private Method setter;
    private Method getter;

    @Setup
    public void setup() throws Throwable {
        this.c = Test.class.getDeclaredConstructor();
        this.setter = Test.class.getDeclaredMethod("setInteger", int.class);
        this.getter = Test.class.getDeclaredMethod("getInteger");
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(ReflectionTest.class, MethodHandles.lookup());
        this.constructor = lambdaGenerateConstructor(lookup);
        this.setConsumer = lambdaGenerateSetter(lookup);
        this.getFunction = lambdaGenerateGetter(lookup);
    }

    @SuppressWarnings("unchecked")
    private Supplier<Test> lambdaGenerateConstructor(MethodHandles.Lookup lookup) throws Throwable {
        MethodHandle cmh = lookup.findConstructor(Test.class, MethodType.methodType(void.class));
        CallSite c1 = LambdaMetafactory.metafactory(lookup,
                "get",
                MethodType.methodType(Supplier.class),
                MethodType.methodType(Object.class), cmh, MethodType.methodType(Test.class));
        return (Supplier<Test>) c1.getTarget().invokeExact();
    }

    @SuppressWarnings("unchecked")
    private BiConsumer<Object, Object> lambdaGenerateSetter(MethodHandles.Lookup lookup) throws Throwable {
        MethodHandle setHandle = lookup.findVirtual(Test.class, "setInteger", MethodType.methodType(void.class, int.class));
        CallSite callSite = LambdaMetafactory.metafactory(lookup,
                "accept",
                MethodType.methodType(BiConsumer.class),
                MethodType.methodType(void.class, Object.class, Object.class),
                setHandle,
                MethodType.methodType(void.class, Test.class, Integer.class));
        return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
    }

    @SuppressWarnings("unchecked")
    private Function<Test, Integer> lambdaGenerateGetter(MethodHandles.Lookup lookup) throws Throwable {
        MethodHandle getHandle = lookup.findVirtual(Test.class, "getInteger", MethodType.methodType(int.class));
        CallSite getSite = LambdaMetafactory.metafactory(
                lookup,
                "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                getHandle,
                MethodType.methodType(Integer.class, Test.class)
        );
        return (Function<Test, Integer>) getSite.getTarget().invokeExact();
    }

    @SuppressWarnings("unchecked")
    private Supplier<Test> lambdaGenerateConstructor1(MethodHandles.Lookup lookup) throws Throwable {
        MethodHandle cmh = lookup.unreflectConstructor(Test.class.getDeclaredConstructor());
        CallSite c1 = LambdaMetafactory.metafactory(lookup,
                "get",
                MethodType.methodType(Supplier.class),
                MethodType.methodType(Object.class), cmh, MethodType.methodType(Test.class));
        return (Supplier<Test>) c1.getTarget().invokeExact();
    }

    @SuppressWarnings("unchecked")
    private BiConsumer<Object, Object> lambdaGenerateSetter1(MethodHandles.Lookup lookup) throws Throwable {
        MethodHandle setHandle = lookup.unreflect(Test.class.getDeclaredMethod("setInteger", int.class));
        CallSite callSite = LambdaMetafactory.metafactory(lookup,
                "accept",
                MethodType.methodType(BiConsumer.class),
                MethodType.methodType(void.class, Object.class, Object.class),
                setHandle,
                MethodType.methodType(void.class, Test.class, Integer.class));
        return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
    }

    @SuppressWarnings("unchecked")
    private Function<Test, Integer> lambdaGenerateGetter1(MethodHandles.Lookup lookup) throws Throwable {
        MethodHandle getHandle = lookup.unreflect(Test.class.getDeclaredMethod("getInteger"));
        CallSite getSite = LambdaMetafactory.metafactory(
                lookup,
                "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                getHandle,
                MethodType.methodType(Integer.class, Test.class)
        );
        return (Function<Test, Integer>) getSite.getTarget().invokeExact();
    }

    static class Test {
        private int integer;

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }
    }

    //@Benchmark
    public void testDirectCall(Blackhole bh) {
//        for(int i = 0; i < size; i++) {
//            Test test = new Test();
//            bh.consume(test);
//            test.setInteger(i);
//            bh.consume(test.getInteger());
//        }
    }

    //@Benchmark
    public void testNormalReflection(Blackhole bh) {
//        try{
//            for(int i = 0; i < size; i++) {
//                Test test = c.newInstance();
//                bh.consume(test);
//                setter.invoke(test, i);
//                int integer = (int) getter.invoke(test);
//                bh.consume(integer);
//            }
//        }catch (Throwable e) {
//            throw new UnknownError();
//        }
    }

    //@Benchmark
    public void testMethodHandleReflection(Blackhole bh) {
//        try{
//            for(int i = 0; i < size; i++) {
//                Test test = (Test) constructorHandle.invokeExact();
//                bh.consume(test);
//                iSetter.invokeExact(test, i);
//                int integer = (int) iGetter.invokeExact(test);
//                bh.consume(integer);
//            }
//        }catch (Throwable e) {
//            throw new UnknownError();
//        }
    }

    //@Benchmark
    public void testLambda(Blackhole bh) {
//        for(int i = 0; i < size; i++) {
//            Test test = constructor.get();
//            bh.consume(test);
//            setConsumer.accept(test, i);
//            int integer = getFunction.apply(test);
//            bh.consume(integer);
//        }
    }

    @Benchmark
    public void testLambdaGeneration(Blackhole bh) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(ReflectionTest.class, MethodHandles.lookup());
        bh.consume(lambdaGenerateConstructor(lookup));
        bh.consume(lambdaGenerateSetter(lookup));
        bh.consume(lambdaGenerateGetter(lookup));
    }

    @Benchmark
    public void testLambdaGeneration1(Blackhole bh) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(ReflectionTest.class, MethodHandles.lookup());
        bh.consume(lambdaGenerateConstructor1(lookup));
        bh.consume(lambdaGenerateSetter1(lookup));
        bh.consume(lambdaGenerateGetter1(lookup));
    }

    public static void main(String[] args) throws Throwable {
        runTest(ReflectionTest.class);
    }
}
