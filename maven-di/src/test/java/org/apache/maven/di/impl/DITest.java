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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.di.*;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class DITest {

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

    static class InjectList {

        interface MyService {}

        @Named("foo")
        static class MyServiceImpl implements MyService {}

        @Named("bar")
        static class AnotherServiceImpl implements MyService {}
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
        private static final AtomicInteger bean1 = new AtomicInteger();
        private static final AtomicInteger bean2 = new AtomicInteger();

        @Named
        @Singleton
        static class Bean1 {
            int num = bean1.incrementAndGet();
        }

        @Named
        static class Bean2 {
            int num = bean2.incrementAndGet();
        }
    }
}
