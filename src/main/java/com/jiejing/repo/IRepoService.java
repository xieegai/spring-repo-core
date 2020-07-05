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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * The interface of repository data service.
 * @author baihe
 * Created on 2019/5/30
 */
public interface IRepoService<I, T, Q extends IQuery<T>> {

    /**
     * find records match the query
     * @param query the query param
     * @param sort the sort param
     * @return found records
     */
    Iterable<T> findByQuery(Q query, Sort sort, long offset, long limit);

    default Iterable<T> findByQuery(Q query, Sort sort) {
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
    default Iterable<T> findByQuery(Q query) {
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
    default Optional<T> findOneByQuery(Q query, Sort sort) {
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
    default Page<T> findByQueryPage(Q query, Pageable pageable) {
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
    default boolean deleteById(I id) {
        return deleteByIds(ImmutableList.of(id)).iterator().hasNext();
    }

    /**
     * delete entities with ids
     * @param ids the id set
     * @return deleted id set
     */
    Iterable<I> deleteByIds(Iterable<I> ids);

    /**
     * delete entities with given query
     * @param query the query param
     * @return deleted record count
     */
    long deleteByQuery(Q query);

    /**
     * update an existed entity
     * @param entity the given entity holds the id and updates
     * @return the updated entity
     */
    T updateById(T entity);

    /**
     * update multiple entities by ids
     * @param entity the entity holds updates
     * @param ids the id set
     * @return updated entities with ids
     */
    Iterable<T> updateByIds(T entity, Iterable<I> ids);

    /**
     * Update entities with query and the same entity holds the updates.
     * @param query the query
     * @param entity the entity holds the updates.
     * @return the updated count.
     */
    long updateByQuery(T entity, Q query);

    /**
     * Count the entities match the query
     * @param query the query
     * @return the count
     */
    long countByQuery(Q query);

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    Optional<T> getById(I id);

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param ids must not be {@literal null}.
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    Iterable<T> getAllById(Iterable<I> ids);

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param ids must not be {@literal null}.
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    default List<T> getListByIds(Iterable<I> ids) {
        return ImmutableList.copyOf(getAllById(ids));
    }

    /**
     * insert an entity
     * @param entity the entity to insert
     * @return inserted entity
     */
    default T insert(T entity) {
        return insertAll(ImmutableList.of(entity)).iterator().next();
    }

    /**
     * insert multiple entites;
     * @param entities entities to insert
     * @return inserted entities;
     */
    Iterable<T> insertAll(Iterable<T> entities);

    /**
     * save entities if they are not already existed
     * @param entities entities to save
     * @return the bulk result
     */
    Object saveIgnore(Iterable<T> entities);

    /**
     * save entities if they are not already existed
     * @param entities entities to save
     * @param fieldName fields to validate the existence
     * @return the bulk result
     */
    Object saveIgnore(Iterable<T> entities, String... fieldName);

    /**
     * parse the bulk object
     * @param toSave the raw original records to write
     * @param bulk the bulk operation result
     * @return the parsed records
     */
    default Iterable<T> parseBulk(Iterable<T> toSave, Object bulk) {
        throw new UnsupportedOperationException();
    }

    /**
     * parse the repository condition from query
     * @param query the query
     * @return the parsed condition
     */
    default Object parseCond(Q query) {
        return query;
    }
}
