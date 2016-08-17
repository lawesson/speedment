/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2016 Speedment, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.speedment.common.dagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a class that contributes to the object graph.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Module {
  /**
   * Returns classes that object graphs created with this module must be able to
   * inject. This includes both classes passed to {@link ObjectGraph#get},
   * the types of instances passed {@link ObjectGraph#inject} and
   * {@link javax.inject.Inject} annotated classes that need to be scoped to the
   * resulting object graphs.
   *
   * <p>It is an error to call {@link ObjectGraph#get} or {@link
   * ObjectGraph#inject} with a type that isn't listed in the {@code injects}
   * set for any of the object graph's modules. Making such a call will trigger
   * an {@code IllegalArgumentException} at runtime.
   *
   * <p>Maintaining this set is onerous, but doing so provides benefits to the
   * application. This set enables dagger to perform more aggressive static
   * analysis than would be otherwise possible:
   * <ul>
   *   <li><strong>Detect missing bindings.</strong> Dagger can check that all
   *       injected dependencies can be satisfied. Set {@code complete=false} to
   *       disable this check for the current module.
   *   <li><strong>Detect unused bindings.</strong> Dagger can check that all
   *       provides methods are used to satisfy injected dependencies. Set
   *       {@code library=true} to disable this check for the current module.
   * </ul>
   * 
   * @return  the classes that is injectable
   */
  Class<?>[] injects() default { };
  
  /**
   * Returns static injections.
   * 
   * @return  static injectables 
   */
  Class<?>[] staticInjections() default { };

  /**
   * True if {@code @Provides} methods from this module are permitted to
   * override those of other modules. This is a dangerous feature as it permits
   * binding conflicts to go unnoticed. It should only be used in test and
   * development modules.
   * 
   * @return  are providers allowed to override other providers
   */
  boolean overrides() default false;

  /**
   * Additional {@code @Module}-annotated classes from which this module is
   * composed. The de-duplicated contributions of the modules in
   * {@code includes}, and of their inclusions recursively, are all contributed
   * to the object graph.
   * 
   * @return  additional modules
   */
  Class<?>[] includes() default { };

  /**
   * An optional {@code @Module}-annotated class upon which this module can be
   * {@link ObjectGraph#plus added} to form a complete graph.
   * 
   * @return  module to which this can be added
   */
  Class<?> addsTo() default Void.class;

  /**
   * True if all of the bindings required by this module can also be satisfied
   * by this module, its {@link #includes} and its {@link #addsTo}. If a module
   * is complete it is eligible for additional static checking: tools can detect
   * if required bindings are not available. Modules that have external
   * dependencies must use {@code complete = false}.
   * 
   * @return true if this graph is complete
   */
  boolean complete() default true;

  /**
   * False if all the included bindings in this module are necessary to satisfy
   * all of its {@link #injects injectable types}. If a module is not a library
   * module, it is eligible for additional static checking: tools can detect if
   * included bindings are not necessary. If you provide bindings that are not
   * used by this module's graph, then you must declare {@code library = true}.
   * <p>
   * This is intended to help you detect dead code.
   * 
   * @return  if this is a library module
   */
  boolean library() default false;
}