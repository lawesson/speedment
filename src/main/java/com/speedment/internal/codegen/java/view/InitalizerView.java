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
package com.speedment.internal.codegen.java.view;

import com.speedment.codegen.Generator;
import com.speedment.codegen.Transform;
import com.speedment.codegen.model.Initializer;
import static com.speedment.internal.codegen.util.Formatting.*;
import static com.speedment.util.CollectorUtil.joinIfNotEmpty;
import static java.util.Objects.requireNonNull;
import java.util.Optional;

/**
 * Transforms from an {@link Initializer} to java code.
 * 
 * @author Emil Forslund
 */
public final class InitalizerView implements Transform<Initializer, String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> transform(Generator gen, Initializer model) {
        requireNonNull(gen);
        requireNonNull(model);
        
        return Optional.of(
            gen.onEach(model.getModifiers()).collect(joinIfNotEmpty(SPACE, EMPTY, SPACE)) +
            block(model.getCode().stream())
        );
    }
}