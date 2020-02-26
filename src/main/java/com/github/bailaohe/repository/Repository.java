package com.github.bailaohe.repository;

import java.util.List;

public interface Repository<I, T> {
    int insert(T model);
    int insert(List<T> models);//批量持久化
    int deleteById(I id);//通过主鍵刪除
    int deleteByIds(List<I> ids);//批量刪除 eg：ids -> “1,2,3,4”
    int deleteByCondition(Object condition);
    int update(T model);//更新
    int updateByIds(List<I> ids, T model);//更新
    int updateByCondition(Object condition, T model);//更新
    T findById(I id);//通过ID查找
    List<T> findByIds(List<I> ids);//通过多个ID查找//eg：ids -> “1,2,3,4”
    List<T> findByCondition(Object condition);//根据条件查找
    List<T> findAll();//获取所有
    I getId(T model);
    default void setId(T model, I id) {
        throw new UnsupportedOperationException();
    }
    default long count() {
        throw new UnsupportedOperationException();
    }
    default long countByCondition(Object condition) {
        throw new UnsupportedOperationException();
    }
}
