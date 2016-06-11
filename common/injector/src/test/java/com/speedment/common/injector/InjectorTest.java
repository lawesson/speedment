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
package com.speedment.common.injector;

import com.speedment.common.injector.test_a.StringIdentityMapper;
import com.speedment.common.injector.test_a.TypeMapperComponent;
import com.speedment.common.injector.test_b.A;
import com.speedment.common.injector.test_b.B;
import com.speedment.common.injector.test_b.C;
import com.speedment.common.injector.test_c.ChildType;
import com.speedment.common.injector.test_c.ParentType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author Emil Forslund
 * @since  1.0.0
 */
public class InjectorTest {
    
    @Test
    public void testSimpleInjector() {
        final Injector injector;
        
        try {
            injector = Injector.builder()
                .canInject(StringIdentityMapper.class)
                .canInject(TypeMapperComponent.class)
                .build();
        } catch (final InstantiationException ex) {
            throw new RuntimeException(
                "Failed to instantiate class.", ex
            );
        }
        
        final StringIdentityMapper mapper = injector.get(StringIdentityMapper.class);
        final TypeMapperComponent mappers = injector.get(TypeMapperComponent.class);
        
        assertNotNull(mapper);
        assertNotNull(mappers);
        
        assertEquals(mapper, mappers.toDatabaseTypeMappers().get(String.class));
        assertEquals(mapper, mappers.toJavaTypeMappers().get(String.class));
    }
    
    @Test
    public void testPotentialCyclicDependency() {
        final Injector injector;
        
        try {
            injector = Injector.builder()
                .canInject(A.class)
                .canInject(B.class)
                .canInject(C.class)
                .build();
        } catch (final InstantiationException ex) {
            throw new RuntimeException(
                "Failed to instantiate class.", ex
            );
        }
        
        assertNotNull(injector.get(A.class).b);
        assertNotNull(injector.get(A.class).c);
        assertNotNull(injector.get(B.class).a);
        assertNotNull(injector.get(B.class).c);
        assertNotNull(injector.get(C.class).a);
        assertNotNull(injector.get(C.class).b);
    }
    
    @Test
    public void testInheritance() {
        final Injector injector;
        
        try {
            injector = Injector.builder()
                .canInject(A.class)
                .canInject(B.class)
                .canInject(C.class)
                .canInject(ChildType.class)
                .build();
        } catch (final Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(
                "Failed to instantiate class.", ex
            );
        }
        
        assertNotNull(injector.get(ParentType.class).a);
        assertNotNull(injector.get(ChildType.class).b);
    }
}