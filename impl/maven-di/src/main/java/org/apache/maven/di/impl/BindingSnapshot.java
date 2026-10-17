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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

/** Detached diagnostic metadata; capturing it does not compile or provision bindings. */
final class BindingSnapshot {
    private BindingSnapshot() {}

    static List<String> capture(
            Map<Key<?>, Set<Binding<?>>> bindings, Map<Class<? extends Annotation>, Supplier<Scope>> scopes) {
        List<String> rows = new ArrayList<>();
        Map<Binding<?>, String> descriptions = new IdentityHashMap<>();
        bindings.forEach((key, candidates) -> {
            String name = key(key);
            if (candidates.isEmpty()) {
                rows.add("  " + name + " -> <no candidates>");
            } else {
                for (Binding<?> binding : candidates) {
                    rows.add("  " + name + " -> " + descriptions.computeIfAbsent(binding, BindingSnapshot::describe));
                }
            }
        });
        scopes.forEach((annotation, supplier) ->
                rows.add("  scope " + type(annotation) + " -> supplier " + Binding.identityString(supplier)));
        Collections.sort(rows);
        return List.copyOf(rows);
    }

    static String render(Object injector, long revision, String reason, boolean partial, List<String> rows) {
        return "Maven DI native registry " + Binding.identityString(injector) + " revision=" + revision + " ("
                + reason + (partial ? ", partial" : "") + ") {\n"
                + String.join("\n", rows) + "\n} Maven DI native registry " + Binding.identityString(injector);
    }

    private static String describe(Binding<?> binding) {
        String dependencies = binding.getDependencies().stream()
                .map(dependency -> (dependency.optional() ? "?" : "") + key(dependency.key()))
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
        return "original=" + (binding.getOriginalKey() == null ? "<none>" : key(binding.getOriginalKey()))
                + ", priority=" + binding.getPriority()
                + ", scope="
                + (binding.getScope() == null
                        ? "<none>"
                        : type(binding.getScope().annotationType()))
                + ", source=" + escape(binding.getDiagnosticSource())
                + ", dependencies=" + dependencies;
    }

    private static String key(Key<?> key) {
        Object qualifier = key.getQualifier();
        String prefix = qualifier instanceof String name
                ? "@Named(\"" + escape(name) + "\") "
                : qualifier instanceof Class<?> annotationType
                        ? "@" + type(annotationType) + " "
                        : qualifier instanceof Annotation annotation
                                ? "@" + type(annotation.annotationType()) + " [qualifier="
                                        + Binding.identityString(annotation) + "] "
                                : qualifier != null ? "@" + Binding.identityString(qualifier) + " " : "";
        return prefix + type(key.getType());
    }

    private static String type(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.isArray()
                    ? type(clazz.getComponentType()) + "[]"
                    : clazz.getName() + " [loader=" + Binding.identityString(clazz.getClassLoader()) + "]";
        }
        if (type instanceof ParameterizedType parameterized) {
            return (parameterized.getOwnerType() == null ? "" : type(parameterized.getOwnerType()) + ".")
                    + type(parameterized.getRawType()) + "<"
                    + Arrays.stream(parameterized.getActualTypeArguments())
                            .map(BindingSnapshot::type)
                            .collect(Collectors.joining(", "))
                    + ">";
        }
        if (type instanceof GenericArrayType array) {
            return type(array.getGenericComponentType()) + "[]";
        }
        if (type instanceof TypeVariable<?> variable) {
            return variable.getName();
        }
        if (type instanceof WildcardType wildcard) {
            return "? extends "
                    + Arrays.stream(wildcard.getUpperBounds())
                            .map(BindingSnapshot::type)
                            .collect(Collectors.joining(" & "))
                    + (wildcard.getLowerBounds().length == 0
                            ? ""
                            : " super "
                                    + Arrays.stream(wildcard.getLowerBounds())
                                            .map(BindingSnapshot::type)
                                            .collect(Collectors.joining(" & ")));
        }
        return Binding.identityString(type);
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
