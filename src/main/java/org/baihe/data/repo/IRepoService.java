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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * The interface of repository data service.
 * @author baihe
 * Created on 2019/5/30
 */
public interface IRepoService<I, T, Q> {

    /**
     * find records match the query
     * @param query the query param
     * @param sort the sort param
     * @return found records
     */
    Iterable<T> findByQuery(Q query, Sort sort);

    /**
     * find records match the query
     * @param query the query param
     * @return found records
     */
    default Iterable<T> findByQuery(Q query) {
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
    Page<T> findByQueryPage(Q query, Pageable pageable);

    /**
     * drop the entity with the given id
     * @param id the id to drop
     * @return result flag
     */
    default boolean dropById(I id) {
        return dropByIds(ImmutableList.of(id)).iterator().hasNext();
    }

    /**
     *
     * @param ids
     * @return
     */
    Iterable<I> dropByIds(Iterable<I> ids);

    long dropByQuery(Q query);

    boolean updateById(T model);

    int updateByIds(T model, Iterable<I> ids);

    int updateByQuery(T model, Q query);

    long countByQuery(Q query);

    Optional<T> getById(I id);

    Iterable<T> getAllById(Iterable<I> ids);

    default List<T> getListByIds(Iterable<I> ids) {
        return ImmutableList.copyOf(getAllById(ids));
    }

    default T insert(T model) {
        return insertList(ImmutableList.of(model)).iterator().next();
    }

    Iterable<T> insertList(Iterable<T> models);

    Object saveIgnore(Iterable<T> entities);

    Object saveIgnore(Iterable<T> entities, String... fieldName);

    default List<T> parseBulk(Iterable<T> toSave, Object bulk) {
        throw new UnsupportedOperationException();
    }

    Object parseCond(Q query);
}
