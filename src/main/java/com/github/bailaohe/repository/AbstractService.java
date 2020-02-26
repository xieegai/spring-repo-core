package com.github.bailaohe.repository;

import com.github.bailaohe.repository.sync.IDBSyncPublisher;
import com.github.bailaohe.repository.utils.BeanUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractService<I, T> implements RepositoryService<I, T> {
    @Autowired(required = false)
    protected Repository<I, T> innerRepository;

    protected Repository<I, T> repository;

    @Autowired(required = false)
    private IDBSyncPublisher dbSyncPublisher;

    @Getter
    private Type keyType;

    @Getter
    private Type modelType;

    public Boolean enableHook() {
        return false;
    }

    public Boolean asyncHook() {
        return true;
    }

    public String schema() {
        return null;
    }

    public String table() {
        return null;
    }

    public AbstractService() {
        Class subClass = getClass();
        while (!(subClass.getGenericSuperclass() instanceof ParameterizedType)
                && subClass.getSuperclass() != AbstractService.class) {
            subClass = subClass.getSuperclass();
        }

        keyType = ((ParameterizedType) subClass.getGenericSuperclass()).getActualTypeArguments()[0];
        modelType = ((ParameterizedType) subClass.getGenericSuperclass()).getActualTypeArguments()[1];
    }

    protected void initInnerRepository() {
    }

    @PostConstruct
    private void initRepository() {
        if (innerRepository == null) {
            log.warn("Cannot find autowired candidate for inner repository, try to initialize");
            initInnerRepository();
        }

        Objects.requireNonNull(innerRepository, "Initialize inner repository failed");

        //TODO set repository to inner repository directly
        this.repository = innerRepository;
    }

    public abstract List<T> findByQuery(Object query);

    public abstract List<T> findByQuery(Object query, Object orderByClause);

    public T findOneByQuery(Object query) {
        List<T> itemList = findByQuery(query);
        return CollectionUtils.isEmpty(itemList) ? null : itemList.get(0);
    }

    public abstract List<T> findByQueryPage(Object query, Object orderByClause, int pageNo, int pageSize);

    public List<T> findByQueryPage(Object query, int pageNo, int pageSize) {
        return findByQueryPage(query, null, pageNo, pageSize);
    }

    public int deleteById(I id) {
        return deleteByIds(ImmutableList.of(id));
    }

    public int deleteByIds(List<I> ids) {
        int deleted = repository.deleteByIds(ids);

        if (enableHook()) {
            publishDelete(ids);
        }

        return deleted;
    }

    public int deleteByQuery(Object query) {
        int nDelete = countByQuery(query);
        if (nDelete > 0) {
            List<T> toDeleteList = findByQuery(query);
            if (!CollectionUtils.isEmpty(toDeleteList)) {
                return deleteByIds(Lists.transform(toDeleteList, m -> innerRepository.getId(m)));
            }
        }
        return 0;
    }


    public boolean updateById(T model) {
        return updateByIds(model, ImmutableList.of(innerRepository.getId(model))) > 0;
    }

    public int updateByIds(T model, List<I> ids) {

        if (enableHook()) {
            List<T> toUpdateList = innerRepository.findByIds(ids);

            List<T> updated = BeanUtil.mapList(toUpdateList, (Class<T>) modelType)
                    .stream().map(x -> {
                        BeanUtil.copyPropertiesExcludeNULL(model, x);
                        return x;
                    }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(toUpdateList)) {
                int nUpdated = repository.updateByIds(ids, model);
                Set<String> modifiedFields = getModifiedFields(model, toUpdateList);
                publishUpdate(toUpdateList, updated, modifiedFields);
                return nUpdated;
            }

            return 0;
        }

        return repository.updateByIds(ids, model);
    }

    public int updateByQuery(T model, Object query) {
        int nUpdate = countByQuery(query);
        if (nUpdate > 0) {
            List<T> toUpdates = findByQuery(query);
            if (!CollectionUtils.isEmpty(toUpdates)) {
                return updateByIds(model, Lists.transform(toUpdates, repository::getId));
            }
        }
        return 0;
    }

    public int countAll() {
        return Long.valueOf(innerRepository.count()).intValue();
    }

    public int countByQuery(Object query) {
        return Long.valueOf(innerRepository.countByCondition(query)).intValue();
    }

    public long countLong() {
        return innerRepository.count();
    }

    public long countLongByQuery(Object query) {
        return innerRepository.countByCondition(query);
    }

    public T getById(I id) {
        return repository.findById(id);
    }

    public List<T> getListByIds(List<I> ids) {
        return repository.findByIds(ids);
    }

    public int insert(T model) {
        return insertList(ImmutableList.of(model));
    }

    public int insertList(List<T> models) {
        int nInsert = repository.insert(models);
        if (enableHook()) {
            publishInsert(models);
        }
        return nInsert;
    }

    public final Object insertIfNotExist(List<T> modelList) {
        Object bulkResult = insertIfNotExistInternal(modelList);

        if (enableHook()) {
            List<T> inserted = getInsertedFromBulkResult(modelList, bulkResult);
            if (!CollectionUtils.isEmpty(inserted)) {
                publishInsert(inserted);
            }
        }

        return bulkResult;
    }

    public final Object insertIfNotExist(List<T> modelList, String... fieldName) {
        Object bulkResult = insertIfNotExistInternal(modelList, fieldName);

        if (enableHook()) {
            List<T> inserted = getInsertedFromBulkResult(modelList, bulkResult);
            if (!CollectionUtils.isEmpty(inserted)) {
                publishInsert(inserted);
            }
        }

        return bulkResult;
    }

    public Object insertIfNotExistInternal(List<T> modelList) {
        throw new UnsupportedOperationException();
    }

    public Object insertIfNotExistInternal(List<T> modelList, String... fieldName) {
        throw new UnsupportedOperationException();
    }

    public List<T> getInsertedFromBulkResult(List<T> toInsertList, Object bulkResult) {
        throw new UnsupportedOperationException();
    }

    public void publishInsert(List<T> models) {
        if (null != dbSyncPublisher) {
            dbSyncPublisher.publishInsert((Class<T>) getModelType(), schema(), table(), models, asyncHook());
        }
    }

    public void publishDelete(List<I> ids) {
        if (null != dbSyncPublisher) {
            List<T> models = ids.stream().map(id -> {
                T model = null;
                try {
                    model = ((Class<T>) getModelType()).newInstance();
                    innerRepository.setId(model, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return model;
            }).collect(Collectors.toList());

            dbSyncPublisher.publishDelete((Class<T>) getModelType(), schema(), table(), models, asyncHook());
        }
    }

    public void publishUpdate(List<T> oldModels, List<T> newModels, Set<String> modifiedFields) {
        if (null != dbSyncPublisher) {
            dbSyncPublisher.publishUpdate((Class<T>) getModelType(), schema(), table(), oldModels, newModels, modifiedFields, asyncHook());
        }
    }

    private Set<String> getModifiedFields(T model, List<T> oldModels) {
        Map<String, Object> modifiedFields = Maps.newHashMap();
        Map<String, Method> getters = Maps.newHashMap();

        Class<T> entityClass = (Class<T>) modelType;
        Arrays.stream(entityClass.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).forEach(field -> {
            try {
                Method getter = entityClass.getMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
                getters.put(field.getName(), getter);

                Object fieldValue = getter.invoke(model);
                if (fieldValue != null) {
                    modifiedFields.put(field.getName(), fieldValue);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        Set<String> modifiedFieldNames = modifiedFields.entrySet().stream().filter(entry -> {
            for (T oldModel : oldModels) {
                Method getter = getters.get(entry.getKey());
                Object oldFieldValue = null;
                try {
                    oldFieldValue = getter.invoke(oldModel);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (!entry.getValue().equals(oldFieldValue)) {
                    return true;
                }
            }
            return false;
        }).map(entry -> entry.getKey()).collect(Collectors.toSet());

        return modifiedFieldNames;
    }
}
