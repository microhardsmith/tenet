package cn.zorcc.orm.common;

import cn.zorcc.common.Meta;
import cn.zorcc.common.OldPair;
import cn.zorcc.common.database.Filler;
import cn.zorcc.common.database.Mapper;
import cn.zorcc.common.database.Page;
import cn.zorcc.common.database.Where;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.util.SqlUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SqlMapper<T> implements Mapper<T> {
    private static final Logger log = new Logger(SqlMapper.class);
    private static final Map<Class<?>, Mapper<?>> mapperMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Mapper<T> of(Class<T> poClass) {
        return (Mapper<T>) mapperMap.computeIfAbsent(poClass, p -> new SqlMapper<>(poClass));
    }
    private final Class<T> clazz;
    private final Meta meta;
    private final TableDescription description;

    public SqlMapper(Class<T> clazz) {
        this.clazz = clazz;
        this.meta = Meta.of(clazz);
        this.description = TableDescription.of(clazz);
    }

    @Override
    public int insert(T po) {
        if(description.preFill() && po instanceof Filler filler) {
            filler.onInsert();
        }
        OldPair<List<String>, List<Object>> oldPair = meta.getAll(po, description.cols());
        List<String> columns = oldPair.k();
        List<Object> args = oldPair.v();
        StringBuilder sb = new StringBuilder();
        SqlUtil.insertInto(sb, description.tableName(), columns);
        SqlUtil.values(sb, args.size());
        String sql = sb.toString();
        if(description.generatingId()) {

        }else {

        }
        return 0;
    }

    @Override
    public int insertBatch(Collection<T> poCollection) {
        return 0;
    }

    @Override
    public int delete(Where where) {
        return 0;
    }

    @Override
    public int deleteById(Object id) {
        return 0;
    }

    @Override
    public int deleteByIds(Collection<Object> idCollection) {
        return 0;
    }

    @Override
    public int update(T po, Where where) {
        return 0;
    }

    @Override
    public int updateById(T po) {
        return 0;
    }

    @Override
    public T selectById(Object id) {
        return null;
    }

    @Override
    public List<T> selectByIds(Collection<Object> idCollection) {
        return null;
    }

    @Override
    public List<T> select(Where where) {
        return null;
    }

    @Override
    public T selectOne(Where where) {
        return null;
    }

    @Override
    public long count(Where where) {
        return 0;
    }

    @Override
    public Page<T> page(Long pageNo, Long pageSize, Where where) {
        return null;
    }
}
