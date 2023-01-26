package cn.zorcc.app.http;

import cn.zorcc.app.http.anno.*;
import cn.zorcc.common.Constants;
import cn.zorcc.common.Context;
import cn.zorcc.common.MethodInvoker;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.event.EventHandler;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.exception.ServiceException;
import cn.zorcc.common.serializer.JsonPoolSerializer;
import cn.zorcc.common.util.ClassUtil;
import cn.zorcc.http.HttpReq;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Slf4j
public class AppHttpEventHandler implements EventHandler<AppHttpEvent>, AppHttpMapping {
    /**
     *  可解析的method参数注解
     */
    private static final List<Class<?>> methodAnnos = List.of(Get.class, Post.class, Put.class, Patch.class, Delete.class);
    /**
     *  可解析的parameter参数注解
     */
    private static final List<Class<?>> parameterAnnos = List.of(Req.class, Header.class, Param.class, PathVariable.class, Body.class);
    /**
     *  初始化标识
     */
    private final AtomicBoolean initFlag = new AtomicBoolean(false);
    /**
     * Http方法对应的url匹配树
     */
    private final Map<HttpMethod, UrlTree> urlTreeMap = Map.of(HttpMethod.GET, new UrlTree(),
            HttpMethod.PUT, new UrlTree(),
            HttpMethod.POST, new UrlTree(),
            HttpMethod.DELETE, new UrlTree(),
            HttpMethod.PATCH, new UrlTree());
    private final MethodInvoker methodInvoker = MethodInvoker.INSTANCE;

    public AppHttpEventHandler() {
        Context.loadContainer(this, AppHttpMapping.class);
    }

    @Override
    public void init() {
        if(!initFlag.compareAndSet(false, true)) {
            throw new FrameworkException(ExceptionType.HTTP, "HttpEventHandler already started initialization");
        }
        for (UrlTree urlTree : urlTreeMap.values()) {
            urlTree.init();
        }
    }

    @Override
    public void handle(AppHttpEvent event) {
        HttpReq httpReq = event.httpReq();
        UrlTree urlTree = urlTreeMap.get(httpReq.method());
        if (urlTree.searchPath(event)) {
            Object invokeResult = methodInvoker.invoke(event.methodIndex(), event.args());

        }
    }

    @Override
    public void shutdown() {

    }

    /**
     *  注册HttpMapping对象
     */
    @Override
    public void registerHttpMapping(Object impl) {
        if(initFlag.get()) {
            throw new FrameworkException(ExceptionType.CONTEXT, "Can't register HttpMapping after initialization");
        }
        Class<?> implClass = impl.getClass();
        log.info("Registering HttpMapping for class : {}", implClass.getSimpleName());
        String prefix = implClass.getAnnotation(HttpMapping.class).prefix();
        for (Method method : ClassUtil.getAllMethod(implClass)) {
            processMethod(impl, method, prefix);
        }
    }

    /**
     *  处理HttpMapping中的方法解析部分
     * @param impl 具体实现对象
     * @param method Http映射方法
     * @param prefix Http映射路径前缀
     */
    private void processMethod(Object impl, Method method, String prefix) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation anno : annotations) {
            if(methodAnnos.contains(anno.getClass())) {
                processMethod(impl, anno, method, prefix);
            }
        }
    }

    private void processMethod(Object impl, Annotation anno, Method method, String prefix) {
        switch (anno) {
            case Get get -> registerUrlMapping(impl, method, HttpMethod.GET, prefix + get.path());
            case Post post -> registerUrlMapping(impl, method, HttpMethod.POST, prefix + post.path());
            case Patch patch -> registerUrlMapping(impl, method, HttpMethod.PATCH, prefix + patch.path());
            case Put put -> registerUrlMapping(impl, method, HttpMethod.PUT, prefix + put.path());
            case Delete delete -> registerUrlMapping(impl, method, HttpMethod.DELETE, prefix + delete.path());
            default -> throw new FrameworkException(ExceptionType.HTTP, "Target annotation not found");
        }
    }

    /**
     * 将方法映射添加到UrlTree中
     * @param impl 具体实现对象
     * @param method Http映射方法
     * @param httpMethod Http请求方法类型
     * @param path Http映射路径
     */
    private void registerUrlMapping(Object impl, Method method, HttpMethod httpMethod, String path) {
        int methodIndex = methodInvoker.registerMethodMapping(impl, method);
        Parameter[] parameters = method.getParameters();
        Function<AppHttpEvent, AppHttpEvent> func = event -> {
            event.setArgIndex(Constants.ZERO);
            event.setArgs(new Object[parameters.length]);
            return event;
        };
        for (Parameter parameter : parameters) {
            func = func.andThen(processParameter(parameter));
        }
        UrlTree urlTree = urlTreeMap.get(httpMethod);
        urlTree.addPath(path, methodIndex, func.andThen(AppHttpEvent::args));
    }

    /**
     * 处理HttpMapping中的参数解析部分，将HttpReq中的值提取至Args中
     * @param parameter 参数体
     * @return AppHttpEvent事件映射
     */
    private Function<AppHttpEvent, AppHttpEvent> processParameter(Parameter parameter) {
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation anno : annotations) {
            if(parameterAnnos.contains(anno.getClass())) {
                return processParameter(anno, parameter);
            }
        }
        throw new FrameworkException(ExceptionType.HTTP, "Http mapping parameters must have annotation on them");
    }

    private Function<AppHttpEvent, AppHttpEvent> processParameter(Annotation anno, Parameter parameter) {
        Class<?> parameterType = parameter.getType();
        switch (anno) {
            case Req ignoredReq -> {
                if (HttpReq.class.equals(parameterType)) {
                    return httpEvent -> {
                        int argIndex = httpEvent.argIndex();
                        httpEvent.setArgIndex(argIndex + 1);
                        httpEvent.args()[argIndex] = httpEvent.httpReq();
                        return httpEvent;
                    };
                }else {
                    throw new FrameworkException(ExceptionType.HTTP, "@Req parameter type must be HttpReq");
                }
            }
            case Header header -> {
                Function<String, Object> stringFunc = stringToObjectFunc(parameterType);
                String headerKey = header.value();
                boolean required = header.required();
                return httpEvent -> {
                    Map<String, String> headers = httpEvent.httpReq().headers();
                    String headerValue = headers.get(headerKey);
                    if(required && (headerValue == null || headerValue.isBlank())) {
                        throw new ServiceException("Missing header : " + headerKey);
                    }
                    int argIndex = httpEvent.argIndex();
                    httpEvent.setArgIndex(argIndex + 1);
                    httpEvent.args()[argIndex] = stringFunc.apply(headerValue);
                    return httpEvent;
                };
            }
            case Param param -> {
                Function<String, Object> stringFunc = stringToObjectFunc(parameterType);
                String paramKey = param.value();
                boolean required = param.required();
                return httpEvent -> {
                    Map<String, String> params = httpEvent.httpReq().params();
                    String paramValue = params.get(paramKey);
                    if(required && (paramValue == null || paramValue.isBlank())) {
                        throw new ServiceException("Missing param : " + paramKey);
                    }
                    int argIndex = httpEvent.argIndex();
                    httpEvent.setArgIndex(argIndex + 1);
                    httpEvent.args()[argIndex] = stringFunc.apply(paramValue);
                    return httpEvent;
                };
            }
            case PathVariable pathVariable -> {
                Function<String, Object> stringFunc = stringToObjectFunc(parameterType);
                boolean required = pathVariable.required();
                return httpEvent -> {
                    String wildStr = httpEvent.wildStr();
                    if(required && (wildStr == null || wildStr.isBlank())) {
                        throw new ServiceException("Missing pathVariable");
                    }
                    int argIndex = httpEvent.argIndex();
                    httpEvent.setArgIndex(argIndex + 1);
                    httpEvent.args()[argIndex] = stringFunc.apply(wildStr);
                    return httpEvent;
                };
            }
            case Body ignoredBody -> {
                return httpEvent -> {
                    byte[] body = httpEvent.httpReq().body();
                    int argIndex = httpEvent.argIndex();
                    httpEvent.setArgIndex(argIndex + 1);
                    httpEvent.args()[argIndex] = JsonPoolSerializer.INSTANCE.deserialize(body, parameterType);
                    return httpEvent;
                };
            }
            default -> throw new FrameworkException(ExceptionType.HTTP, "Target annotation not found");
        }
    }

    /**
     * 获取将字符串转化为指定类型的映射
     * @param parameterType 对象类型
     * @return 字符串到指定类型对象的映射
     */
    private Function<String, Object> stringToObjectFunc(Class<?> parameterType) {
        if(String.class.equals(parameterType)) {
            return s -> s;
        }else if(Integer.class.equals(parameterType)) {
            return Integer::parseInt;
        }else if(Short.class.equals(parameterType)) {
            return Short::parseShort;
        }else if(Long.class.equals(parameterType)) {
            return Long::parseLong;
        }else if(Float.class.equals(parameterType)) {
            return Float::parseFloat;
        }else if(Double.class.equals(parameterType)) {
            return Double::parseDouble;
        }else if(BigDecimal.class.equals(parameterType)) {
            return BigDecimal::new;
        }else {
            throw new FrameworkException(ExceptionType.HTTP, "No class match current receiver type");
        }
    }

}
