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
package com.speedment.runtime.component;

import com.speedment.runtime.annotation.Api;
import com.speedment.runtime.config.Table;
import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.runtime.manager.Manager;

import java.util.stream.Stream;

/**
 * The {@code ManagerComponent} provides the mapping between entities and their
 * corresponding managers. Custom managers may be plugged into the Speedment
 * framework.
 *
 * @author  Emil Forslund
 * @since   2.0.0
 */
@Api(version = "3.0")
public interface ManagerComponent extends Component {

    @Override
    default Class<ManagerComponent> getComponentClass() {
        return ManagerComponent.class;
    }

    /**
     * Obtains and returns the currently associated {@link Manager}
     * implementation for the given Entity interface Class. If no Manager exists
     * for the given entityClass, a SpeedmentException will be thrown.
     *
     * @param <E>          the entity interface type
     * @param entityClass  the entity interface {@code Class}
     * @return             the currently associated {@link Manager} 
     *                     implementation for the given Entity interface Class
     * 
     * @throws SpeedmentException if no Manager exists for the given entityClass
     */
    <E> Manager<E> managerOf(Class<E> entityClass) throws SpeedmentException;

    /**
     * Obtains and returns the currently associated {@link Manager}
     * implementation for the given Table.
     *
     * @param <E>    the Entity interface type
     * @param table  the table to use
     * @return       the currently associated {@link Manager} implementation for 
     *               the given table
     */
    <E> Manager<E> findByTable(Table table);

    /**
     * Returns a {@link Stream} of all {@link Manager Managers} associated with
     * this ManagerComponent.
     *
     * @return  a {@link Stream} of all {@link Manager Managers} associated with
     *          this ManagerComponent
     */
    Stream<Manager<?>> stream();
}
