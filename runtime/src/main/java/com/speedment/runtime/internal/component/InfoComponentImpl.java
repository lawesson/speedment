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

import com.speedment.runtime.component.InfoComponent;
import javax.inject.Singleton;

/**
 *
 * @author Emil Forslund
 * @since  3.0.0
 */
@Singleton
public final class InfoComponentImpl extends InternalOpenSourceComponent 
    implements InfoComponent {

    @Override
    protected String getDescription() {
        return "Sets the name, description and version of Speedment to show in the UI.";
    }

    @Override
    public String title() {
        return "Speedment";
    }

    @Override
    public String subtitle() {
        return "Open Source";
    }

    @Override
    public String version() {
        return "3.0.0";
    }
}