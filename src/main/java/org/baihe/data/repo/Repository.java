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

package org.baihe.data.repo;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.data.repository.CrudRepository;

/**
 * The core interface to abstract a data repository
 * @author baihe
 * Created on 2019/5/30
 */
public interface Repository<I, T> extends CrudRepository<T, I> {
    /**
     * Get the id of the given entity
     * @param entity the entity
     * @return the id of the entity will never be {@literal null}.
     */
    I getId(T entity);

    /**
     * Set the id into the given entity
     * @param entity the entity
     * @param id the given id value;
     */
    default void setId(T entity, I id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Insert a given entity. Use the returned instance for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param entity must not be {@literal null}.
     * @return the inserted entity will never be {@literal null}.
     */
    <S extends T> S insert(S entity);

    /**
     * Insert all given entities.
     *
     * @param entities must not be {@literal null}.
     * @return the inserted entities will never be {@literal null}.
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    <S extends T> Iterable<S> insertAll(Iterable<S> entities);

    /**
     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param entity must not be {@literal null}.
     * @return the saved entity will never be {@literal null}.
     */
    default <S extends T> S save(S entity) {
        return insert(entity);
    }

    /**
     * Saves all given entities.
     *
     * @param entities must not be {@literal null}.
     * @return the saved entities will never be {@literal null}.
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    default <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        return insertAll(entities);
    }

    /**
     * Update a existed entity by given entity with id and fields to update filled. Returned instance with id field filled at least to indicate a succeeded update.
     * @param entity the entity with id and fields to update filled
     * @return the updated entity holds the id
     */
    T updateById(T entity);

    /**
     * Update multiple entities referred by a list of with the same entity with fields to update filled. Return instances with id field filled at least.
     * @param ids the id set to update with the entity
     * @param entity the entity holds the updates
     * @return the updated entities
     */
    Iterable<T> updateByIds(Iterable<I> ids, T entity);

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param id must not be {@literal null}.
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     * @throws IllegalArgumentException if {@code id} is {@literal null}.
     */
    default boolean existsById(I id) {
        return findById(id).isPresent();
    }

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    default Iterable<T> findAll() {
        throw new UnsupportedOperationException("disabled by default for data security");
    }

    /**
     * Deletes the entity with the given id.
     *
     * @param id must not be {@literal null}.
     * @throws IllegalArgumentException in case the given {@code id} is {@literal null}
     */
    default void deleteById(I id) {
        dropById(id);
    }

    /**
     * Deletes the entity with the given id, return flag indicating the operation result.
     * @param id must not be {@literal null}.
     * @return true if deleted or else false.
     */
    boolean dropById(I id);

    /**
     * Deletes a given entity.
     *
     * @param entity
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    default void delete(T entity) {
        if (null == entity) {
            throw new IllegalArgumentException("entity is null");
        }
        deleteById(getId(entity));
    }

    /**
     * Delete multiple entities by given ids.
     * @param ids the id set to delete
     * @return deleted ids
     */
    Iterable<I> dropByIds(Iterable<I> ids);

    /**
     * Deletes the given entities.
     *
     * @param entities
     * @throws IllegalArgumentException in case the given {@link Iterable} is {@literal null}.
     */
    default void deleteAll(Iterable<? extends T> entities) {
        dropByIds(StreamSupport.stream(entities.spliterator(), false).map(this::getId).collect(
            Collectors.toList()));
    }

    /**
     * Deletes all entities managed by the repository.
     */
    default void deleteAll() {
        throw new UnsupportedOperationException("disabled by default for data security");
    }

    /**
     * Deletes all entities match the filter param.
     * @param cond the filter param
     * @return the deleted count
     */
    long dropByCond(Object cond);

    /**
     * Update entities with filter param and the same entity holds the updates.
     * @param cond the filter param.
     * @param entity the entity holds the updates.
     * @return the updated count.
     */
    long updateByCond(Object cond, T entity);

    /**
     * Delete entities with filter param.
     * @param cond the filter param.
     * @return found entities
     */
    Iterable<T> findByCond(Object cond);

    /**
     * Count the entities match the filter param
     * @param cond the filter param
     * @return the count
     */
    default long countByCond(Object cond) {
        throw new UnsupportedOperationException();
    }
}
