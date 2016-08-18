/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.runtime.internal.component;

import com.speedment.common.mapstream.MapStream;
import com.speedment.runtime.component.ManagerComponent;
import com.speedment.runtime.config.Table;
import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.runtime.license.Software;
import com.speedment.runtime.manager.Manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import java.util.Set;
import javax.inject.Singleton;

/**
 *
 * @author Emil Forslund
 */
@Singleton
public final class ManagerComponentImpl 
extends InternalOpenSourceComponent 
implements ManagerComponent {

    private final Map<Table, Manager<?>> tableMap;
    private final Map<Class<?>, Manager<?>> managersByEntity;

    public ManagerComponentImpl(Set<Manager<?>> managers) { // Injected.
        managersByEntity = mapByEntity(managers);
        tableMap         = new ConcurrentHashMap<>();
    }
    
    private Map<Class<?>, Manager<?>> mapByEntity(Set<Manager<?>> managers) {
        @SuppressWarnings("unchecked")
        final Map<Class<?>, Manager<?>> result = 
            (Map<Class<?>, Manager<?>>) MapStream.fromValues(
                managers.stream(), 
                m -> m.getEntityClass()
            ).toConcurrentMap();
        
        return result;
    }
    
    @Override
    protected String getDescription() {
        return "Holds the manager instances used to communicate with various database tables.";
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Manager<E> managerOf(Class<E> entityClass) throws SpeedmentException {
        requireNonNull(entityClass);
        
        @SuppressWarnings("unchecked")
        final Manager<E> manager = (Manager<E>) managersByEntity.get(entityClass);
        
        if (manager == null) {
            throw new SpeedmentException("No manager exists for " + entityClass);
        }
        
        return manager;
    }

    @Override
    public Stream<Manager<?>> stream() {
        return managersByEntity.values().stream();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ENTITY> Manager<ENTITY> findByTable(Table table) {
        requireNonNull(table);
        return (Manager<ENTITY>) tableMap.get(table);
    }

    @Override
    public Stream<Software> getDependencies() {
        return Stream.empty();
    }
}