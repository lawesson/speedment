/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2012 Google, Inc.
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

import com.speedment.common.dagger.internal.DaggerObjectGraph;
import com.speedment.common.dagger.internal.FailoverLoader;
import com.speedment.common.dagger.internal.Loader;

/**
 * A graph of objects linked by their dependencies.
 *
 * <p>
 * The following injection features are supported:
 * <ul>
 * <li>Field injection. A class may have any number of field injections, and
 * fields may be of any visibility. Static fields will be injected each time an
 * instance is injected.
 * <li>Constructor injection. A class may have a single
 * {@code @Inject}-annotated constructor. Classes that have fields injected may
 * omit the {@code @Inject} annotation if they have a public no-arguments
 * constructor.
 * <li>Injection of {@code @Provides} method parameters.
 * <li>{@code @Provides} methods annotated {@code @Singleton}.
 * <li>Constructor-injected classes annotated {@code @Singleton}.
 * <li>Injection of {@code Provider}s.
 * <li>Injection of {@code MembersInjector}s.
 * <li>Qualifier annotations on injected parameters and fields.
 * <li>JSR 330 annotations.
 * </ul>
 *
 * <p>
 * The following injection features are not currently supported:
 * <ul>
 * <li>Method injection.</li>
 * <li>Circular dependencies.</li>
 * </ul>
 */
public abstract class ObjectGraph {

    protected ObjectGraph() {}

    /**
     * Returns an instance of {@code type}.
     *
     * @param <T>   the type that is requested
     * @param type  to retreive
     * @return      the retreived instance
     * 
     * @throws IllegalArgumentException if {@code type} is not one of this
     * object graph's {@link Module#injects injectable types}.
     */
    public abstract <T> T get(Class<T> type);

    /**
     * Injects the members of {@code instance}, including injectable members
     * inherited from its supertypes.
     *
     * @param <T>       the type that is requested
     * @param instance  instance to inject in
     * @return          the same instance as was specified
     * 
     * @throws IllegalArgumentException if the runtime type of {@code instance}
     * is not one of this object graph's
     * {@link Module#injects injectable types}.
     */
    public abstract <T> T inject(T instance);

    /**
     * Returns a new object graph that includes all of the objects in this
     * graph, plus additional objects in the
     * {@literal @}{@link Module}-annotated modules. This graph is a subgraph of
     * the returned graph.
     * <p>
     * The current graph is not modified by this operation: its objects and the
     * dependency links between them are unchanged. But this graph's objects may
     * be shared by both graphs. For example, the singletons of this graph may
     * be injected and used by the returned graph.
     * <p>
     * This <strong>does not</strong> inject any members or validate the graph.
     * 
     * @param modules  the modules to append
     * @return         the new graph with the modules appended
     * 
     * @see {@link #create} for guidance on injection and validation.
     */
    public abstract ObjectGraph plus(Object... modules);

    /**
     * Do runtime graph problem detection. For fastest graph creation, rely on
     * build time tools for graph validation.
     *
     * @throws IllegalStateException if this graph has problems.
     */
    public abstract void validate();

    /**
     * Injects the static fields of the classes listed in the object graph's
     * {@code staticInjections} property.
     */
    public abstract void injectStatics();

    /**
     * Returns a new dependency graph using the {@literal @}{@link
     * Module}-annotated modules.
     * <p>
     * This <strong>does not</strong> inject any members. Most applications
     * should call {@link #injectStatics} to inject static members and {@link
     * #inject} or get {@link #get(Class)} to inject instance members when this
     * method has returned.
     * <p>
     * This <strong>does not</strong> validate the graph. Rely on build time
     * tools for graph validation, or call {@link #validate} to find problems in
     * the graph at runtime.
     * 
     * @param modules  the modules to include in the graph
     * @return         the created object graph
     */
    public static ObjectGraph create(Object... modules) {
        return DaggerObjectGraph.makeGraph(null, new FailoverLoader(), modules);
    }

    // visible for testing
    static ObjectGraph createWith(Loader loader, Object... modules) {
        return DaggerObjectGraph.makeGraph(null, loader, modules);
    }
}
