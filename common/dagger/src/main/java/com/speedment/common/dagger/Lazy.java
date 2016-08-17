/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2012 Google, Inc.
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

/**
 * A handle to a lazily-computed value. Each {@code Lazy} computes its value on
 * the first call to {@code get()} and remembers that same value for all
 * subsequent calls to {@code get()}.
 *
 * <h2>Example</h2>
 * The differences between <strong>direct injection</strong>, <strong>provider
 * injection</strong> and <strong>lazy injection</strong> are best demonstrated
 * with an example. Start with a module that computes a different integer for
 * each use:  {@code
 *   &#64;Module
 *   public class CounterModule {
 *
 *     int next = 100;
 *
 *     &#64;Provides Integer provideInteger() {
 *       System.out.println("computing...");
 *       return next++;
 * }
 * }
 * }
 *
 * <h3>Direct Injection</h3>
 * This class injects that integer and prints it 3 times:  {@code
 *   public class DirectCounter {
 *
 *     &#64Inject Integer value;
 *
 *     public void print() {
 *       System.out.println("printing...");
 *       System.out.println(value);
 *       System.out.println(value);
 *       System.out.println(value);
 * }
 * }
 * }
 *
 * Injecting a {@code DirectCounter} and invoking {@code print()} reveals that
 * the value is computed <i>before</i> it is required:  {@code
 *   computing...
 *   printing...
 *   100
 *   100
 *   100
 * }
 *
 * <h3>Provider Injection</h3>
 * This class injects a {@linkplain javax.inject.Provider provider} for the
 * integer. It calls {@code Provider.get()} 3 times and prints each result: {@code
 *   public class ProviderCounter {
 *
 *     &#64;Inject Provider<Integer> provider;
 *
 *     public void print() {
 *       System.out.println("printing...");
 *       System.out.println(provider.get());
 *       System.out.println(provider.get());
 *       System.out.println(provider.get());
 * }
 * }
 * }
 * Injecting a {@code ProviderCounter} and invoking {@code print()} shows that a
 * new value is computed each time {@code Provider.get()} is used:  {@code
 *   printing...
 *   computing...
 *   100
 *   computing...
 *   101
 *   computing...
 *   102
 * }
 *
 * <h3>Lazy Injection</h3>
 * This class injects a {@code Lazy} for the integer. Like the provider above,
 * it calls {@code Lazy.get()} 3 times and prints each result:  {@code
 *   public static class LazyCounter {
 *
 *     &#64;Inject Lazy<Integer> lazy;
 *
 *     public void print() {
 *       System.out.println("printing...");
 *       System.out.println(lazy.get());
 *       System.out.println(lazy.get());
 *       System.out.println(lazy.get());
 * }
 * }
 * }
 *
 * Injecting a {@code LazyCounter} and invoking {@code print()} shows that a new
 * value is computed immediately before it is needed. The same value is returned
 * for all subsequent uses:  {@code
 *   printing...
 *   computing...
 *   100
 *   100
 *   100
 * }
 *
 * <h3>Lazy != Singleton</h3>
 * Note that each injected {@code Lazy} is independent, and remembers its value
 * in isolation of other {@code Lazy} instances. In this example, two {@code
 * LazyCounter} objects are created and {@code print()} is called on each:  {@code
 *     public void run() {
 *       ObjectGraph graph = ObjectGraph.create(new CounterModule());
 *
 *       LazyCounter counter1 = graph.get(LazyCounter.class);
 *       counter1.print();
 *
 *       LazyCounter counter2 = graph.get(LazyCounter.class);
 *       counter2.print();
 * }
 * }
 *
 * The program's output demonstrates that each {@code Lazy} works independently:  {@code
 *   printing...
 *   computing...
 *   100
 *   100
 *   100
 *   printing...
 *   computing...
 *   101
 *   101
 *   101
 * }
 *
 * Use {@linkplain javax.inject.Singleton @Singleton} to share one instance
 * among all clients, and {@code Lazy} for lazy computation in a single client.
 *
 * @param <T> the wrapped type
 */
public interface Lazy<T> {

    /**
     * Return the underlying value, computing the value if necessary. All calls
     * to the same {@code Lazy} instance will return the same result.
     *
     * @return the inner object
     */
    T get();
}
