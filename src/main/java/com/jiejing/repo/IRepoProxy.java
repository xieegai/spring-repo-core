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

import java.util.List;
import java.util.Set;

/**
 * The interface to abstract a data repository proxy
 * @author baihe
 * Created on 2019/5/30
 */
public interface IRepoProxy {

    /**
     * hook interceptor pre insert
     * @param entityClass the entity class
     * @param schema the database of the entity
     * @param table the table of the entity
     * @param entities the inserted entities
     */
    <T> void preInsert(Class<T> entityClass, String schema, String table, Iterable<T> entities);

    /**
     * hook interceptor pre delete
     * @param entityClass the entity class
     * @param schema the database of the entity
     * @param table the table of the entity
     * @param entities the deleted entities
     */
    <T> void preDelete(Class<T> entityClass, String schema, String table, Iterable<T> entities);

    /**
     * hook interceptor pre update
     * @param entityClass the entity class
     * @param schema the database of the entity
     * @param table the table of the entity
     * @param oldEntities old entities
     * @param newEntities new entities
     * @param modifiedFields modified fields
     */
    <T> void preUpdate(Class<T> entityClass, String schema, String table,
        Iterable<T> oldEntities, Iterable<T> newEntities, Set<String> modifiedFields);
}
