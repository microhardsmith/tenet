module tenet.common {
    requires lombok;
    requires org.slf4j;
    requires java.management;
    requires jctools.core;
    requires io.netty.codec;
    requires com.esotericsoftware.kryo;
    requires com.esotericsoftware.reflectasm;
    requires io.netty.transport;
    requires io.netty.buffer;
    requires io.netty.transport.classes.epoll;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.annotation;
    requires org.objenesis;
    requires jdk.incubator.concurrent;

    exports cn.zorcc.common;
    exports cn.zorcc.common.config;
    exports cn.zorcc.common.enums;
    exports cn.zorcc.common.event;
    exports cn.zorcc.common.exception;
    exports cn.zorcc.common.json;
    exports cn.zorcc.common.pojo;
    exports cn.zorcc.common.serializer;
    exports cn.zorcc.common.util;
    exports cn.zorcc.common.wheel;
    exports cn.zorcc.common.network;
    exports cn.zorcc.common.network.http;
}