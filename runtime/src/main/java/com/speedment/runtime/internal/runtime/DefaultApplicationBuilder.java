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
import com.speedment.runtime.ApplicationMetadata;
import com.speedment.runtime.Speedment;

/**
 * The default application builder is an implementation of the 
 * {@link ApplicationBuilder}-interface that uses the standard 
 * {@link SpeedmentImpl}-class as the application and the metadata supplied in 
 * the constructor. This can be useeful when constructing tests where the 
 * metadata is created programatically.
 * 
 * @author  Emil Forslund
 * @since   3.0.0
 */
public final class DefaultApplicationBuilder extends
    AbstractApplicationBuilder<Speedment, DefaultApplicationBuilder> {

    public DefaultApplicationBuilder(Class<? extends ApplicationMetadata> metadataClass) {
        super(SpeedmentImpl.class, metadataClass);
    }
    
    public DefaultApplicationBuilder(ObjectGraph graph) {
        super(graph);
    }

    @Override
    protected Speedment build(ObjectGraph graph) {
        return graph.get(Speedment.class);
    }

    @Override
    protected void printWelcomeMessage(ObjectGraph graph) {
        // Do not print any welcome message when using the default builder.
    }
}