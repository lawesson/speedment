/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2012 Google, Inc.
 * Copyright (C) 2016 Speedment, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.speedment.common.dagger.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * A BindingsGroup which fails when existing values are clobbered and sets
 * aside {@link SetBinding}.
 */
final class StandardBindings extends BindingsGroup {

    private final List<SetBinding<?>> setBindings;

    public StandardBindings() {
        setBindings = new ArrayList<>();
    }

    public StandardBindings(List<SetBinding<?>> baseSetBindings) {
        setBindings = new ArrayList<>(baseSetBindings.size());
        for (final SetBinding<?> sb : baseSetBindings) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            final SetBinding<?> child = new SetBinding(sb);
            setBindings.add(child);
            super.put(child.provideKey, child);
       }
   }

    @Override
    public Binding<?> contributeSetBinding(String key, SetBinding<?> value) {
        setBindings.add(value);
        return super.put(key, value);
    }
    
    List<SetBinding<?>> getSetBindings() {
        return setBindings;
    }
}