/*
 * This file is part of repo-core, which is free library: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.baihe.data.repo;

import java.util.stream.StreamSupport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.baihe.data.repo.anno.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AbstractRepoService<I, T, Q> implements IRepoService<I, T, Q> {

    /**
     * the inner repository to access data
     */
    @Autowired(required = false)
    protected Repository<I, T> innerRepository;

    /**
     * the wrapper repository, left here to be history compatible
     */
    protected Repository<I, T> repository;

    /**
     * the proxy to sync data change
     */
    @Autowired(required = false)
    private IRepoProxy repoProxy;

    /**
     * the repository service configuration
     */
    private RepoService config;

    /**
     * the type of entity id
     */
    @Getter
    private Type idType;

    /**
     * the type of entity
     */
    @Getter
    private Type entityType;

    /**
     * if the repository service use a proxy
     * @return the flag
     */
    private boolean useProxy() {
        boolean flag = null != config && config.useProxy();
        return flag && repoProxy != null;
    }

    /**
     * the schema/database of the entity
     * @return the schema/database
     */
    public String schema() {
        return null != config ? config.schema() : "";
    }

    /**
     * the table name of the entity
     * @return the table name
     */
    public String table() {
        return null != config ? config.table() : "";
    }

    /**
     * the index of the first page
     * @return default to 0
     */
    public int startPage() {
        return null != config ? config.startPage() : 0;
    }

    public AbstractRepoService() {
        Class subClass = getClass();
        while (!(subClass.getGenericSuperclass() instanceof ParameterizedType)
                && subClass.getSuperclass() != AbstractRepoService.class) {
            subClass = subClass.getSuperclass();
        }

        idType = ((ParameterizedType) subClass.getGenericSuperclass()).getActualTypeArguments()[0];
        entityType = ((ParameterizedType) subClass.getGenericSuperclass()).getActualTypeArguments()[1];
        config = this.getClass().getAnnotation(RepoService.class);
    }

    /**
     * manually initialize the inner repository
     */
    void initInnerRepository() {
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

    /**
     * find the paged records by query, sort and page param
     * @param query the query param
     * @param sort the sort param
     * @param pageNo the page no
     * @param pageSize the page size
     * @return the paged records
     */
    public Page<T> findByQueryPage(Q query, Sort sort, int pageNo, int pageSize) {
        Pageable pageReq = PageRequest.of(Math.max(pageNo - startPage(), 0), pageSize, sort);
        return findByQueryPage(query, pageReq);
    }

    /**
     * find the paged records by query, sort and page param
     * @param query the query param
     * @param pageNo the page no
     * @param pageSize the page size
     * @return the paged records
     */
    public Page<T> findByQueryPage(Q query, int pageNo, int pageSize) {
        return findByQueryPage(query, null, pageNo, pageSize);
    }

    @Override
    public Iterable<I> deleteByIds(Iterable<I> ids) {
        Iterable<I> droppedIds = repository.dropByIds(ids);

        if (useProxy()) {
            publishDelete(droppedIds);
        }

        return droppedIds;
    }

    @Override
    public long dropByQuery(Q query) {
        long nDelete = countByQuery(query);
        if (nDelete > 0) {
            if (useProxy()) {
                Iterable<T> toDeleted = findByQuery(query);
                if (toDeleted.iterator().hasNext()) {
                    Iterable droppedIds = dropByIds(StreamSupport.stream(toDeleted.spliterator(), false)
                        .map(m -> innerRepository.getId(m)).collect(
                            Collectors.toList()));
                    return StreamSupport.stream(droppedIds.spliterator(), false).count();
                }
            } else {
                repository.dropByCond(parseCond(query));
            }
        }
        return 0;
    }

    @Override
    public boolean updateById(T model) {
        return updateByIds(model, ImmutableList.of(innerRepository.getId(model))) > 0;
    }

    @Override
    public int updateByIds(T model, Iterable<I> ids) {

        if (useProxy()) {
            Iterable<T> toUpdate = innerRepository.findAllById(ids);

            List<T> updated = BeanUtil.mapList(toUpdate, (Class<T>) entityType)
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

    public long countByQuery(Object query) {
        return Long.valueOf(innerRepository.countByCond(query)).intValue();
    }

    @Override
    public Optional<T> getById(I id) {
        return repository.findById(id);
    }

    public int insert(T model) {
        return insertList(ImmutableList.of(model));
    }

    public int insertList(List<T> models) {
        int nInsert = repository.insert(models);
        if (useProxy()) {
            repoProxy.preInsert(entityType, schema(), table(), models)
        }
        return nInsert;
    }

    @Override
    public final Object saveIgnore(Iterable<T> entites) {
        Object bulkResult = saveIgnoreInternal(entites);

        if (useProxy()) {
            List<T> inserted = parseBulk(entites, bulkResult);
            if (!CollectionUtils.isEmpty(inserted)) {
                publishInsert(inserted);
            }
        }

        return bulkResult;
    }

    @Override
    public final Object saveIgnore(Iterable<T> entities, String... fieldName) {
        Object bulkResult = saveIgnoreInternal(entities, fieldName);

        if (useProxy()) {
            List<T> inserted = parseBulk(entities, bulkResult);
            if (!CollectionUtils.isEmpty(inserted)) {
                publishInsert(inserted);
            }
        }
        return bulkResult;
    }

    public Object saveIgnoreInternal(Iterable<T> entities) {
        throw new UnsupportedOperationException();
    }

    public Object saveIgnoreInternal(Iterable<T> entities, String... fieldName) {
        throw new UnsupportedOperationException();
    }

    public void publishInsert(List<T> models) {
        if (null != repoProxy) {
            repoProxy.publishInsert((Class<T>) getEntityType(), schema(), table(), models, asyncHook());
        }
    }

    public void publishDelete(Iterable<I> ids) {
        if (null != dbSyncPublisher) {
            List<T> models = StreamSupport.stream(ids.spliterator(), false).map(id -> {
                T model = null;
                try {
                    model = ((Class<T>) getEntityType()).newInstance();
                    innerRepository.setId(model, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return model;
            }).collect(Collectors.toList());

            dbSyncPublisher.publishDelete((Class<T>) getEntityType(), schema(), table(), models, asyncHook());
        }
    }

    public void publishUpdate(List<T> oldModels, List<T> newModels, Set<String> modifiedFields) {
        if (null != dbSyncPublisher) {
            dbSyncPublisher.publishUpdate((Class<T>) getEntityType(), schema(), table(), oldModels, newModels, modifiedFields, asyncHook());
        }
    }

    private Set<String> getModifiedFields(T model, List<T> oldModels) {
        Map<String, Object> modifiedFields = Maps.newHashMap();
        Map<String, Method> getters = Maps.newHashMap();

        Class<T> entityClass = (Class<T>) entityType;
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
