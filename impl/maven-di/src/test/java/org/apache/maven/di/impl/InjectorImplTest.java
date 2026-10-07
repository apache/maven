/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.di.impl;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Aggregate;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Qualifier;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.Typed;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class InjectorImplTest {

    @Test
    void markerQualifierTest() {
        Injector injector = Injector.create().bindImplicit(QualifierTest.class);
        QualifierTest.MyMojo mojo = injector.getInstance(QualifierTest.MyMojo.class);
        assertNotNull(mojo);
        assertInstanceOf(QualifierTest.MyQualifiedServiceImpl.class, mojo.service);
    }

    static class QualifierTest {
        @Qualifier
        @Retention(RUNTIME)
        @interface MyQualifier {}

        interface MyService {}

        @Named
        @Priority(10)
        static class MyNamedServiceImpl implements MyService {}

        @MyQualifier
        static class MyQualifiedServiceImpl implements MyService {}

        @Named
        static class MyMojo {
            @Inject
            @MyQualifier
            MyService service;
        }
    }

    @Test
    void priorityTest() {
        Injector injector = Injector.create().bindImplicit(PriorityTest.class);
        PriorityTest.MyMojo mojo = injector.getInstance(PriorityTest.MyMojo.class);
        assertNotNull(mojo);
        assertInstanceOf(PriorityTest.MyPriorityServiceImpl.class, mojo.service);
    }

    static class PriorityTest {

        interface MyService {}

        @Named
        static class MyServiceImpl implements MyService {}

        @Named
        @Priority(10)
        static class MyPriorityServiceImpl implements MyService {}

        @Named
        static class MyMojo {
            @Inject
            MyService service;
        }
    }

    @Test
    void mojoTest() {
        Injector injector = Injector.create().bindImplicit(MojoTest.class);
        MojoTest.MyMojo mojo = injector.getInstance(MojoTest.MyMojo.class);
        assertNotNull(mojo);
    }

    @SuppressWarnings("unused")
    static class MojoTest {
        @Qualifier
        @Retention(RUNTIME)
        @interface Mojo {}

        interface MyService {}

        @Named
        static class MyServiceImpl implements MyService {}

        @Mojo
        static class MyMojo {
            @Inject
            MyService service;
        }
    }

    @Test
    void typedTest() {
        Injector injector =
                Injector.create().bindImplicit(TypedTest.MyServiceImpl.class).bindImplicit(TypedTest.MyMojo.class);
        TypedTest.MyMojo mojo = injector.getInstance(TypedTest.MyMojo.class);
        assertNotNull(mojo);
        assertNotNull(mojo.service);
    }

    @SuppressWarnings("unused")
    static class TypedTest {

        interface MyService {}

        @Named
        @Typed
        static class MyServiceImpl implements MyService {}

        @Named
        static class MyMojo {
            @Inject
            MyService service;
        }
    }

    @Test
    public void bindInterfacesTest() {
        Injector injector = Injector.create().bindImplicit(BindInterfaces.class);
        BindInterfaces.TestInterface<String> inst =
                injector.getInstance(new Key<BindInterfaces.TestInterface<String>>() {});
        assertNotNull(inst);
    }

    static class BindInterfaces {

        interface TestInterface<T> {
            T getObj();
        }

        @Named
        static class ClassImpl implements TestInterface<String> {
            @Override
            public String getObj() {
                return null;
            }
        }

        @Named
        @Typed
        static class TypedClassImpl implements TestInterface<String> {
            @Override
            public String getObj() {
                return null;
            }
        }
    }

    @Test
    void injectListTest() {
        Injector injector = Injector.create().bindImplicit(InjectList.class);
        List<InjectList.MyService> services = injector.getInstance(new Key<List<InjectList.MyService>>() {});
        assertNotNull(services);
        assertEquals(2, services.size());

        assertNotNull(services.get(0));
        assertInstanceOf(InjectList.MyService.class, services.get(0));
        assertNotNull(services.get(1));
        assertInstanceOf(InjectList.MyService.class, services.get(1));
        assertNotSame(services.get(0).getClass(), services.get(1).getClass());
    }

    @Test
    void injectListWithPriorityTest() {
        Injector injector = Injector.create().bindImplicit(InjectListWithPriority.class);
        List<InjectListWithPriority.MyService> services =
                injector.getInstance(new Key<List<InjectListWithPriority.MyService>>() {});
        assertNotNull(services);
        assertEquals(3, services.size());

        // Verify services are ordered by priority (highest first)
        assertInstanceOf(InjectListWithPriority.HighPriorityServiceImpl.class, services.get(0));
        assertInstanceOf(InjectListWithPriority.MediumPriorityServiceImpl.class, services.get(1));
        assertInstanceOf(InjectListWithPriority.LowPriorityServiceImpl.class, services.get(2));
    }

    static class InjectList {

        interface MyService {}

        @Named("foo")
        static class MyServiceImpl implements MyService {}

        @Named("bar")
        static class AnotherServiceImpl implements MyService {}
    }

    static class InjectListWithPriority {

        interface MyService {}

        @Named
        @Priority(100)
        static class HighPriorityServiceImpl implements MyService {}

        @Named
        @Priority(50)
        static class MediumPriorityServiceImpl implements MyService {}

        @Named
        @Priority(10)
        static class LowPriorityServiceImpl implements MyService {}
    }

    @Test
    void injectMapTest() {
        Injector injector = Injector.create().bindImplicit(InjectMap.class);
        Map<String, InjectMap.MyService> services =
                injector.getInstance(new Key<Map<String, InjectMap.MyService>>() {});
        assertNotNull(services);
        assertEquals(2, services.size());

        List<Map.Entry<String, InjectMap.MyService>> entries = new ArrayList<>(services.entrySet());
        assertNotNull(entries.get(0));
        assertInstanceOf(InjectMap.MyService.class, entries.get(0).getValue());
        assertInstanceOf(String.class, entries.get(0).getKey());
        assertNotNull(entries.get(1));
        assertInstanceOf(String.class, entries.get(1).getKey());
        assertInstanceOf(InjectMap.MyService.class, entries.get(1).getValue());
        assertNotEquals(entries.get(0).getKey(), entries.get(1).getKey());
        assertNotSame(
                entries.get(0).getValue().getClass(), entries.get(1).getValue().getClass());

        InjectMap.MyMojo mojo = injector.getInstance(InjectMap.MyMojo.class);
        assertNotNull(mojo);
        assertNotNull(mojo.services);
        assertEquals(2, mojo.services.size());
    }

    static class InjectMap {

        interface MyService {}

        @Named("foo")
        static class MyServiceImpl implements MyService {}

        @Named("bar")
        static class AnotherServiceImpl implements MyService {}

        @Named
        static class MyMojo {
            @Inject
            Map<String, MyService> services;
        }
    }

    @Test
    void testSingleton() {
        Injector injector = Injector.create()
                .bindImplicit(SingletonContainer.Bean1.class)
                .bindImplicit(SingletonContainer.Bean2.class);

        SingletonContainer.Bean1 b1a = injector.getInstance(SingletonContainer.Bean1.class);
        assertNotNull(b1a);
        SingletonContainer.Bean1 b1b = injector.getInstance(SingletonContainer.Bean1.class);
        assertNotNull(b1b);
        assertEquals(b1a.num, b1b.num);

        SingletonContainer.Bean2 b2a = injector.getInstance(SingletonContainer.Bean2.class);
        assertNotNull(b2a);
        SingletonContainer.Bean2 b2b = injector.getInstance(SingletonContainer.Bean2.class);
        assertNotNull(b2b);
        assertNotEquals(b2a.num, b2b.num);
    }

    static class SingletonContainer {
        private static final AtomicInteger BEAN_1 = new AtomicInteger();
        private static final AtomicInteger BEAN_2 = new AtomicInteger();

        @Named
        @Singleton
        static class Bean1 {
            int num = BEAN_1.incrementAndGet();
        }

        @Named
        static class Bean2 {
            int num = BEAN_2.incrementAndGet();
        }
    }

    @Test
    void testProvides() {
        Injector injector = Injector.create().bindImplicit(ProvidesContainer.class);

        assertNotNull(injector.getInstance(String.class));
    }

    static class ProvidesContainer {

        @Provides
        static ArrayList<String> newStringList() {
            return new ArrayList<>(Arrays.asList("foo", "bar"));
        }

        @Provides
        static String newStringOfList(List<String> list) {
            return list.toString();
        }
    }

    @Test
    void testInjectConstructor() {
        Injector injector = Injector.create().bindImplicit(InjectConstructorContainer.class);

        assertNotNull(injector.getInstance(InjectConstructorContainer.Bean.class));
    }

    static class InjectConstructorContainer {
        @Named
        static class Bean {
            @Inject
            Bean(Another another, Third third) {}

            Bean() {}
        }

        @Named
        static class Another {}

        @Named
        static class Third {}
    }

    @Test
    void testNullableOnField() {
        Injector injector = Injector.create().bindImplicit(NullableOnField.class);
        NullableOnField.MyMojo mojo = injector.getInstance(NullableOnField.MyMojo.class);
        assertNotNull(mojo);
        assertNull(mojo.service);
    }

    static class NullableOnField {

        @Named
        interface MyService {}

        @Named
        static class MyMojo {
            @Inject
            @Nullable
            MyService service;
        }
    }

    @Test
    void testNullableOnConstructor() {
        Injector injector = Injector.create().bindImplicit(NullableOnConstructor.class);
        NullableOnConstructor.MyMojo mojo = injector.getInstance(NullableOnConstructor.MyMojo.class);
        assertNotNull(mojo);
        assertNull(mojo.service);
    }

    static class NullableOnConstructor {

        @Named
        interface MyService {}

        @Named
        static class MyMojo {
            private final MyService service;

            @Inject
            MyMojo(@Nullable MyService service) {
                this.service = service;
            }
        }
    }

    @Test
    void testCircularPriorityDependency() {
        Injector injector = Injector.create().bindImplicit(CircularPriorityTest.class);

        DIException exception = assertThrows(DIException.class, () -> {
            injector.getInstance(CircularPriorityTest.MyService.class);
        });
        assertInstanceOf(DIException.class, exception, "Expected exception to be DIException");
        assertTrue(
                exception.getMessage().contains("HighPriorityServiceImpl"),
                "Expected exception message to contain 'HighPriorityServiceImpl' but was: " + exception.getMessage());

        assertInstanceOf(DIException.class, exception.getCause(), "Expected cause to be DIException");
        assertTrue(
                exception.getCause().getMessage().contains("Cyclic dependency detected"),
                "Expected cause message to contain 'Cyclic dependency detected' but was: "
                        + exception.getCause().getMessage());
        assertTrue(
                exception.getCause().getMessage().contains("MyService"),
                "Expected cause message to contain 'MyService' but was: "
                        + exception.getCause().getMessage());
    }

    @Test
    void testListInjectionWithMixedPriorities() {
        Injector injector = Injector.create().bindImplicit(MixedPriorityTest.class);
        List<MixedPriorityTest.MyService> services =
                injector.getInstance(new Key<List<MixedPriorityTest.MyService>>() {});
        assertNotNull(services);
        assertEquals(4, services.size());

        // Verify services are ordered by priority (highest first)
        // Priority 200 (highest)
        assertInstanceOf(MixedPriorityTest.VeryHighPriorityServiceImpl.class, services.get(0));
        // Priority 100
        assertInstanceOf(MixedPriorityTest.HighPriorityServiceImpl.class, services.get(1));
        // Priority 50
        assertInstanceOf(MixedPriorityTest.MediumPriorityServiceImpl.class, services.get(2));
        // No priority annotation (default 0)
        assertInstanceOf(MixedPriorityTest.DefaultPriorityServiceImpl.class, services.get(3));
    }

    static class CircularPriorityTest {
        interface MyService {}

        @Named
        static class DefaultServiceImpl implements MyService {}

        @Named
        @Priority(10)
        static class HighPriorityServiceImpl implements MyService {
            @Inject
            MyService defaultService; // This tries to inject the default implementation
        }
    }

    static class MixedPriorityTest {

        interface MyService {}

        @Named
        @Priority(200)
        static class VeryHighPriorityServiceImpl implements MyService {}

        @Named
        @Priority(100)
        static class HighPriorityServiceImpl implements MyService {}

        @Named
        @Priority(50)
        static class MediumPriorityServiceImpl implements MyService {}

        @Named
        static class DefaultPriorityServiceImpl implements MyService {}
    }

    @Test
    void testDisposeClearsBindingsAndCache() {
        final Injector injector = Injector.create()
                // bind two simple beans
                .bindImplicit(DisposeTest.Foo.class)
                .bindImplicit(DisposeTest.Bar.class);

        // make sure they really get created
        assertNotNull(injector.getInstance(DisposeTest.Foo.class));
        assertNotNull(injector.getInstance(DisposeTest.Bar.class));

        // now dispose
        injector.dispose();

        // after dispose, bindings should be gone => DIException on lookup
        assertThrows(DIException.class, () -> injector.getInstance(DisposeTest.Foo.class));
        assertThrows(DIException.class, () -> injector.getInstance(DisposeTest.Bar.class));
    }

    /**
     * Simple test classes for dispose().
     */
    static class DisposeTest {
        @Named
        static class Foo {}

        @Named
        static class Bar {}
    }

    // ============================================================================
    // Collection Aggregation Tests
    // ============================================================================

    @Test
    void testListAggregationFromMultipleNamedBeans() {
        Injector injector = Injector.create().bindImplicit(ListAggregationTest.class);

        List<ListAggregationTest.MyService> services =
                injector.getInstance(new Key<List<ListAggregationTest.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());

        // Verify all three implementations are present
        assertTrue(services.stream().anyMatch(s -> s instanceof ListAggregationTest.FooService));
        assertTrue(services.stream().anyMatch(s -> s instanceof ListAggregationTest.BarService));
        assertTrue(services.stream().anyMatch(s -> s instanceof ListAggregationTest.BazService));
    }

    static class ListAggregationTest {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Named("baz")
        static class BazService implements MyService {}
    }

    @Test
    void testListAggregationFromProvidesMethod() {
        Injector injector = Injector.create().bindImplicit(ListAggregationFromProvides.class);

        List<ListAggregationFromProvides.MyService> services =
                injector.getInstance(new Key<List<ListAggregationFromProvides.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());
    }

    static class ListAggregationFromProvides {
        interface MyService {}

        @Provides
        MyService foo() {
            return new MyService() {};
        }

        @Provides
        MyService bar() {
            return new MyService() {};
        }

        @Provides
        MyService baz() {
            return new MyService() {};
        }
    }

    @Test
    void testListAggregationMixedNamedAndProvides() {
        Injector injector = Injector.create().bindImplicit(ListAggregationMixed.class);

        List<ListAggregationMixed.MyService> services =
                injector.getInstance(new Key<List<ListAggregationMixed.MyService>>() {});

        assertNotNull(services);
        assertEquals(4, services.size());
    }

    static class ListAggregationMixed {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Provides
        MyService provided1() {
            return new MyService() {};
        }

        @Provides
        MyService provided2() {
            return new MyService() {};
        }
    }

    @Test
    void testEmptyListWhenNoBeansAvailable() {
        Injector injector = Injector.create().bindImplicit(EmptyListTest.class);

        List<EmptyListTest.NonExistentService> services =
                injector.getInstance(new Key<List<EmptyListTest.NonExistentService>>() {});

        assertNotNull(services);
        assertEquals(0, services.size());
    }

    static class EmptyListTest {
        interface NonExistentService {}
    }

    @Test
    void testMapAggregationFromMultipleNamedBeans() {
        Injector injector = Injector.create().bindImplicit(MapAggregationTest.class);

        Map<String, MapAggregationTest.MyService> services =
                injector.getInstance(new Key<Map<String, MapAggregationTest.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());

        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("baz"));

        assertInstanceOf(MapAggregationTest.FooService.class, services.get("foo"));
        assertInstanceOf(MapAggregationTest.BarService.class, services.get("bar"));
        assertInstanceOf(MapAggregationTest.BazService.class, services.get("baz"));
    }

    static class MapAggregationTest {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Named("baz")
        static class BazService implements MyService {}
    }

    @Test
    void testMapAggregationFromNamedProvidesMethod() {
        Injector injector = Injector.create().bindImplicit(MapAggregationFromProvides.class);

        Map<String, MapAggregationFromProvides.MyService> services =
                injector.getInstance(new Key<Map<String, MapAggregationFromProvides.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());

        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("baz"));
    }

    static class MapAggregationFromProvides {
        interface MyService {}

        @Provides
        @Named("foo")
        MyService foo() {
            return new MyService() {};
        }

        @Provides
        @Named("bar")
        MyService bar() {
            return new MyService() {};
        }

        @Provides
        @Named("baz")
        MyService baz() {
            return new MyService() {};
        }
    }

    @Test
    void testMapAggregationMixedNamedAndProvides() {
        Injector injector = Injector.create().bindImplicit(MapAggregationMixed.class);

        Map<String, MapAggregationMixed.MyService> services =
                injector.getInstance(new Key<Map<String, MapAggregationMixed.MyService>>() {});

        assertNotNull(services);
        assertEquals(4, services.size());

        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("provided1"));
        assertTrue(services.containsKey("provided2"));
    }

    static class MapAggregationMixed {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Provides
        @Named("provided1")
        MyService provided1() {
            return new MyService() {};
        }

        @Provides
        @Named("provided2")
        MyService provided2() {
            return new MyService() {};
        }
    }

    @Test
    void testEmptyMapWhenNoNamedBeansAvailable() {
        Injector injector = Injector.create().bindImplicit(EmptyMapTest.class);

        Map<String, EmptyMapTest.NonExistentService> services =
                injector.getInstance(new Key<Map<String, EmptyMapTest.NonExistentService>>() {});

        assertNotNull(services);
        assertEquals(0, services.size());
    }

    static class EmptyMapTest {
        interface NonExistentService {}
    }

    @Test
    void testMapIgnoresUnnamedBeans() {
        Injector injector = Injector.create().bindImplicit(MapIgnoresUnnamed.class);

        Map<String, MapIgnoresUnnamed.MyService> services =
                injector.getInstance(new Key<Map<String, MapIgnoresUnnamed.MyService>>() {});

        assertNotNull(services);
        assertEquals(2, services.size()); // Only foo and bar, not unnamed

        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
    }

    static class MapIgnoresUnnamed {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Named // No value, so not added to map
        static class UnnamedService implements MyService {}
    }

    @Test
    void testListAggregationWithPriorityOrdering() {
        Injector injector = Injector.create().bindImplicit(ListPriorityOrdering.class);

        List<ListPriorityOrdering.MyService> services =
                injector.getInstance(new Key<List<ListPriorityOrdering.MyService>>() {});

        assertNotNull(services);
        assertEquals(4, services.size());

        // Verify priority ordering: highest priority first
        assertInstanceOf(ListPriorityOrdering.HighPriority.class, services.get(0));
        assertInstanceOf(ListPriorityOrdering.MediumPriority.class, services.get(1));
        assertInstanceOf(ListPriorityOrdering.LowPriority.class, services.get(2));
        assertInstanceOf(ListPriorityOrdering.NoPriority.class, services.get(3));
    }

    static class ListPriorityOrdering {
        interface MyService {}

        @Named
        @Priority(100)
        static class HighPriority implements MyService {}

        @Named
        @Priority(50)
        static class MediumPriority implements MyService {}

        @Named
        @Priority(10)
        static class LowPriority implements MyService {}

        @Named
        static class NoPriority implements MyService {}
    }

    @Test
    void testInjectListIntoMojo() {
        Injector injector = Injector.create().bindImplicit(InjectListIntoMojo.class);

        InjectListIntoMojo.MyMojo mojo = injector.getInstance(InjectListIntoMojo.MyMojo.class);

        assertNotNull(mojo);
        assertNotNull(mojo.services);
        assertEquals(3, mojo.services.size());
    }

    static class InjectListIntoMojo {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Named("baz")
        static class BazService implements MyService {}

        @Named
        static class MyMojo {
            @Inject
            List<MyService> services;
        }
    }

    @Test
    void testInjectMapIntoMojoViaConstructor() {
        Injector injector = Injector.create().bindImplicit(InjectMapConstructor.class);

        InjectMapConstructor.MyMojo mojo = injector.getInstance(InjectMapConstructor.MyMojo.class);

        assertNotNull(mojo);
        assertNotNull(mojo.services);
        assertEquals(2, mojo.services.size());
        assertTrue(mojo.services.containsKey("foo"));
        assertTrue(mojo.services.containsKey("bar"));
    }

    static class InjectMapConstructor {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Named
        static class MyMojo {
            final Map<String, MyService> services;

            @Inject
            MyMojo(Map<String, MyService> services) {
                this.services = services;
            }
        }
    }

    @Test
    void testListAggregationWithSingletonScope() {
        Injector injector = Injector.create().bindImplicit(ListSingletonScope.class);

        List<ListSingletonScope.MyService> services1 =
                injector.getInstance(new Key<List<ListSingletonScope.MyService>>() {});
        List<ListSingletonScope.MyService> services2 =
                injector.getInstance(new Key<List<ListSingletonScope.MyService>>() {});

        assertNotNull(services1);
        assertNotNull(services2);
        assertEquals(2, services1.size());
        assertEquals(2, services2.size());

        // Singleton beans should be the same instance
        ListSingletonScope.MyService singleton1a = services1.stream()
                .filter(s -> s instanceof ListSingletonScope.SingletonService)
                .findFirst()
                .orElse(null);
        ListSingletonScope.MyService singleton1b = services2.stream()
                .filter(s -> s instanceof ListSingletonScope.SingletonService)
                .findFirst()
                .orElse(null);

        assertNotNull(singleton1a);
        assertNotNull(singleton1b);
        assertEquals(singleton1a, singleton1b); // Same instance

        // Non-singleton beans should be different instances
        ListSingletonScope.MyService nonSingleton1a = services1.stream()
                .filter(s -> s instanceof ListSingletonScope.NonSingletonService)
                .findFirst()
                .orElse(null);
        ListSingletonScope.MyService nonSingleton1b = services2.stream()
                .filter(s -> s instanceof ListSingletonScope.NonSingletonService)
                .findFirst()
                .orElse(null);

        assertNotNull(nonSingleton1a);
        assertNotNull(nonSingleton1b);
        assertNotEquals(nonSingleton1a, nonSingleton1b); // Different instances
    }

    static class ListSingletonScope {
        interface MyService {}

        @Named
        @Singleton
        static class SingletonService implements MyService {}

        @Named
        static class NonSingletonService implements MyService {}
    }

    @Test
    void testMapAggregationWithQualifiers() {
        Injector injector = Injector.create().bindImplicit(MapWithQualifiers.class);

        Map<String, MapWithQualifiers.MyService> services =
                injector.getInstance(new Key<Map<String, MapWithQualifiers.MyService>>() {});

        assertNotNull(services);
        // Should only include @Named beans, not other qualifiers
        assertEquals(2, services.size());
        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
    }

    static class MapWithQualifiers {
        @Qualifier
        @Retention(RUNTIME)
        @interface CustomQualifier {}

        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @CustomQualifier
        static class QualifiedService implements MyService {}
    }

    @Test
    void testNestedListAndMapInjection() {
        Injector injector = Injector.create().bindImplicit(NestedCollections.class);

        NestedCollections.Aggregator aggregator = injector.getInstance(NestedCollections.Aggregator.class);

        assertNotNull(aggregator);
        assertNotNull(aggregator.allServices);
        assertNotNull(aggregator.namedServices);

        assertEquals(3, aggregator.allServices.size());
        assertEquals(3, aggregator.namedServices.size());
    }

    static class NestedCollections {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Named("baz")
        static class BazService implements MyService {}

        @Named
        static class Aggregator {
            @Inject
            List<MyService> allServices;

            @Inject
            Map<String, MyService> namedServices;
        }
    }

    @Test
    void testListAggregationWithTypedAnnotation() {
        Injector injector = Injector.create().bindImplicit(ListWithTyped.class);

        List<ListWithTyped.MyService> services = injector.getInstance(new Key<List<ListWithTyped.MyService>>() {});

        assertNotNull(services);
        // @Typed beans should only be accessible by their explicit types
        assertEquals(1, services.size());
        assertInstanceOf(ListWithTyped.RegularService.class, services.get(0));
    }

    static class ListWithTyped {
        interface MyService {}

        @Named
        static class RegularService implements MyService {}

        @Named
        @Typed(ListWithTyped.SpecificInterface.class)
        static class TypedService implements MyService, SpecificInterface {}

        interface SpecificInterface {}
    }

    // ============================================================================
    // @Aggregate Annotation Tests
    // ============================================================================

    @Test
    void testAggregateMapContribution() {
        Injector injector = Injector.create().bindImplicit(AggregateMapTest.class);

        Map<String, AggregateMapTest.MyService> services =
                injector.getInstance(new Key<Map<String, AggregateMapTest.MyService>>() {});

        assertNotNull(services);
        assertEquals(4, services.size());

        // Should contain all entries from both modules
        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("extra1"));
        assertTrue(services.containsKey("extra2"));
    }

    static class AggregateMapTest {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        // Bulk contribution using @Aggregate
        @Provides
        @Aggregate
        Map<String, MyService> extraServices() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("extra1", new MyService() {});
            map.put("extra2", new MyService() {});
            return map;
        }
    }

    @Test
    void testAggregateListContribution() {
        Injector injector = Injector.create().bindImplicit(AggregateListTest.class);

        List<AggregateListTest.MyService> services =
                injector.getInstance(new Key<List<AggregateListTest.MyService>>() {});

        assertNotNull(services);
        assertEquals(5, services.size()); // 2 named + 3 from aggregate
    }

    static class AggregateListTest {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        // Bulk contribution using @Aggregate
        @Provides
        @Aggregate
        List<MyService> extraServices() {
            return Arrays.asList(new MyService() {}, new MyService() {}, new MyService() {});
        }
    }

    @Test
    void testExplicitMapProviderWithoutAggregate() {
        Injector injector = Injector.create().bindImplicit(ExplicitMapProvider.class);

        Map<String, ExplicitMapProvider.MyService> services =
                injector.getInstance(new Key<Map<String, ExplicitMapProvider.MyService>>() {});

        assertNotNull(services);
        // Without @Aggregate, explicit provider REPLACES auto-aggregation
        assertEquals(2, services.size());
        assertTrue(services.containsKey("explicit1"));
        assertTrue(services.containsKey("explicit2"));

        // "foo" and "bar" should NOT be in the map
        assertFalse(services.containsKey("foo"));
        assertFalse(services.containsKey("bar"));
    }

    static class ExplicitMapProvider {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        // Explicit provider WITHOUT @Aggregate replaces auto-aggregation
        @Provides
        Map<String, MyService> explicitMap() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("explicit1", new MyService() {});
            map.put("explicit2", new MyService() {});
            return map;
        }
    }

    @Test
    void testExplicitListProviderWithoutAggregate() {
        Injector injector = Injector.create().bindImplicit(ExplicitListProvider.class);

        List<ExplicitListProvider.MyService> services =
                injector.getInstance(new Key<List<ExplicitListProvider.MyService>>() {});

        assertNotNull(services);
        // Without @Aggregate, explicit provider REPLACES auto-aggregation
        assertEquals(2, services.size());

        // Should only contain services from the explicit provider
        // Not the @Named beans
    }

    static class ExplicitListProvider {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        // Explicit provider WITHOUT @Aggregate replaces auto-aggregation
        @Provides
        List<MyService> explicitList() {
            return Arrays.asList(new MyService() {}, new MyService() {});
        }
    }

    @Test
    void testMultipleAggregateProviders() {
        Injector injector = Injector.create().bindImplicit(MultipleAggregateProviders.class);

        Map<String, MultipleAggregateProviders.MyService> services =
                injector.getInstance(new Key<Map<String, MultipleAggregateProviders.MyService>>() {});

        assertNotNull(services);
        assertEquals(6, services.size());

        // Should contain entries from all sources
        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("module1"));
        assertTrue(services.containsKey("module2"));
        assertTrue(services.containsKey("module3"));
        assertTrue(services.containsKey("module4"));
    }

    static class MultipleAggregateProviders {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Provides
        @Aggregate
        Map<String, MyService> moduleA() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("module1", new MyService() {});
            map.put("module2", new MyService() {});
            return map;
        }

        @Provides
        @Aggregate
        Map<String, MyService> moduleB() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("module3", new MyService() {});
            map.put("module4", new MyService() {});
            return map;
        }
    }

    @Test
    void testAggregateWithDuplicateKeys() {
        Injector injector = Injector.create().bindImplicit(AggregateWithDuplicates.class);

        Map<String, AggregateWithDuplicates.MyService> services =
                injector.getInstance(new Key<Map<String, AggregateWithDuplicates.MyService>>() {});

        assertNotNull(services);
        // When duplicate keys exist, last one wins (or you could throw an exception)
        assertEquals(3, services.size());
        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("duplicate"));
    }

    static class AggregateWithDuplicates {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Provides
        @Aggregate
        Map<String, MyService> module1() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("duplicate", new MyService() {});
            return map;
        }

        @Provides
        @Aggregate
        Map<String, MyService> module2() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("duplicate", new MyService() {}); // Same key
            return map;
        }
    }

    @Test
    void testAggregateSingleBeanToMap() {
        Injector injector = Injector.create().bindImplicit(AggregateSingleToMap.class);

        Map<String, AggregateSingleToMap.MyService> services =
                injector.getInstance(new Key<Map<String, AggregateSingleToMap.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());

        assertTrue(services.containsKey("foo"));
        assertTrue(services.containsKey("bar"));
        assertTrue(services.containsKey("extra"));
    }

    static class AggregateSingleToMap {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        // Single bean with @Aggregate and @Named - contributes to map
        @Provides
        @Aggregate
        @Named("extra")
        MyService extra() {
            return new MyService() {};
        }
    }

    @Test
    void testAggregateSingleBeanToList() {
        Injector injector = Injector.create().bindImplicit(AggregateSingleToList.class);

        List<AggregateSingleToList.MyService> services =
                injector.getInstance(new Key<List<AggregateSingleToList.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());
    }

    static class AggregateSingleToList {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        // Single bean with @Aggregate - contributes to list
        @Provides
        @Aggregate
        MyService extra() {
            return new MyService() {};
        }
    }

    @Test
    void testAggregateEmptyCollections() {
        Injector injector = Injector.create().bindImplicit(AggregateEmptyCollections.class);

        Map<String, AggregateEmptyCollections.MyService> mapServices =
                injector.getInstance(new Key<Map<String, AggregateEmptyCollections.MyService>>() {});
        List<AggregateEmptyCollections.MyService> listServices =
                injector.getInstance(new Key<List<AggregateEmptyCollections.MyService>>() {});

        assertNotNull(mapServices);
        assertNotNull(listServices);

        // Empty @Aggregate contributions should not cause errors
        assertEquals(2, mapServices.size());
        assertEquals(2, listServices.size());
    }

    static class AggregateEmptyCollections {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Named("bar")
        static class BarService implements MyService {}

        @Provides
        @Aggregate
        Map<String, MyService> emptyMap() {
            return new java.util.HashMap<>();
        }

        @Provides
        @Aggregate
        List<MyService> emptyList() {
            return new ArrayList<>();
        }
    }

    @Test
    void testAggregateWithPriorityInList() {
        Injector injector = Injector.create().bindImplicit(AggregateWithPriority.class);

        List<AggregateWithPriority.MyService> services =
                injector.getInstance(new Key<List<AggregateWithPriority.MyService>>() {});

        assertNotNull(services);
        assertEquals(5, services.size());

        // Priority ordering should apply to aggregated items too
        assertInstanceOf(AggregateWithPriority.HighPriority.class, services.get(0));
        assertInstanceOf(AggregateWithPriority.LowPriority.class, services.get(services.size() - 1));
    }

    static class AggregateWithPriority {
        interface MyService {}

        @Named
        @Priority(100)
        static class HighPriority implements MyService {}

        @Named
        @Priority(-10)
        static class LowPriority implements MyService {}

        @Provides
        @Aggregate
        List<MyService> extraServices() {
            return Arrays.asList(new MyService() {}, new MyService() {}, new MyService() {});
        }
    }

    @Test
    void testMixedAggregateAndExplicitProviders() {
        Injector injector = Injector.create().bindImplicit(MixedAggregateExplicit.class);

        // When both @Aggregate and explicit (non-@Aggregate) providers exist,
        // the explicit one should win and replace everything
        Map<String, MixedAggregateExplicit.MyService> services =
                injector.getInstance(new Key<Map<String, MixedAggregateExplicit.MyService>>() {});

        assertNotNull(services);
        assertEquals(2, services.size());
        assertTrue(services.containsKey("explicit1"));
        assertTrue(services.containsKey("explicit2"));

        // Aggregate contributions should be ignored when explicit provider exists
        assertFalse(services.containsKey("foo"));
        assertFalse(services.containsKey("aggregate1"));
    }

    static class MixedAggregateExplicit {
        interface MyService {}

        @Named("foo")
        static class FooService implements MyService {}

        @Provides
        @Aggregate
        Map<String, MyService> aggregateMap() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("aggregate1", new MyService() {});
            return map;
        }

        // Explicit provider (no @Aggregate) takes precedence
        @Provides
        Map<String, MyService> explicitMap() {
            Map<String, MyService> map = new java.util.HashMap<>();
            map.put("explicit1", new MyService() {});
            map.put("explicit2", new MyService() {});
            return map;
        }
    }

    @Test
    void testPriorityOnProvidesMethod() {
        Injector injector = Injector.create().bindImplicit(PriorityProvidesTest.class);

        List<PriorityProvidesTest.MyService> services =
                injector.getInstance(new Key<List<PriorityProvidesTest.MyService>>() {});

        assertNotNull(services);
        assertEquals(3, services.size());

        // Should be ordered by priority: High (100), Medium (50), Low (10)
        assertInstanceOf(PriorityProvidesTest.HighPriorityService.class, services.get(0));
        assertInstanceOf(PriorityProvidesTest.MediumPriorityService.class, services.get(1));
        assertInstanceOf(PriorityProvidesTest.LowPriorityService.class, services.get(2));
    }

    static class PriorityProvidesTest {
        interface MyService {}

        static class HighPriorityService implements MyService {}

        static class MediumPriorityService implements MyService {}

        static class LowPriorityService implements MyService {}

        @Provides
        @Priority(100)
        @Named("high")
        MyService highPriorityService() {
            return new HighPriorityService();
        }

        @Provides
        @Priority(50)
        @Named("medium")
        MyService mediumPriorityService() {
            return new MediumPriorityService();
        }

        @Provides
        @Priority(10)
        @Named("low")
        MyService lowPriorityService() {
            return new LowPriorityService();
        }
    }

    @Test
    void testAggregateMapContributesToNamedInjection() {
        Injector injector = Injector.create()
                .bindImplicit(AggregateMapToNamedTest.class)
                .bindImplicit(AggregateMapToNamedTest.ServiceConsumer.class);

        // Should be able to inject individual named services from @Aggregate Map provider
        AggregateMapToNamedTest.ServiceConsumer consumer =
                injector.getInstance(AggregateMapToNamedTest.ServiceConsumer.class);

        assertNotNull(consumer);
        assertNotNull(consumer.fooService);
        assertNotNull(consumer.barService);

        // Verify these are the services from the @Aggregate Map
        assertEquals("foo-service", consumer.fooService.getName());
        assertEquals("bar-service", consumer.barService.getName());
    }

    static class AggregateMapToNamedTest {
        interface MyService {
            String getName();
        }

        static class ServiceConsumer {
            @Inject
            @Named("foo")
            MyService fooService;

            @Inject
            @Named("bar")
            MyService barService;
        }

        @Provides
        @Aggregate
        Map<String, MyService> serviceMap() {
            Map<String, MyService> map = new LinkedHashMap<>();
            map.put("foo", () -> "foo-service");
            map.put("bar", () -> "bar-service");
            return map;
        }
    }

    @Test
    void testAggregateListContributesToIndividualInjection() {
        Injector injector = Injector.create()
                .bindImplicit(AggregateListToIndividualTest.class)
                .bindImplicit(AggregateListToIndividualTest.ServiceConsumer.class);

        // Should be able to inject individual services from @Aggregate List provider
        AggregateListToIndividualTest.ServiceConsumer consumer =
                injector.getInstance(AggregateListToIndividualTest.ServiceConsumer.class);

        assertNotNull(consumer);
        assertNotNull(consumer.service);

        // Should get one of the services from the @Aggregate List
        // (the exact one depends on priority/order, but it should be non-null)
        assertNotNull(consumer.service.getName());
        assertTrue(consumer.service.getName().startsWith("service-"));
    }

    static class AggregateListToIndividualTest {
        interface MyService {
            String getName();
        }

        static class ServiceConsumer {
            @Inject
            MyService service;
        }

        @Provides
        @Aggregate
        List<MyService> serviceList() {
            List<MyService> list = new ArrayList<>();
            list.add(() -> "service-1");
            list.add(() -> "service-2");
            return list;
        }
    }
}
