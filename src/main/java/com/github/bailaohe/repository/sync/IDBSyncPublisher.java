package com.github.bailaohe.repository.sync;

import java.util.List;
import java.util.Set;

public interface IDBSyncPublisher {

    <T> void publishInsert(Class<T> entityClass, String schema, String table, List<T> models, boolean async);

    <T> void publishDelete(Class<T> entityClass, String schema, String table, List<T> models, boolean async);

    <T> void publishUpdate(Class<T> entityClass, String schema, String table, List<T> oldModels, List<T> newModels, Set<String> modifiedFields, boolean async);
}
