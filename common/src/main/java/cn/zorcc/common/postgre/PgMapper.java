package cn.zorcc.common.postgre;

import cn.zorcc.common.database.Mapper;
import cn.zorcc.common.database.Page;
import cn.zorcc.common.database.Where;

import java.util.Collection;
import java.util.List;

public class PgMapper<T> implements Mapper<T> {
    public PgMapper() {

    }

    @Override
    public int insert(T po) {
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
