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
package com.speedment.runtime.internal.manager;

import com.speedment.common.dagger.Provides;
import com.speedment.runtime.component.ProjectComponent;
import com.speedment.runtime.component.StreamSupplierComponent;
import com.speedment.runtime.manager.Manager;
import com.speedment.runtime.stream.StreamDecorator;
import java.util.Optional;
import java.util.stream.Stream;
import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.runtime.field.Field;
import com.speedment.runtime.field.trait.HasComparableOperators;
import static java.util.Objects.requireNonNull;
import javax.inject.Inject;

/**
 *
 * @param <ENTITY>  entity type for this Manager
 * 
 * @author  Emil Forslund
 * @since   2.0.0
 */
public abstract class AbstractManager<ENTITY> implements Manager<ENTITY> {

    @Inject StreamSupplierComponent streamSupplierComponent;

    protected AbstractManager() {}
    
    @Provides(type = Provides.Type.SET)
    Manager<ENTITY> provideManager(ProjectComponent projectComponent) {
        requireNonNull(projectComponent); // Must be initalized for this to execute.
        return this;
    }

    @Override
    public Stream<ENTITY> stream(StreamDecorator decorator) {
        return streamSupplierComponent
            .stream(getEntityClass(), decorator);
    }

    @Override
    public <V extends Comparable<? super V>> Optional<ENTITY> findAny(Field<ENTITY> field, V value) {
        try {
            @SuppressWarnings("unchecked")
            final HasComparableOperators<ENTITY, V> casted = (HasComparableOperators<ENTITY, V>) field;
            return streamSupplierComponent.findAny(getEntityClass(), casted, value);
        } catch (final ClassCastException ex) {
            throw new SpeedmentException(
                "Field " + field + " was not of the expected type.", ex
            );
        }
    }
}