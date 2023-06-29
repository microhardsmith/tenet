package cn.zorcc.common.sql;

import java.util.Collection;
import java.util.List;

public interface Mapper<T> {

    /**
     *  Insert a record, only process the non-null fields
     */
    int insert(T po);

    /**
     *  Insert batch record, process all the fields
     *  Note that the user must safely configure the batch size when using this method, dividing a big batch into several small one would be much better
     */
    int insertBatch(Collection<T> poCollection);

    /**
     *  Delete a record base on where condition
     */
    int delete(Where where);

    /**
     *  Delete a record based on Id
     */
    int deleteById(Object id);

    /**
     *  Delete several records based on Ids
     */
    int deleteByIds(Collection<Object> idCollection);

    /**
     *  Update records based on where condition
     *  Note that this method will only process the non-null fields, if you want to set some field to null, use a DynamicSqlMapper instead
     */
    int update(T po, Where where);

    /**
     *  Update record based on Id
     */
    int updateById(T po);

    /**
     *  Select a record based on Id
     */
    T selectById(Object id);

    /**
     *  Select records based on Id
     */
    List<T> selectByIds(Collection<Object> idCollection);

    /**
     *  Select records based on where
     */
    List<T> select(Where where);

    /**
     *  Select a record based on Id, throw a exception if multiple records return
     */
    T selectOne(Where where);

    /**
     *  Select count(*) based on Where
     */
    long count(Where where);

    /**
     *  Select a page based on Where
     */
    Page<T> page(Long pageNo, Long pageSize, Where where);
}
