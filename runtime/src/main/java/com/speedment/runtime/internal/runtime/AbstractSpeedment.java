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
package com.speedment.runtime.internal.runtime;

import com.speedment.common.dagger.ObjectGraph;
import com.speedment.runtime.Speedment;
import com.speedment.runtime.component.ManagerComponent;
import com.speedment.runtime.component.ProjectComponent;
import com.speedment.runtime.config.Project;
import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.runtime.manager.Manager;

import java.util.Optional;
import javax.inject.Inject;

/**
 * An abstract base implementation of the {@link Speedment} interface.
 * 
 * @author Emil Forslund
 * @since  3.0.0
 */
public abstract class AbstractSpeedment implements Speedment {
    
    @Inject ProjectComponent projectComponent;
    @Inject ManagerComponent managerComponent;
    @Inject ObjectGraph graph;
    
    protected AbstractSpeedment() {}

    @Override
    public <T> Optional<T> get(Class<T> type) {
        try {
            return Optional.of(graph.get(type));
        } catch (final IllegalArgumentException ex) {
            // Ignore this exception as a temporary solution.
            return Optional.empty();
        }
    }

    @Override
    public <T> T getOrThrow(Class<T> componentClass) throws SpeedmentException {
        try {
            return graph.get(componentClass);
        } catch (final IllegalArgumentException ex) {
            throw new SpeedmentException(
                "Specified component '" + componentClass.getSimpleName() + 
                "' is not installed in the platform.", ex
            );
        }
    }

    @Override
    public <ENTITY> Manager<ENTITY> managerOf(Class<ENTITY> entityType) {
        return managerComponent.managerOf(entityType);
    }

    @Override
    public Project project() {
        return projectComponent.getProject();
    }

    @Override
    public void stop() {
        // Do nothing as of now. In the future, we might want to do something
        // more advanced here to make sure all components are closed properly.
    }
}