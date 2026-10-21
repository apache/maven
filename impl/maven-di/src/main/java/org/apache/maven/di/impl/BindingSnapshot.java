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
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

/** Detached diagnostic metadata; capturing it does not compile or provision bindings. */
final class BindingSnapshot {
    private static final String NONE = "<none>";
    private final Map<ClassLoader, String> loaderIds = new IdentityHashMap<>();
    private final List<String> loaderRows = new ArrayList<>();

    private BindingSnapshot() {}

    static List<String> capture(
            Map<Key<?>, Set<Binding<?>>> bindings, Map<Class<? extends Annotation>, Supplier<Scope>> scopes) {
        BindingSnapshot format = new BindingSnapshot();
        List<String> blocks = new ArrayList<>();
        Map<Binding<?>, String> descriptions = new IdentityHashMap<>();
        Comparator<Binding<?>> candidateOrder = Binding.getPriorityComparator().thenComparing(Binding::identityString);
        var sortedRegistrations = bindings.entrySet().stream()
                .map(entry -> Map.entry(key(entry.getKey(), Binding::identityString), entry))
                .sorted(Map.Entry.comparingByKey())
                .toList();
        for (var sortEntry : sortedRegistrations) {
            var registration = sortEntry.getValue();
            String name = format.key(registration.getKey());
            String candidates = registration.getValue().isEmpty()
                    ? "    <no candidates>"
                    : registration.getValue().stream()
                            .sorted(candidateOrder)
                            .map(binding -> descriptions.computeIfAbsent(binding, format::describe))
                            .collect(Collectors.joining("\n"));
            blocks.add("  key: " + name + "\n" + candidates);
        }
        if (!scopes.isEmpty()) {
            String scopeRows = scopes.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> type(entry.getKey(), Binding::identityString)))
                    .map(entry -> "    " + format.type(entry.getKey()) + "\n      supplier: "
                            + Binding.identityString(entry.getValue()))
                    .collect(Collectors.joining("\n"));
            blocks.add("  scopes:\n" + scopeRows);
        }
        if (!format.loaderRows.isEmpty()) {
            blocks.add("  class loaders:\n" + String.join("\n", format.loaderRows));
        }
        return List.copyOf(blocks);
    }

    static String render(Object injector, long revision, String reason, boolean partial, List<String> blocks) {
        String registry = "Maven DI native registry " + Binding.identityString(injector);
        String header = registry + " revision=" + revision + " (" + reason + (partial ? ", partial" : "") + ") {";
        return String.join("\n", header, String.join("\n\n", blocks), "} " + registry);
    }

    private String describe(Binding<?> binding) {
        // Assign loader IDs in full-key order before sorting the displayed dependencies.
        List<String> dependencies = binding.getDependencies().stream()
                .sorted(Comparator.comparing(dependency -> key(dependency.key(), Binding::identityString)))
                .map(dependency -> (dependency.optional() ? "?" : "") + key(dependency.key()))
                .sorted()
                .toList();
        List<String> source = binding.getDiagnosticSourceLines(this::sourceLines);
        StringJoiner lines = new StringJoiner("\n");
        lines.add("    candidate: " + (binding.getOriginalKey() == null ? NONE : key(binding.getOriginalKey())));
        lines.add("      priority: " + binding.getPriority());
        lines.add("      scope: "
                + (binding.getScope() == null ? NONE : type(binding.getScope().annotationType())));
        if (source.size() == 1) {
            lines.add("      source: " + escape(source.get(0)));
        } else {
            lines.add("      source:");
            source.forEach(line -> lines.add("        " + escape(line)));
        }
        if (dependencies.isEmpty()) {
            lines.add("      dependencies: " + NONE);
        } else {
            lines.add("      dependencies:");
            dependencies.forEach(dependency -> lines.add("        - " + dependency));
        }
        return lines.toString();
    }

    private List<String> sourceLines(Member member) {
        String loaderReference = " [loader=" + loader(member.getDeclaringClass().getClassLoader()) + "]";
        if (!(member instanceof Executable executable)) {
            return List.of(member + loaderReference);
        }
        List<String> lines = new ArrayList<>();
        int mask = member instanceof Method ? Modifier.methodModifiers() : Modifier.constructorModifiers();
        String modifiers = Modifier.toString(member.getModifiers() & mask);
        String prefix = modifiers.isEmpty() ? "" : modifiers + " ";
        String name = member.getDeclaringClass().getName();
        if (member instanceof Method method) {
            lines.add(prefix + (method.isDefault() ? "default " : "") + type(method.getReturnType()));
            prefix = "";
            name += "." + member.getName();
        }
        Class<?>[] parameters = executable.getParameterTypes();
        if (parameters.length == 0) {
            lines.add(prefix + name + "()" + loaderReference);
        } else {
            lines.add(prefix + name + "(");
            for (int i = 0; i < parameters.length; i++) {
                lines.add("  " + type(parameters[i]) + (i + 1 < parameters.length ? "," : ""));
            }
            lines.add(")" + loaderReference);
        }
        for (Class<?> exception : executable.getExceptionTypes()) {
            lines.add("throws " + type(exception));
        }
        return List.copyOf(lines);
    }

    private String loader(ClassLoader loader) {
        if (loader == null) {
            return "bootstrap";
        }
        return loaderIds.computeIfAbsent(loader, value -> {
            String id = "L" + (loaderIds.size() + 1);
            loaderRows.add("    " + id + ": " + Binding.identityString(value));
            return id;
        });
    }

    private String key(Key<?> key) {
        return key(key, this::loader);
    }

    private String type(Type type) {
        return type(type, this::loader);
    }

    private static String key(Key<?> key, Function<ClassLoader, String> loader) {
        Object qualifier = key.getQualifier();
        String prefix;
        if (qualifier instanceof String name) {
            prefix = "@Named(\"" + escape(name) + "\") ";
        } else if (qualifier instanceof Class<?> annotationType) {
            prefix = "@" + type(annotationType, loader) + " ";
        } else if (qualifier instanceof Annotation annotation) {
            prefix = "@" + type(annotation.annotationType(), loader) + " [qualifier="
                    + Binding.identityString(annotation) + "] ";
        } else if (qualifier != null) {
            prefix = "@" + Binding.identityString(qualifier) + " ";
        } else {
            prefix = "";
        }
        return prefix + type(key.getType(), loader);
    }

    private static String type(Type type, Function<ClassLoader, String> loader) {
        if (type instanceof Class<?> clazz) {
            return clazz.isArray()
                    ? type(clazz.getComponentType(), loader) + "[]"
                    : classType(clazz, clazz.getName(), loader);
        }
        if (type instanceof ParameterizedType parameterized) {
            Type owner = parameterized.getOwnerType();
            Type raw = parameterized.getRawType();
            String name = owner != null && raw instanceof Class<?> clazz
                    ? classType(clazz, clazz.getSimpleName(), loader)
                    : type(raw, loader);
            return (owner == null ? "" : type(owner, loader) + ".") + name + "<"
                    + Arrays.stream(parameterized.getActualTypeArguments())
                            .map(argument -> type(argument, loader))
                            .collect(Collectors.joining(", "))
                    + ">";
        }
        if (type instanceof GenericArrayType array) {
            return type(array.getGenericComponentType(), loader) + "[]";
        }
        if (type instanceof TypeVariable<?> variable) {
            return variable.getName();
        }
        if (type instanceof WildcardType wildcard) {
            Type[] lowerBounds = wildcard.getLowerBounds();
            if (lowerBounds.length > 0) {
                return "? super "
                        + Arrays.stream(lowerBounds)
                                .map(argument -> type(argument, loader))
                                .collect(Collectors.joining(" & "));
            }
            Type[] upperBounds = wildcard.getUpperBounds();
            if (upperBounds.length == 1 && upperBounds[0] == Object.class) {
                return "?";
            }
            return "? extends "
                    + Arrays.stream(upperBounds)
                            .map(argument -> type(argument, loader))
                            .collect(Collectors.joining(" & "));
        }
        return Binding.identityString(type);
    }

    private static String classType(Class<?> clazz, String name, Function<ClassLoader, String> loader) {
        return name + " [loader=" + loader.apply(clazz.getClassLoader()) + "]";
    }

    private static String escape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') {
                result.append('\\').append(c);
            } else if (Character.isISOControl(c)) {
                result.append(String.format("\\u%04x", (int) c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
