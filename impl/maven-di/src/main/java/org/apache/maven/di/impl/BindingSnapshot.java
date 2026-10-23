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
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final String NO_CANDIDATES = "<no candidates>";
    private static final String ADDED = "+ ";
    private static final String REMOVED = "- ";
    private static final String CHANGED = "~ ";
    private final long revision;
    private final List<KeyBindings> keys;
    private final List<ScopeBinding> scopes;
    private final Map<String, String> loaders;

    private BindingSnapshot(
            long revision, List<KeyBindings> keys, List<ScopeBinding> scopes, Map<String, String> loaders) {
        this.revision = revision;
        this.keys = List.copyOf(keys);
        this.scopes = List.copyOf(scopes);
        this.loaders = Collections.unmodifiableMap(new LinkedHashMap<>(loaders));
    }

    static BindingSnapshot capture(
            Map<Key<?>, Set<Binding<?>>> bindings,
            Map<Class<? extends Annotation>, Supplier<Scope>> scopes,
            DiagnosticIdentity identities,
            long revision) {
        Capture format = new Capture(identities);
        List<KeyBindings> keys = new ArrayList<>();
        Map<Binding<?>, Candidate> descriptions = new IdentityHashMap<>();
        Comparator<Binding<?>> candidateOrder = Binding.getPriorityComparator().thenComparing(Binding::identityString);
        var registrations = bindings.entrySet().stream()
                .map(entry -> Map.entry(
                        Capture.key(entry.getKey(), Binding::identityString).plain(), entry))
                .sorted(Map.Entry.comparingByKey())
                .toList();
        for (var sortEntry : registrations) {
            var registration = sortEntry.getValue();
            KeyText name = format.key(registration.getKey());
            List<Candidate> candidates = registration.getValue().stream()
                    .sorted(candidateOrder)
                    .map(binding -> descriptions.computeIfAbsent(binding, format::describe))
                    .toList();
            keys.add(new KeyBindings(format.keyIdentity(registration.getKey()), name, candidates));
        }
        List<ScopeBinding> scopeRows = scopes.entrySet().stream()
                .sorted(Comparator.comparing(entry -> Capture.type(entry.getKey(), Binding::identityString)))
                .map(entry -> new ScopeBinding(
                        identities.token(entry.getKey()),
                        format.type(entry.getKey()),
                        identities.token(entry.getValue()),
                        Binding.identityString(entry.getValue())))
                .toList();
        return new BindingSnapshot(revision, keys, scopeRows, identities.loaders());
    }

    String render(Object injector, String reason, boolean partial, BindingSnapshot previous) {
        List<String> blocks = new ArrayList<>();
        if (previous == null) {
            keys.forEach(key -> blocks.add(renderKey(key, "")));
            if (!scopes.isEmpty()) {
                blocks.add(renderScopes(scopes, ""));
            }
        } else {
            Map<List<?>, KeyBindings> oldKeys = new LinkedHashMap<>();
            previous.keys.forEach(key -> oldKeys.put(key.identity, key));
            for (KeyBindings key : keys) {
                KeyBindings old = oldKeys.remove(key.identity);
                String changes = old == null ? renderKey(key, ADDED) : renderChanges(old, key);
                if (!changes.isEmpty()) {
                    blocks.add(changes);
                }
            }
            oldKeys.values().forEach(key -> blocks.add(renderKey(key, REMOVED)));
            Map<Long, ScopeBinding> oldScopes = new LinkedHashMap<>();
            previous.scopes.forEach(scope -> oldScopes.put(scope.identity, scope));
            List<ScopeBinding> removed = new ArrayList<>();
            List<ScopeBinding> added = new ArrayList<>();
            for (ScopeBinding scope : scopes) {
                ScopeBinding old = oldScopes.remove(scope.identity);
                if (!scope.equals(old)) {
                    if (old != null) {
                        removed.add(old);
                    }
                    added.add(scope);
                }
            }
            removed.addAll(oldScopes.values());
            if (!removed.isEmpty()) {
                blocks.add(renderScopes(removed, REMOVED));
            }
            if (!added.isEmpty()) {
                blocks.add(renderScopes(added, ADDED));
            }
            if (blocks.isEmpty() && !partial) {
                return null;
            }
        }
        if (!loaders.isEmpty()) {
            blocks.add("  " + "class loaders:" + "\n"
                    + loaders.entrySet().stream()
                            .map(entry -> "    " + entry.getKey() + ": " + entry.getValue())
                            .collect(Collectors.joining("\n")));
        }
        String registry = "Maven DI native registry " + Binding.identityString(injector);
        String revisions = previous == null ? Long.toString(revision) : previous.revision + "->" + revision;
        String header = registry + " revision=" + revisions + " (" + reason
                + (previous == null ? "" : ", delta")
                + (partial ? ", partial" : "") + ") {";
        return String.join("\n", header, String.join("\n\n", blocks), "} " + registry);
    }

    private static String renderChanges(KeyBindings before, KeyBindings after) {
        Map<Long, Candidate> oldCandidates = new LinkedHashMap<>();
        before.candidates.forEach(candidate -> oldCandidates.put(candidate.identity, candidate));
        List<String> changes = new ArrayList<>();
        for (Candidate candidate : after.candidates) {
            Candidate old = oldCandidates.remove(candidate.identity);
            if (!candidate.equals(old)) {
                changes.add(renderCandidate(candidate, old == null ? ADDED : CHANGED));
            }
        }
        oldCandidates.values().forEach(candidate -> changes.add(renderCandidate(candidate, REMOVED)));
        if (changes.isEmpty()) {
            return "";
        }
        if (after.candidates.isEmpty()) {
            changes.add("    " + NO_CANDIDATES);
        }
        return "  " + (CHANGED + "key:") + " " + after.name.plain() + "\n" + String.join("\n\n", changes);
    }

    private static String renderKey(KeyBindings key, String marker) {
        String candidates = key.candidates.isEmpty()
                ? "    " + (marker + NO_CANDIDATES)
                : key.candidates.stream()
                        .map(candidate -> renderCandidate(candidate, marker))
                        .collect(Collectors.joining("\n\n"));
        return "  " + (marker + "key:") + " " + key.name.plain() + "\n" + candidates;
    }

    private static String renderScopes(List<ScopeBinding> scopes, String marker) {
        return "  " + (marker + "scopes:") + "\n"
                + scopes.stream()
                        .map(scope -> "    " + scope.type + "\n      supplier: " + scope.supplier)
                        .collect(Collectors.joining("\n"));
    }

    private static String renderCandidate(Candidate candidate, String marker) {
        StringJoiner lines = new StringJoiner("\n");
        lines.add("    " + (marker + "candidate:") + " "
                + (candidate.original == null ? NONE : candidate.original.plain()));
        lines.add("      priority: " + candidate.priority);
        lines.add("      scope: " + (candidate.scope == null ? NONE : candidate.scope));
        if (candidate.source.size() == 1) {
            lines.add("      source: " + escape(candidate.source.get(0)));
        } else if (!candidate.source.isEmpty()) {
            lines.add("      source:");
            candidate.source.forEach(line -> lines.add("        " + escape(line)));
        }
        if (candidate.dependencies.isEmpty()) {
            lines.add("      dependencies: " + NONE);
        } else {
            lines.add("      dependencies:");
            candidate.dependencies.forEach(
                    dependency -> lines.add("        - " + (dependency.optional ? "?" : "") + dependency.key.plain()));
        }
        return lines.toString();
    }

    private record KeyText(String qualifier, String type) {
        String plain() {
            return (qualifier.isEmpty() ? "" : qualifier + " ") + type;
        }
    }

    private record KeyBindings(List<?> identity, KeyText name, List<Candidate> candidates) {}

    private record Candidate(
            long identity,
            List<?> originalIdentity,
            KeyText original,
            int priority,
            String scope,
            List<String> source,
            Set<DependencyText> dependencies) {}

    private record DependencyText(List<?> identity, KeyText key, boolean optional) {
        String plain() {
            return (optional ? "?" : "") + key.plain();
        }
    }

    private record ScopeBinding(long identity, String type, long supplierIdentity, String supplier) {}

    /** Snapshots retain detached metadata, not registry objects. */
    private static final class Capture {
        private final DiagnosticIdentity identities;

        private Capture(DiagnosticIdentity identities) {
            this.identities = identities;
        }

        private Candidate describe(Binding<?> binding) {
            // Assign loader IDs in full-key order before sorting the displayed dependencies.
            List<DependencyText> dependencies = binding.getDependencies().stream()
                    .sorted(Comparator.comparing(dependency ->
                            key(dependency.key(), Binding::identityString).plain()))
                    .map(dependency -> new DependencyText(
                            keyIdentity(dependency.key()), key(dependency.key()), dependency.optional()))
                    .sorted(Comparator.comparing(DependencyText::plain))
                    .toList();
            List<String> source = binding.getDiagnosticSourceLines(this::sourceLines);
            return new Candidate(
                    identities.token(binding),
                    binding.getOriginalKey() == null ? List.of() : keyIdentity(binding.getOriginalKey()),
                    binding.getOriginalKey() == null ? null : key(binding.getOriginalKey()),
                    binding.getPriority(),
                    binding.getScope() == null ? null : type(binding.getScope().annotationType()),
                    List.copyOf(source),
                    Collections.unmodifiableSet(new LinkedHashSet<>(dependencies)));
        }

        private List<String> sourceLines(Member member) {
            String loaderReference =
                    " [loader=" + loader(member.getDeclaringClass().getClassLoader()) + "]";
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
            return identities.loader(loader);
        }

        private List<?> keyIdentity(Key<?> key) {
            Object qualifier = key.getQualifier();
            Object qualifierIdentity = qualifier == null
                    ? List.of()
                    : qualifier instanceof String name
                            ? List.of("name", name)
                            : List.of("object", identities.token(qualifier));
            return List.of(qualifierIdentity, typeIdentity(key.getType()));
        }

        private Object typeIdentity(Type type) {
            if (type instanceof Class<?> clazz) {
                return List.of("class", identities.token(clazz));
            }
            if (type instanceof ParameterizedType parameterized) {
                return List.of(
                        "parameterized",
                        parameterized.getOwnerType() == null ? List.of() : typeIdentity(parameterized.getOwnerType()),
                        typeIdentity(parameterized.getRawType()),
                        typeIdentities(parameterized.getActualTypeArguments()));
            }
            if (type instanceof GenericArrayType array) {
                return List.of("array", typeIdentity(array.getGenericComponentType()));
            }
            if (type instanceof TypeVariable<?> variable) {
                return List.of("variable", declarationIdentity(variable.getGenericDeclaration()), variable.getName());
            }
            if (type instanceof WildcardType wildcard) {
                return List.of(
                        "wildcard",
                        typeIdentities(wildcard.getLowerBounds()),
                        typeIdentities(wildcard.getUpperBounds()));
            }
            return List.of("opaque", identities.token(type));
        }

        private List<?> typeIdentities(Type[] types) {
            return Arrays.stream(types).map(this::typeIdentity).toList();
        }

        private Object declarationIdentity(GenericDeclaration declaration) {
            if (declaration instanceof Class<?> clazz) {
                return typeIdentity(clazz);
            }
            if (declaration instanceof Executable executable) {
                return List.of(
                        executable instanceof Method ? "method" : "constructor",
                        identities.token(executable.getDeclaringClass()),
                        executable.getName(),
                        typeIdentities(executable.getParameterTypes()),
                        executable instanceof Method method ? typeIdentity(method.getReturnType()) : List.of());
            }
            return List.of("declaration", identities.token(declaration));
        }

        private KeyText key(Key<?> key) {
            return key(key, this::loader);
        }

        private String type(Type type) {
            return type(type, this::loader);
        }

        private static KeyText key(Key<?> key, Function<ClassLoader, String> loader) {
            Object qualifier = key.getQualifier();
            String prefix;
            if (qualifier instanceof String name) {
                prefix = "@Named(\"" + escape(name) + "\")";
            } else if (qualifier instanceof Class<?> annotationType) {
                prefix = "@" + type(annotationType, loader);
            } else if (qualifier instanceof Annotation annotation) {
                prefix = "@" + type(annotation.annotationType(), loader) + " [qualifier="
                        + Binding.identityString(annotation) + "]";
            } else if (qualifier != null) {
                prefix = "@" + Binding.identityString(qualifier);
            } else {
                prefix = "";
            }
            return new KeyText(prefix, type(key.getType(), loader));
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
