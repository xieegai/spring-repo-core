package com.github.bailaohe.repository;

import com.google.common.collect.ImmutableList;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by baihe on 2019/5/30
 */
public interface RepositoryService<I, T> {
    List<T> findByQuery(Object query);

    List<T> findByQuery(Object query, Object orderByClause);

    T findOneByQuery(Object query);

    List<T> findByQueryPage(Object query, Object orderByClause, int pageNo, int pageSize);

    default List<T> findByQueryPage(Object query, int pageNo, int pageSize) {
        return findByQueryPage(query, null, pageNo, pageSize);
    }

    default int deleteById(I id) {
        return deleteByIds(ImmutableList.of(id));
    }

    int deleteByIds(List<I> ids);

    int deleteByQuery(Object query);

    boolean updateById(T model);

    int updateByIds(T model, List<I> ids);

    int updateByQuery(T model, Object query);

    int countAll();

    int countByQuery(Object query);

    default T getById(I id) {
        List<T> resultList = getListByIds(ImmutableList.of(id));
        if (!CollectionUtils.isEmpty(resultList)) {
            return resultList.get(0);
        }
        return null;
    }

    List<T> getListByIds(List<I> ids);

    default int insert(T model) {
        return insertList(ImmutableList.of(model));
    }

    int insertList(List<T> models);

    Object insertIfNotExist(List<T> modelList);

    Object insertIfNotExist(List<T> modelList, String... fieldName);

    default List<T> getInsertedFromBulkResult(List<T> toInsertList, Object bulkResult) {
        throw new UnsupportedOperationException();
    }
}
