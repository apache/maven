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

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.*;
import org.apache.maven.di.Key;

import static java.util.stream.Collectors.toList;

public final class ReflectionUtils {
    private static final String IDENT = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    private static final Pattern PACKAGE = Pattern.compile("(?:" + IDENT + "\\.)*");
    private static final Pattern PACKAGE_AND_PARENT = Pattern.compile(PACKAGE.pattern() + "(?:" + IDENT + "\\$\\d*)?");
    private static final Pattern ARRAY_SIGNATURE = Pattern.compile("\\[L(.*?);");

    public static String getDisplayName(Type type) {
        Class<?> raw = Types.getRawType(type);
        String typeName;
        if (raw.isAnonymousClass()) {
            Type superclass = raw.getGenericSuperclass();
            typeName = "? extends " + superclass.getTypeName();
        } else {
            typeName = type.getTypeName();
        }

        return PACKAGE_AND_PARENT
                .matcher(ARRAY_SIGNATURE.matcher(typeName).replaceAll("$1[]"))
                .replaceAll("");
    }

    public static @Nullable Object getOuterClassInstance(Object innerClassInstance) {
        if (innerClassInstance == null) {
            return null;
        }
        Class<?> cls = innerClassInstance.getClass();
        Class<?> enclosingClass = cls.getEnclosingClass();
        if (enclosingClass == null) {
            return null;
        }
        for (Field field : cls.getDeclaredFields()) {
            if (!field.isSynthetic() || !field.getName().startsWith("this$") || field.getType() != enclosingClass) {
                continue;
            }
            field.setAccessible(true);
            try {
                return field.get(innerClassInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static @Nullable Object qualifierOf(AnnotatedElement annotatedElement) {
        Object qualifier = null;
        for (Annotation annotation : annotatedElement.getDeclaredAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                if (qualifier != null) {
                    throw new DIException("More than one qualifier annotation on " + annotatedElement);
                }
                if (annotation instanceof Named) {
                    qualifier = ((Named) annotation).value();
                } else {
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    qualifier = Utils.isMarker(annotationType) ? annotationType : annotation;
                }
            }
        }
        return qualifier;
    }

    public static @Nullable Annotation scopeOf(AnnotatedElement annotatedElement) {
        Annotation scope = null;
        for (Annotation annotation : annotatedElement.getDeclaredAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(org.apache.maven.api.di.Scope.class)) {
                if (scope != null) {
                    throw new DIException("More than one scope annotation on " + annotatedElement);
                }
                scope = annotation;
            }
        }
        return scope;
    }

    public static <T> Key<T> keyOf(@Nullable Type container, Type type, AnnotatedElement annotatedElement) {
        return Key.ofType(
                container != null ? Types.bind(type, Types.getAllTypeBindings(container)) : type,
                qualifierOf(annotatedElement));
    }

    public static <T extends AnnotatedElement & Member> List<T> getAnnotatedElements(
            Class<?> cls,
            Class<? extends Annotation> annotationType,
            Function<Class<?>, T[]> extractor,
            boolean allowStatic) {
        List<T> result = new ArrayList<>();
        while (cls != null) {
            for (T element : extractor.apply(cls)) {
                if (element.isAnnotationPresent(annotationType)) {
                    if (!allowStatic && Modifier.isStatic(element.getModifiers())) {
                        throw new DIException(
                                "@" + annotationType.getSimpleName() + " annotation is not allowed on " + element);
                    }
                    result.add(element);
                }
            }
            cls = cls.getSuperclass();
        }
        return result;
    }

    public static <T> @Nullable Binding<T> generateImplicitBinding(Key<T> key) {
        Binding<T> binding = generateConstructorBinding(key);
        if (binding != null) {
            Annotation scope = scopeOf(key.getRawType());
            if (scope != null) {
                binding = binding.scope(scope);
            }
            binding = binding.initializeWith(generateInjectingInitializer(key));
        }
        return binding;
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable Binding<T> generateConstructorBinding(Key<T> key) {
        Class<?> cls = key.getRawType();

        Annotation classInjectAnnotation = Stream.of(cls.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .findAny()
                .orElse(null);
        List<Constructor<?>> constructors = Arrays.asList(cls.getDeclaredConstructors());
        List<Constructor<?>> injectConstructors = constructors.stream()
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(toList());

        List<Method> factoryMethods = Arrays.stream(cls.getDeclaredMethods())
                .filter(method -> method.getReturnType() == cls && Modifier.isStatic(method.getModifiers()))
                .collect(toList());
        List<Method> injectFactoryMethods = factoryMethods.stream()
                .filter(method -> method.isAnnotationPresent(Inject.class))
                .collect(toList());

        if (classInjectAnnotation != null) {
            if (!injectConstructors.isEmpty()) {
                throw failedImplicitBinding(key, "inject annotation on class with inject constructor");
            }
            if (!factoryMethods.isEmpty()) {
                throw failedImplicitBinding(key, "inject annotation on class with factory method");
            }
            if (constructors.isEmpty()) {
                throw failedImplicitBinding(key, "inject annotation on interface");
            }
            if (constructors.size() > 1) {
                throw failedImplicitBinding(key, "inject annotation on class with multiple constructors");
            }
            Constructor<T> declaredConstructor =
                    (Constructor<T>) constructors.iterator().next();

            Class<?> enclosingClass = cls.getEnclosingClass();
            if (enclosingClass != null
                    && !Modifier.isStatic(cls.getModifiers())
                    && declaredConstructor.getParameterCount() != 1) {
                throw failedImplicitBinding(
                        key,
                        "inject annotation on local class that closes over outside variables and/or has no default constructor");
            }
            return bindingFromConstructor(key, declaredConstructor);
        }
        if (!injectConstructors.isEmpty()) {
            if (injectConstructors.size() > 1) {
                throw failedImplicitBinding(key, "more than one inject constructor");
            }
            if (!injectFactoryMethods.isEmpty()) {
                throw failedImplicitBinding(key, "both inject constructor and inject factory method are present");
            }
            return bindingFromConstructor(
                    key, (Constructor<T>) injectConstructors.iterator().next());
        }

        if (!injectFactoryMethods.isEmpty()) {
            if (injectFactoryMethods.size() > 1) {
                throw failedImplicitBinding(key, "more than one inject factory method");
            }
            return bindingFromMethod(injectFactoryMethods.iterator().next());
        }
        return null;
    }

    private static DIException failedImplicitBinding(Key<?> requestedKey, String message) {
        return new DIException(
                "Failed to generate implicit binding for " + requestedKey.getDisplayString() + ", " + message);
    }

    public static <T> BindingInitializer<T> generateInjectingInitializer(Key<T> container) {
        Class<T> rawType = container.getRawType();
        List<BindingInitializer<T>> initializers = Stream.concat(
                        getAnnotatedElements(rawType, Inject.class, Class::getDeclaredFields, false).stream()
                                .map(field -> fieldInjector(container, field)),
                        getAnnotatedElements(rawType, Inject.class, Class::getDeclaredMethods, true).stream()
                                .filter(method -> !Modifier.isStatic(
                                        method.getModifiers())) // we allow them and just filter out to allow
                                // static factory methods
                                .map(method -> methodInjector(container, method)))
                .collect(toList());
        return BindingInitializer.combine(initializers);
    }

    public static <T> BindingInitializer<T> fieldInjector(Key<T> container, Field field) {
        field.setAccessible(true);
        Key<Object> key = keyOf(container.getType(), field.getGenericType(), field);
        return new BindingInitializer<T>(Collections.singleton(key)) {
            @Override
            public Consumer<T> compile(Function<Key<?>, Supplier<?>> compiler) {
                Supplier<?> binding = compiler.apply(key);
                return (T instance) -> {
                    Object arg = binding.get();
                    try {
                        field.set(instance, arg);
                    } catch (IllegalAccessException e) {
                        throw new DIException("Not allowed to set injectable field " + field, e);
                    }
                };
            }
        };
    }

    public static <T> BindingInitializer<T> methodInjector(Key<T> container, Method method) {
        method.setAccessible(true);
        Key<?>[] dependencies = toDependencies(container.getType(), method);
        return new BindingInitializer<T>(new HashSet<>(Arrays.asList(dependencies))) {
            @Override
            public Consumer<T> compile(Function<Key<?>, Supplier<?>> compiler) {
                return instance -> {
                    Object[] args = getDependencies().stream()
                            .map(compiler)
                            .map(Supplier::get)
                            .toArray();
                    try {
                        method.invoke(instance, args);
                    } catch (IllegalAccessException e) {
                        throw new DIException("Not allowed to call injectable method " + method, e);
                    } catch (InvocationTargetException e) {
                        throw new DIException("Failed to call injectable method " + method, e.getCause());
                    }
                };
            }
        };
    }

    public static Key<?>[] toDependencies(@Nullable Type container, Executable executable) {
        Key<?>[] keys = toArgDependencies(container, executable);
        if (executable instanceof Constructor || Modifier.isStatic(executable.getModifiers())) {
            return keys;
        } else {
            Key<?>[] nkeys = new Key[keys.length + 1];
            nkeys[0] = Key.ofType(container);
            System.arraycopy(keys, 0, nkeys, 1, keys.length);
            return nkeys;
        }
    }

    private static Key<?>[] toArgDependencies(@Nullable Type container, Executable executable) {
        Parameter[] parameters = executable.getParameters();
        Key<?>[] dependencies = new Key<?>[parameters.length];
        if (parameters.length == 0) {
            return dependencies;
        }

        Type type = parameters[0].getParameterizedType();
        Parameter parameter = parameters[0];
        dependencies[0] = keyOf(container, type, parameter);

        Type[] genericParameterTypes = executable.getGenericParameterTypes();
        boolean hasImplicitDependency = genericParameterTypes.length != parameters.length;
        for (int i = 1; i < dependencies.length; i++) {
            type = genericParameterTypes[hasImplicitDependency ? i - 1 : i];
            parameter = parameters[i];
            dependencies[i] = keyOf(container, type, parameter);
        }
        return dependencies;
    }

    @SuppressWarnings("unchecked")
    public static <T> Binding<T> bindingFromMethod(Method method) {
        method.setAccessible(true);
        Binding<T> binding = Binding.to(
                args -> {
                    try {
                        Object instance;
                        Object[] params;
                        if (Modifier.isStatic(method.getModifiers())) {
                            instance = null;
                            params = args;
                        } else {
                            instance = args[0];
                            params = Arrays.copyOfRange(args, 1, args.length);
                        }
                        T result = (T) method.invoke(instance, params);
                        if (result == null) {
                            throw new NullPointerException(
                                    "@Provides method must return non-null result, method " + method);
                        }
                        return result;
                    } catch (IllegalAccessException e) {
                        throw new DIException("Not allowed to call method " + method, e);
                    } catch (InvocationTargetException e) {
                        throw new DIException("Failed to call method " + method, e.getCause());
                    }
                },
                toDependencies(method.getDeclaringClass(), method));

        Priority priority = method.getAnnotation(Priority.class);
        if (priority != null) {
            binding = binding.prioritize(priority.value());
        }

        return binding;
    }

    public static <T> Binding<T> bindingFromConstructor(Key<T> key, Constructor<T> constructor) {
        constructor.setAccessible(true);

        Key<?>[] dependencies = toDependencies(key.getType(), constructor);

        Binding<T> binding = Binding.to(
                args -> {
                    try {
                        return constructor.newInstance(args);
                    } catch (InstantiationException e) {
                        throw new DIException(
                                "Cannot instantiate object from the constructor " + constructor
                                        + " to provide requested key " + key,
                                e);
                    } catch (IllegalAccessException e) {
                        throw new DIException(
                                "Not allowed to call constructor " + constructor + " to provide requested key " + key,
                                e);
                    } catch (InvocationTargetException e) {
                        throw new DIException(
                                "Failed to call constructor " + constructor + " to provide requested key " + key,
                                e.getCause());
                    }
                },
                dependencies);

        Priority priority = constructor.getDeclaringClass().getAnnotation(Priority.class);
        if (priority != null) {
            binding = binding.prioritize(priority.value());
        }

        return binding.withKey(key);
    }
}
