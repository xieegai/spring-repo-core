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

package com.jiejing.repo;

import java.util.stream.StreamSupport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class RepoService<I, T, Q extends IQuery<T>> {

    /**
     * find records match the query
     * @param query the query param
     * @param sort the sort param
     * @return found records
     */
    protected abstract Iterable<T> findByQuery(Q query, Sort sort, long offset, long limit);

    protected Iterable<T> findByQuery(Q query, Sort sort) {
        if (query.isEmpty()) {
            return Collections.emptyList();
        }
        return findByQuery(query, sort, 0, 0);
    }

    /**
     * find records match the query
     * @param query the query param
     * @return found records
     */
    protected Iterable<T> findByQuery(Q query) {
        if (query.isEmpty()) {
            return Collections.emptyList();
        }
        return findByQuery(query, null);
    }

    /**
     * find the first record match the query
     * @param query the query param
     * @param sort the sort param
     * @return the found record
     */
    protected Optional<T> findOneByQuery(Q query, Sort sort) {
        long foundCount = countByQuery(query);
        if (foundCount > 0L) {
            Page<T> resultPage = findByQueryPage(query, PageRequest.of(0, 1, sort));
            return resultPage.getContent().stream().findFirst();
        }
        return Optional.empty();
    }

    /**
     * find the paged records match the query
     * @param query the query param
     * @param pageable the page param
     * @return the paged records
     */
    protected Page<T> findByQueryPage(Q query, Pageable pageable) {
        long totalCount = countByQuery(query);
        if (0L == totalCount) {
            return Page.empty();
        }
        Iterable<T> itemList = findByQuery(query, pageable.getSort(),
            pageable.getOffset(), pageable.getPageSize());
        return new PageImpl<>(ImmutableList.copyOf(itemList), pageable, totalCount);
    }

    /**
     * delete the entity with the given id
     * @param id the id to drop
     * @return result flag
     */
    public boolean deleteById(I id) {
        return deleteByIds(ImmutableList.of(id)).iterator().hasNext();
    }

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param ids must not be {@literal null}.
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    public List<T> getListByIds(Iterable<I> ids) {
        return ImmutableList.copyOf(getAllById(ids));
    }

    /**
     * insert an entity
     * @param entity the entity to insert
     * @return inserted entity
     */
    public T insert(T entity) {
        return insertAll(ImmutableList.of(entity)).iterator().next();
    }

    /**
     * parse the bulk object
     * @param toSave the raw original records to write
     * @param bulk the bulk operation result
     * @return the parsed records
     */
    protected Iterable<T> parseBulk(Iterable<T> toSave, Object bulk) {
        throw new UnsupportedOperationException();
    }

    /**
    /**
     * parse the repository condition from query
     * @param query the query
     * @return the parsed condition
     */
    protected Object parseCond(Q query) {
        return query;
    }

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
    private RepoServiceConfig config;

    /**
     * the type of entity id
     */
    protected Type idType;

    /**
     * the type of entity
     */
    @Getter
    protected Type entityType;

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

    public RepoService() {
        Class subClass = getClass();
        while (!(subClass.getGenericSuperclass() instanceof ParameterizedType)
                && subClass.getSuperclass() != RepoService.class) {
            subClass = subClass.getSuperclass();
        }

        idType = ((ParameterizedType) subClass.getGenericSuperclass()).getActualTypeArguments()[0];
        entityType = ((ParameterizedType) subClass.getGenericSuperclass()).getActualTypeArguments()[1];
        config = this.getClass().getAnnotation(RepoServiceConfig.class);
    }

    /**
     * manually initialize the inner repository
     */
    protected abstract Repository<I, T> initInnerRepository();

    @PostConstruct
    private void initRepository() {
        if (innerRepository == null) {
            log.warn("Cannot find autowired candidate for inner repository, try to initialize");
            innerRepository = initInnerRepository();
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
    protected Page<T> findByQueryPage(Q query, Sort sort, int pageNo, int pageSize) {
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
    protected Page<T> findByQueryPage(Q query, int pageNo, int pageSize) {
        return findByQueryPage(query, null, pageNo, pageSize);
    }

    /**
     * delete entities with ids
     * @param ids the id set
     * @return deleted id set
     */
    public Iterable<I> deleteByIds(Iterable<I> ids) {
        Iterable<I> droppedIds = repository.dropByIds(ids);

        if (useProxy()) {
            List<T> entities = StreamSupport.stream(ids.spliterator(), false).map(id -> {
                T entity = null;
                try {
                    entity = ((Class<T>) getEntityType()).newInstance();
                    innerRepository.setId(entity, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return entity;
            }).collect(Collectors.toList());

            repoProxy.preDelete((Class<T>) getEntityType(), schema(), table(), entities);
        }

        return ids;
    }

    /**
     * delete entities with given query
     * @param query the query param
     * @return deleted record count
     */
    protected long deleteByQuery(Q query) {
        // skip delete for empty query
        if (query.isEmpty()) {
            return 0L;
        }
        long nDelete = countByQuery(query);
        if (nDelete > 0) {
            if (useProxy()) {
                Iterable<T> toDeleted = findByQuery(query);
                if (toDeleted.iterator().hasNext()) {
                    Iterable droppedIds = deleteByIds(
                      StreamSupport.stream(toDeleted.spliterator(), false)
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

    /**
     * update an existed entity
     * @param entity the given entity holds the id and updates
     * @return the flag
     */
    public T updateById(T entity) {
        updateByIds(entity, ImmutableList.of(innerRepository.getId(entity)));
        return entity;
    }

    /**
     * update multiple entities by ids
     * @param entity the entity holds updates
     * @param ids the id set
     * @return updated entities with ids
     */
    public Iterable<T> updateByIds(T entity, Iterable<I> ids) {

        if (useProxy()) {
            List<T> toUpdateList = getListByIds(ids);

            List<T> updated = BeanUtil.mapList(toUpdateList, (Class<T>) entityType)
                    .stream().map(target -> {
                        BeanUtil.copyPropertiesExcludeNULL(entity, target);
                        return target;
                    }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(toUpdateList)) {
                Iterable<T> updateEnties = repository.updateByIds(ids, entity);
                Set<String> modifiedFields = getModifiedFields(entity, toUpdateList);
                repoProxy.preUpdate((Class<T>)entityType, schema(), table(), toUpdateList, updated, modifiedFields);
                return updateEnties;
            }

            return ImmutableList.of();
        }

        return repository.updateByIds(ids, entity);
    }

    /**
     * Update entities with query and the same entity holds the updates.
     * @param query the query
     * @param entity the entity holds the updates.
     * @return the updated count.
     */
    protected long updateByQuery(T entity, Q query) {
        // skip update for empty query
        if (query.isEmpty()) {
            return 0L;
        }
        long nUpdate = countByQuery(query);
        if (nUpdate > 0) {
            Iterable<T> toUpdates = findByQuery(query);
            if (toUpdates.iterator().hasNext()) {
                updateByIds(entity, Lists.transform(ImmutableList.copyOf(toUpdates), repository::getId));
                return nUpdate;
            }
        }
        return 0;
    }

    public long countAll() {
        return innerRepository.count();
    }

    /**
     * count entities with query
     * @param query the query
     * @return the count
     */
    protected long countByQuery(Q query) {
        if (query.isEmpty()) {
            return 0L;
        }
        return Long.valueOf(innerRepository.countByCond(parseCond(query))).intValue();
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    public Optional<T> getById(I id) {
        return repository.findById(id);
    }
    /**
     * Returns whether an entity with the given id exists.
     *
     * @param ids must not be {@literal null}.
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    public Iterable<T> getAllById(Iterable<I> ids) {
        return repository.findAllById(ids);
    }

    /**
     * insert multiple entites;
     * @param entities entities to insert
     * @return inserted entities;
     */
    public Iterable<T> insertAll(Iterable<T> entities) {
        repository.insertAll(entities);
        if (useProxy()) {
            repoProxy.preInsert((Class<T>)entityType, schema(), table(), entities);
        }
        return entities;
    }

    /**
     * save entities if they are not already existed
     * @param entities entities to save
     * @return the bulk result
     */
    public final Object saveIgnore(Iterable<T> entities) {
        Object bulkResult = saveIgnoreInternal(entities);

        if (useProxy()) {
            Iterable<T> inserted = parseBulk(entities, bulkResult);
            if (inserted.iterator().hasNext()) {
                repoProxy.preInsert((Class<T>)entityType, schema(), table(), inserted);
            }
        }

        return bulkResult;
    }

    /**
     * save entities if they are not already existed
     * @param entities entities to save
     * @param fieldName fields to validate the existence
     * @return the bulk result
     */
    public final Object saveIgnore(Iterable<T> entities, String... fieldName) {
        Object bulkResult = saveIgnoreInternal(entities, fieldName);

        if (useProxy()) {
            Iterable<T> inserted = parseBulk(entities, bulkResult);
            if (inserted.iterator().hasNext()) {
                repoProxy.preInsert((Class<T>)entityType, schema(), table(), inserted);
            }
        }
        return bulkResult;
    }

    /**
     * save entities if they are not already existed
     * @param entities entities to save
     * @return the bulk result
     */
    protected Object saveIgnoreInternal(Iterable<T> entities) {
        throw new UnsupportedOperationException();
    }

    /**
     * save entities if they are not already existed
     * @param entities entities to save
     * @param fieldName fields to validate the existence
     * @return the bulk result
     */
    protected Object saveIgnoreInternal(Iterable<T> entities, String... fieldName) {
        throw new UnsupportedOperationException();
    }

    /**
     * parse the modified fields
     * @param entity
     * @param oldEntities
     * @return the modified fields
     */
    private Set<String> getModifiedFields(T entity, List<T> oldEntities) {
        Map<String, Object> modifiedFields = Maps.newHashMap();
        Map<String, Method> getters = Maps.newHashMap();

        Class<T> entityClass = (Class<T>) entityType;
        Arrays.stream(entityClass.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).forEach(field -> {
            try {
                Method getter = entityClass.getMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1));
                getters.put(field.getName(), getter);

                Object fieldValue = getter.invoke(entity);
                if (fieldValue != null) {
                    modifiedFields.put(field.getName(), fieldValue);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        Set<String> modifiedFieldNames = modifiedFields.entrySet().stream().filter(entry -> {
            for (T oldModel : oldEntities) {
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
