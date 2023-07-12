package cn.zorcc.common.sql;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Where {
    private static final String WHERE = " where ";
    private final List<Object> args = new ArrayList<>();
    private final StringBuilder sql = new StringBuilder();
    private final Set<String> orderDescParam = new HashSet<>();
    private final Set<String> orderAscParam = new HashSet<>();
    /**
     *  下一个语句使用and还是or连接,默认为false,使用and连接,调用or()方法后下一个语句使用or连接
     */
    private boolean connectFlag = false;
    /**
     *  是否为第一个语句
     */
    private boolean firstFlag = true;
    /**
     *  参数索引
     */
    private int index;
}
