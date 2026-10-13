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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingSelectionTest {
    private static final String NAMED_QUALIFIER = "other";
    private static final String ALTERNATE_QUALIFIER = "another";
    private static final int PREFERRED_PRIORITY = 10;

    @Test
    void defaultProviderWinsOverNamedProvider() {
        InjectorImpl injector = namedFirst(Injector.create().bindImplicit(DefaultAndNamed.class));

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
        assertInstanceOf(NamedService.class, injector.getInstance(Key.of(Service.class, NAMED_QUALIFIER)));
    }

    @Test
    void emptyNamedProviderWinsOverNamedProvider() {
        InjectorImpl injector = namedFirst(Injector.create().bindImplicit(EmptyAndNamed.class));

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
    }

    @Test
    void defaultClassWinsOverNamedClass() {
        InjectorImpl injector =
                namedFirst(Injector.create().bindImplicit(DefaultService.class).bindImplicit(NamedService.class));

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
    }

    @Test
    void anInjectFactoryPreservesTheClassQualifier() {
        InjectorImpl injector = namedFirst(
                Injector.create().bindImplicit(DefaultService.class).bindImplicit(NamedFactoryService.class));

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
        Map<String, Service> services = injector.getInstance(new Key<Map<String, Service>>() {});
        assertInstanceOf(NamedFactoryService.class, services.get(NAMED_QUALIFIER));
    }

    @Test
    void repeatedImplicitRegistrationDoesNotCreateAnotherProvider() {
        Injector injector =
                Injector.create().bindImplicit(DefaultAndNamed.class).bindImplicit(DefaultAndNamed.class);

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
        assertEquals(2, injector.getInstance(new Key<List<Service>>() {}).size());
    }

    @Test
    void inheritedStaticProvidersAreRegisteredOnce() {
        Injector injector = Injector.create().bindImplicit(ParentProvider.class).bindImplicit(ChildProvider.class);

        assertEquals("parent", injector.getInstance(String.class));
        assertEquals(List.of("parent"), injector.getInstance(new Key<List<String>>() {}));
    }

    @Test
    void rebindingTheSameInstanceDoesNotDuplicateItsProviders() {
        InstanceProvider provider = new InstanceProvider();
        Injector injector = Injector.create()
                .bindInstance(InstanceProvider.class, provider)
                .bindInstance(InstanceProvider.class, provider);

        assertEquals("instance", injector.getInstance(String.class));
        assertEquals(provider, injector.getInstance(InstanceProvider.class));
        assertEquals(List.of("instance"), injector.getInstance(new Key<List<String>>() {}));
    }

    @Test
    void rebindingTheSameSupplierDoesNotDuplicateItsProviders() {
        Supplier<InstanceProvider> supplier = InstanceProvider::new;
        Injector injector = Injector.create()
                .bindSupplier(InstanceProvider.class, supplier)
                .bindSupplier(InstanceProvider.class, supplier);

        assertEquals("instance", injector.getInstance(String.class));
        assertEquals(List.of("instance"), injector.getInstance(new Key<List<String>>() {}));
    }

    @Test
    void differentProgrammaticFactoriesRemainAmbiguous() {
        InjectorImpl injector = new InjectorImpl();
        Key<Service> key = Key.of(Service.class);
        injector.bind(key, Binding.to(key, args -> new DefaultService(), new Dependency<?>[0]));
        injector.bind(key, Binding.to(key, args -> new EmptyNamedService(), new Dependency<?>[0]));

        assertThrows(DIException.class, () -> injector.getInstance(Service.class));
    }

    @Test
    void registeringTheSameProgrammaticBindingTwiceDoesNotDuplicateIt() {
        InjectorImpl injector = new InjectorImpl();
        Key<Service> key = Key.of(Service.class);
        Binding<Service> binding = Binding.to(key, args -> new DefaultService(), new Dependency<?>[0]);
        injector.bind(key, binding);
        injector.bind(key, binding);

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
        assertEquals(1, injector.getInstance(new Key<List<Service>>() {}).size());
    }

    @Test
    void differentCustomInitializersRemainDistinct() throws NoSuchMethodException {
        InjectorImpl injector = new InjectorImpl();
        Key<InitializedService> key = Key.of(InitializedService.class);
        Binding<InitializedService> base =
                ReflectionUtils.bindingFromConstructor(key, InitializedService.class.getDeclaredConstructor());
        injector.bind(key, base.initializeWith(tagInitializer("first")));
        injector.bind(key, base.initializeWith(tagInitializer("second")));

        List<InitializedService> services = injector.getInstance(new Key<List<InitializedService>>() {});
        assertEquals(
                Set.of("first", "second"), services.stream().map(s -> s.tag).collect(toSet()));
        assertThrows(DIException.class, () -> injector.getInstance(InitializedService.class));
    }

    @Test
    void aCustomInitializerIsDistinctFromItsRawBinding() throws NoSuchMethodException {
        InjectorImpl injector = new InjectorImpl();
        Key<InitializedService> key = Key.of(InitializedService.class);
        Binding<InitializedService> base =
                ReflectionUtils.bindingFromConstructor(key, InitializedService.class.getDeclaredConstructor());
        injector.bind(key, base);
        injector.bind(key, base.initializeWith(tagInitializer("custom")));

        List<InitializedService> services = injector.getInstance(new Key<List<InitializedService>>() {});
        assertEquals(Set.of("raw", "custom"), services.stream().map(s -> s.tag).collect(toSet()));
        assertThrows(DIException.class, () -> injector.getInstance(InitializedService.class));
    }

    @Test
    void generatedInjectionIsDistinctFromTheRawConstructor() throws NoSuchMethodException {
        InjectorImpl injector = new InjectorImpl();
        injector.bindInstance(String.class, "injected");
        Key<InitializedService> key = Key.of(InitializedService.class);
        Binding<InitializedService> rawBinding =
                ReflectionUtils.bindingFromConstructor(key, InitializedService.class.getDeclaredConstructor());
        Binding<InitializedService> initializedBinding = ReflectionUtils.generateImplicitBinding(key);
        injector.bind(key, rawBinding);
        injector.bind(key, initializedBinding);

        List<InitializedService> services = injector.getInstance(new Key<List<InitializedService>>() {});
        assertEquals(
                Set.of("raw", "injected"), services.stream().map(s -> s.tag).collect(toSet()));
        DIException exception = assertThrows(DIException.class, () -> injector.getInstance(InitializedService.class));
        assertNotEquals(rawBinding.toString(), initializedBinding.toString());
        assertTrue(exception.getMessage().contains(rawBinding.toString()), exception.getMessage());
        assertTrue(exception.getMessage().contains(initializedBinding.toString()), exception.getMessage());
    }

    private static BindingInitializer<InitializedService> tagInitializer(String tag) {
        return new BindingInitializer<>(Set.of()) {
            @Override
            public Consumer<InitializedService> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                return service -> service.tag = tag;
            }
        };
    }

    @Test
    void ambiguityDiagnosticsDoNotInvokeInstanceToString() {
        Injector injector = Injector.create()
                .bindInstance(Service.class, new ExplosiveService())
                .bindInstance(Service.class, new DefaultService());

        DIException exception = assertThrows(DIException.class, () -> injector.getInstance(Service.class));
        assertTrue(exception.getMessage().contains("ExplosiveService"));
        assertTrue(exception.getMessage().contains("DefaultService"));
    }

    @Test
    void ambiguityDiagnosticsDoNotInvokeSupplierToString() {
        Injector injector = Injector.create()
                .bindSupplier(Service.class, new ExplosiveSupplier())
                .bindSupplier(Service.class, DefaultService::new);

        DIException exception = assertThrows(DIException.class, () -> injector.getInstance(Service.class));
        assertTrue(exception.getMessage().contains("ExplosiveSupplier"));
    }

    @Test
    void namedInstanceDoesNotBecomeDefault() {
        InjectorImpl injector = namedFirst(Injector.create()
                .bindInstance(Service.class, new DefaultService())
                .bindInstance(NamedService.class, new NamedService()));

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
        Map<String, Service> services = injector.getInstance(new Key<Map<String, Service>>() {});
        assertInstanceOf(NamedService.class, services.get(NAMED_QUALIFIER));
    }

    @Test
    void namedSupplierDoesNotBecomeDefault() {
        InjectorImpl injector = namedFirst(Injector.create()
                .bindSupplier(Service.class, DefaultService::new)
                .bindSupplier(NamedService.class, NamedService::new));

        assertInstanceOf(DefaultService.class, injector.getInstance(Service.class));
        Map<String, Service> services = injector.getInstance(new Key<Map<String, Service>>() {});
        assertInstanceOf(NamedService.class, services.get(NAMED_QUALIFIER));
    }

    @Test
    void higherPriorityNamedProviderWins() {
        Injector injector = Injector.create().bindImplicit(HigherPriorityNamed.class);

        assertInstanceOf(NamedService.class, injector.getInstance(Service.class));
    }

    @Test
    void aSingleNamedBindingCanSatisfyAnUnqualifiedLookup() {
        Injector injector = Injector.create().bindImplicit(NamedService.class);

        assertInstanceOf(NamedService.class, injector.getInstance(Service.class));
    }

    @Test
    void equalDefaultProvidersAreAmbiguous() {
        Injector injector = Injector.create().bindImplicit(EqualDefaults.class);

        DIException exception = assertThrows(DIException.class, () -> injector.getInstance(Service.class));
        String message = exception.getMessage();
        assertTrue(message.contains("Ambiguous bindings"), message);
        assertTrue(message.contains("Service"), message);
        assertTrue(message.contains("first()"), message);
        assertTrue(message.contains("second()"), message);
        assertTrue(message.contains("priority=0"), message);
    }

    @Test
    void anUnqualifiedClassAndAnEmptyNamedClassAreAmbiguous() {
        Injector injector = Injector.create().bindImplicit(DefaultService.class).bindImplicit(EmptyNamedService.class);

        DIException exception = assertThrows(DIException.class, () -> injector.getInstance(Service.class));
        assertTrue(exception.getMessage().contains("DefaultService"));
        assertTrue(exception.getMessage().contains("EmptyNamedService"));
    }

    @Test
    void equalNamedBindingsAreAmbiguousButRemainAvailableByNameAndInCollections() {
        Injector injector = Injector.create().bindImplicit(EqualNames.class);

        assertThrows(DIException.class, () -> injector.getInstance(Service.class));
        assertInstanceOf(NamedService.class, injector.getInstance(Key.of(Service.class, NAMED_QUALIFIER)));
        assertInstanceOf(DefaultService.class, injector.getInstance(Key.of(Service.class, ALTERNATE_QUALIFIER)));

        List<Service> list = injector.getInstance(new Key<List<Service>>() {});
        assertEquals(2, list.size());
        Map<String, Service> map = injector.getInstance(new Key<Map<String, Service>>() {});
        assertEquals(Set.of(NAMED_QUALIFIER, ALTERNATE_QUALIFIER), map.keySet());
        assertInstanceOf(NamedService.class, map.get(NAMED_QUALIFIER));
        assertInstanceOf(DefaultService.class, map.get(ALTERNATE_QUALIFIER));
    }

    @Test
    void equalBindingsForAnExplicitNameAreAmbiguous() {
        Injector injector = Injector.create().bindImplicit(EqualExplicitNames.class);

        DIException exception =
                assertThrows(DIException.class, () -> injector.getInstance(Key.of(Service.class, NAMED_QUALIFIER)));
        assertTrue(exception.getMessage().contains("@Named(\"other\")"));
        assertTrue(exception.getMessage().contains("first()"));
        assertTrue(exception.getMessage().contains("second()"));
    }

    @Test
    void equalDefaultBindingsRemainAvailableInAList() {
        Injector injector = Injector.create().bindImplicit(EqualDefaults.class);

        List<Service> services = injector.getInstance(new Key<List<Service>>() {});
        assertEquals(2, services.size());
        assertTrue(services.stream().anyMatch(DefaultService.class::isInstance));
        assertTrue(services.stream().anyMatch(EmptyNamedService.class::isInstance));
    }

    @Test
    void lowerPriorityTiesDoNotMakeThePreferredBindingAmbiguous() {
        Injector injector = Injector.create().bindImplicit(UniqueHighestPriority.class);

        assertInstanceOf(NamedService.class, injector.getInstance(Service.class));
    }

    @Test
    void nullableDependenciesDoNotHideAmbiguity() {
        Injector injector = Injector.create().bindImplicit(EqualDefaults.class).bindImplicit(OptionalConsumer.class);

        assertThrows(DIException.class, () -> injector.getInstance(OptionalConsumer.class));
    }

    private static InjectorImpl namedFirst(Injector injector) {
        InjectorImpl impl = (InjectorImpl) injector;
        Map<Key<?>, Set<Binding<?>>> bindings = impl.getBindings();
        // Make the regression independent of HashSet iteration order.
        Set<Binding<?>> ordered = new LinkedHashSet<>(bindings.get(Key.of(Service.class, NAMED_QUALIFIER)));
        ordered.addAll(bindings.get(Key.of(Service.class)));
        bindings.put(Key.of(Service.class), ordered);
        return impl;
    }

    interface Service {}

    static class DefaultService implements Service {}

    @Named
    static class EmptyNamedService implements Service {}

    @Named(NAMED_QUALIFIER)
    static class NamedService implements Service {}

    static class InitializedService {
        @Inject
        String tag = "raw";
    }

    @Named(NAMED_QUALIFIER)
    static class NamedFactoryService implements Service {
        @Inject
        static NamedFactoryService create() {
            return new NamedFactoryService();
        }
    }

    static class ParentProvider {
        @Provides
        static String value() {
            return "parent";
        }
    }

    static class ChildProvider extends ParentProvider {}

    static class InstanceProvider {
        @Provides
        String value() {
            return "instance";
        }
    }

    static class ExplosiveService implements Service {
        @Override
        public String toString() {
            throw new IllegalStateException("instance toString must not be called");
        }
    }

    static class ExplosiveSupplier implements Supplier<Service> {
        @Override
        public Service get() {
            return new DefaultService();
        }

        @Override
        public String toString() {
            throw new IllegalStateException("supplier toString must not be called");
        }
    }

    static class DefaultAndNamed {
        @Provides
        static Service defaultService() {
            return new DefaultService();
        }

        @Provides
        @Named(NAMED_QUALIFIER)
        static Service namedService() {
            return new NamedService();
        }
    }

    static class EmptyAndNamed {
        @Provides
        @Named
        static Service defaultService() {
            return new DefaultService();
        }

        @Provides
        @Named(NAMED_QUALIFIER)
        static Service namedService() {
            return new NamedService();
        }
    }

    static class HigherPriorityNamed {
        @Provides
        static Service defaultService() {
            return new DefaultService();
        }

        @Provides
        @Named(NAMED_QUALIFIER)
        @Priority(PREFERRED_PRIORITY)
        static Service namedService() {
            return new NamedService();
        }
    }

    static class EqualDefaults {
        @Provides
        static Service first() {
            return new DefaultService();
        }

        @Provides
        static Service second() {
            return new EmptyNamedService();
        }
    }

    static class EqualNames {
        @Provides
        @Named(NAMED_QUALIFIER)
        static Service first() {
            return new NamedService();
        }

        @Provides
        @Named(ALTERNATE_QUALIFIER)
        static Service second() {
            return new DefaultService();
        }
    }

    static class EqualExplicitNames {
        @Provides
        @Named(NAMED_QUALIFIER)
        static Service first() {
            return new NamedService();
        }

        @Provides
        @Named(NAMED_QUALIFIER)
        static Service second() {
            return new DefaultService();
        }
    }

    static class UniqueHighestPriority {
        @Provides
        static Service first() {
            return new DefaultService();
        }

        @Provides
        static Service second() {
            return new EmptyNamedService();
        }

        @Provides
        @Priority(PREFERRED_PRIORITY)
        static Service preferred() {
            return new NamedService();
        }
    }

    static class OptionalConsumer {
        @Inject
        @Nullable
        Service service;
    }
}
