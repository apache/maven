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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.maven.api.annotations.Nullable;

import static java.util.stream.Collectors.joining;

/**
 * Various helper methods for type processing
 */
public class Types {
    public static final Type[] NO_TYPES = new Type[0];
    public static final WildcardType WILDCARD_TYPE_ANY = new WildcardTypeImpl(new Type[] {Object.class}, new Type[0]);
    private static final Map<Type, Map<TypeVariable<?>, Type>> TYPE_BINDINGS_CACHE = new ConcurrentHashMap<>();

    /**
     * Returns a raw {@link Class} for a given {@link Type}.
     * <p>
     * A type can be any of {@link Class}, {@link ParameterizedType}, {@link WildcardType},
     * {@link GenericArrayType} or {@link TypeVariable}
     */
    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        } else if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        } else if (type instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            return getRawType(getUppermostType(upperBounds));
        } else if (type instanceof GenericArrayType genericArrayType) {
            Class<?> rawComponentType = getRawType(genericArrayType.getGenericComponentType());
            try {
                return Class.forName("[L" + rawComponentType.getName() + ";");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (type instanceof TypeVariable<?> typeVariable) {
            return getRawType(getUppermostType(typeVariable.getBounds()));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /**
     * Returns the most common type among given types
     */
    public static Type getUppermostType(Type[] types) {
        Type result = types[0];
        for (int i = 1; i < types.length; i++) {
            Type type = types[i];
            if (isAssignable(type, result)) {
                result = type;
                continue;
            } else if (isAssignable(result, type)) {
                continue;
            }
            throw new IllegalArgumentException("Unrelated types: " + result + " , " + type);
        }
        return result;
    }

    /**
     * Returns an array of actual type arguments for a given {@link Type}
     *
     * @param type type whose actual type arguments should be retrieved
     * @return an array of actual type arguments for a given {@link Type}
     */
    public static Type[] getActualTypeArguments(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.isArray() ? new Type[] {clazz.getComponentType()} : NO_TYPES;
        } else if (type instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments();
        } else if (type instanceof GenericArrayType genericArrayType) {
            return new Type[] {genericArrayType.getGenericComponentType()};
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * Returns a map of type bindings for a given {@link Type}
     */
    public static Map<TypeVariable<?>, Type> getTypeBindings(Type type) {
        Type[] typeArguments = getActualTypeArguments(type);
        if (typeArguments.length == 0) {
            return Collections.emptyMap();
        }
        TypeVariable<?>[] typeVariables = getRawType(type).getTypeParameters();
        Map<TypeVariable<?>, Type> map = new HashMap<>();
        for (int i = 0; i < typeVariables.length; i++) {
            map.put(typeVariables[i], typeArguments[i]);
        }
        return map;
    }

    /**
     * Returns a map of all type bindings for a given {@link Type}.
     * Includes type bindings from a whole class hierarchy
     */
    public static Map<TypeVariable<?>, Type> getAllTypeBindings(Type type) {
        return TYPE_BINDINGS_CACHE.computeIfAbsent(type, t -> {
            Map<TypeVariable<?>, Type> mapping = new HashMap<>();
            getAllTypeBindingsImpl(t, mapping);
            return mapping;
        });
    }

    private static void getAllTypeBindingsImpl(Type type, Map<TypeVariable<?>, Type> mapping) {
        Class<?> cls = getRawType(type);

        if (type instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length != 0) {
                TypeVariable<? extends Class<?>>[] typeVariables = cls.getTypeParameters();
                for (int i = 0; i < typeArguments.length; i++) {
                    Type typeArgument = typeArguments[i];
                    mapping.put(
                            typeVariables[i],
                            typeArgument instanceof TypeVariable
                                    ? Objects.requireNonNull(mapping.get(typeArgument))
                                    : typeArgument);
                }
            }
        }

        Type superclass = cls.getGenericSuperclass();
        if (superclass != null) {
            getAllTypeBindingsImpl(superclass, mapping);
        }

        for (Type anInterface : cls.getGenericInterfaces()) {
            getAllTypeBindingsImpl(anInterface, mapping);
        }
    }

    /**
     * Binds a given type with actual type arguments
     *
     * @param type     a type to be bound
     * @param bindings a map of actual types
     */
    public static Type bind(Type type, Map<TypeVariable<?>, Type> bindings) {
        return bind(type, bindings::get);
    }

    /**
     * Binds a given type with actual type arguments
     *
     * @param type     a type to be bound
     * @param bindings a lookup function for actual types
     */
    public static Type bind(Type type, Function<TypeVariable<?>, Type> bindings) {
        if (type instanceof Class) {
            return type;
        }
        if (type instanceof TypeVariable<?> typeVariable) {
            Type actualType = bindings.apply(typeVariable);
            if (actualType == null) {
                throw new TypeNotBoundException("Type variable not found: " + typeVariable + " ( "
                        + typeVariable.getGenericDeclaration() + " ) ");
            }
            return actualType;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Type[] typeArguments2 = new Type[typeArguments.length];
            for (int i = 0; i < typeArguments.length; i++) {
                typeArguments2[i] = bind(typeArguments[i], bindings);
            }
            return new ParameterizedTypeImpl(
                    parameterizedType.getOwnerType(), parameterizedType.getRawType(), typeArguments2);
        }
        if (type instanceof GenericArrayType genericArrayType) {
            Type componentType = genericArrayType.getGenericComponentType();
            return new GenericArrayTypeImpl(bind(componentType, bindings));
        }
        if (type instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            Type[] upperBounds2 = new Type[upperBounds.length];
            for (int i = 0; i < upperBounds.length; i++) {
                upperBounds2[i] = bind(upperBounds[i], bindings);
            }
            Type[] lowerBounds = wildcardType.getLowerBounds();
            Type[] lowerBounds2 = new Type[lowerBounds.length];
            for (int i = 0; i < lowerBounds.length; i++) {
                lowerBounds2[i] = bind(lowerBounds[i], bindings);
            }
            return new WildcardTypeImpl(upperBounds2, lowerBounds2);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * Creates an instance of {@link ParameterizedType}
     *
     * @param ownerType  an owner type
     * @param rawType    a type to be parameterized
     * @param parameters parameter types
     * @return an instance of {@link ParameterizedType}
     */
    public static ParameterizedType parameterizedType(@Nullable Type ownerType, Type rawType, Type[] parameters) {
        return new ParameterizedTypeImpl(ownerType, rawType, parameters);
    }

    /**
     * Creates an instance of {@link ParameterizedType}
     *
     * @see #parameterizedType(Type, Type, Type[])
     */
    public static ParameterizedType parameterizedType(Class<?> rawType, Type... parameters) {
        return new ParameterizedTypeImpl(null, rawType, parameters);
    }

    /**
     * Get all super classes and interface implemented by the given type.
     */
    public static Set<Type> getAllSuperTypes(Type original) {
        Deque<Type> todo = new ArrayDeque<>();
        todo.add(original);
        Set<Type> done = new HashSet<>();
        while (!todo.isEmpty()) {
            Type type = todo.remove();
            if (done.add(type)) {
                Class<?> cls = getRawType(type);
                Function<TypeVariable<?>, Type> bindings;
                if (type instanceof ParameterizedType parameterizedType) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    TypeVariable<? extends Class<?>>[] typeVariables = cls.getTypeParameters();
                    bindings = v -> {
                        for (int i = 0; i < typeArguments.length; i++) {
                            Type typeArgument = typeArguments[i];
                            if (v.equals(typeVariables[i])) {
                                return typeArgument;
                            }
                        }
                        return null;
                    };
                } else {
                    bindings = v -> null;
                }
                Type[] interfaces = cls.getGenericInterfaces();
                for (Type itf : interfaces) {
                    try {
                        todo.add(bind(itf, bindings));
                    } catch (TypeNotBoundException e) {
                        // ignore
                    }
                }
                Type supercls = cls.getGenericSuperclass();
                if (supercls != null) {
                    try {
                        todo.add(bind(supercls, bindings));
                    } catch (TypeNotBoundException e) {
                        // ignore
                    }
                }
            }
        }
        return done;
    }

    public static Type simplifyType(Type original) {
        if (original instanceof Class) {
            return original;
        }

        if (original instanceof GenericArrayType genericArrayType) {
            Type componentType = genericArrayType.getGenericComponentType();
            Type repackedComponentType = simplifyType(componentType);
            if (componentType != repackedComponentType) {
                return genericArrayType(repackedComponentType);
            }
            return original;
        }

        if (original instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Type[] repackedTypeArguments = simplifyTypes(typeArguments);

            if (isAllObjects(repackedTypeArguments)) {
                return parameterizedType.getRawType();
            }

            if (typeArguments != repackedTypeArguments) {
                return parameterizedType(
                        parameterizedType.getOwnerType(), parameterizedType.getRawType(), repackedTypeArguments);
            }
            return original;
        }

        if (original instanceof TypeVariable) {
            throw new IllegalArgumentException("Key should not contain a type variable: " + original);
        }

        if (original instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length == 1) {
                Type upperBound = upperBounds[0];
                if (upperBound != Object.class) {
                    return simplifyType(upperBound);
                }
            } else if (upperBounds.length > 1) {
                throw new IllegalArgumentException("Multiple upper bounds not supported: " + original);
            }

            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds.length == 1) {
                return simplifyType(lowerBounds[0]);
            } else if (lowerBounds.length > 1) {
                throw new IllegalArgumentException("Multiple lower bounds not supported: " + original);
            }
            return Object.class;
        }

        return original;
    }

    private static Type[] simplifyTypes(Type[] original) {
        int length = original.length;
        for (int i = 0; i < length; i++) {
            Type typeArgument = original[i];
            Type repackTypeArgument = simplifyType(typeArgument);
            if (repackTypeArgument != typeArgument) {
                Type[] repackedTypeArguments = new Type[length];
                System.arraycopy(original, 0, repackedTypeArguments, 0, i);
                repackedTypeArguments[i++] = repackTypeArgument;
                for (; i < length; i++) {
                    repackedTypeArguments[i] = simplifyType(original[i]);
                }
                return repackedTypeArguments;
            }
        }
        return original;
    }

    private static boolean isAllObjects(Type[] types) {
        for (Type type : types) {
            if (type != Object.class) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether a {@code from} type is assignable to {@code to} type
     *
     * @param to   a 'to' type that should be checked for possible assignment
     * @param from a 'from' type that should be checked for possible assignment
     * @return whether an object of type {@code from} is assignable to an object of type {@code to}
     */
    public static boolean isAssignable(Type to, Type from) {
        // shortcut
        if (to instanceof Class<?> toClazz && from instanceof Class<?> fromClazz) {
            return toClazz.isAssignableFrom(fromClazz);
        }
        return isAssignable(to, from, false);
    }

    private static boolean isAssignable(Type to, Type from, boolean strict) {
        if (to instanceof WildcardType || from instanceof WildcardType) {
            Type[] toUppers, toLowers;
            if (to instanceof WildcardType wildcardTo) {
                toUppers = wildcardTo.getUpperBounds();
                toLowers = wildcardTo.getLowerBounds();
            } else {
                toUppers = new Type[] {to};
                toLowers = strict ? toUppers : NO_TYPES;
            }

            Type[] fromUppers, fromLowers;
            if (from instanceof WildcardType wildcardFrom) {
                fromUppers = wildcardFrom.getUpperBounds();
                fromLowers = wildcardFrom.getLowerBounds();
            } else {
                fromUppers = new Type[] {from};
                fromLowers = strict ? fromUppers : NO_TYPES;
            }

            for (Type toUpper : toUppers) {
                for (Type fromUpper : fromUppers) {
                    if (!isAssignable(toUpper, fromUpper, false)) {
                        return false;
                    }
                }
            }
            if (toLowers.length == 0) {
                return true;
            }
            if (fromLowers.length == 0) {
                return false;
            }
            for (Type toLower : toLowers) {
                for (Type fromLower : fromLowers) {
                    if (!isAssignable(fromLower, toLower, false)) {
                        return false;
                    }
                }
            }
            return true;
        }
        if (to instanceof GenericArrayType) {
            to = getRawType(to);
        }
        if (from instanceof GenericArrayType) {
            from = getRawType(from);
        }
        if (!strict && to instanceof Class<?> toClazz) {
            return toClazz.isAssignableFrom(getRawType(from));
        }
        Class<?> toRawClazz = getRawType(to);
        Type[] toTypeArguments = getActualTypeArguments(to);
        return isAssignable(toRawClazz, toTypeArguments, from, strict);
    }

    private static boolean isAssignable(Class<?> toRawClazz, Type[] toTypeArguments, Type from, boolean strict) {
        Class<?> fromRawClazz = getRawType(from);
        if (strict && !toRawClazz.equals(fromRawClazz)) {
            return false;
        }
        if (!strict && !toRawClazz.isAssignableFrom(fromRawClazz)) {
            return false;
        }
        if (toRawClazz.isArray()) {
            return true;
        }
        Type[] fromTypeArguments = getActualTypeArguments(from);
        if (toRawClazz == fromRawClazz) {
            if (toTypeArguments.length > fromTypeArguments.length) {
                return false;
            }
            for (int i = 0; i < toTypeArguments.length; i++) {
                if (!isAssignable(toTypeArguments[i], fromTypeArguments[i], true)) {
                    return false;
                }
            }
            return true;
        }
        Map<TypeVariable<?>, Type> typeBindings = getTypeBindings(from);
        for (Type anInterface : fromRawClazz.getGenericInterfaces()) {
            if (isAssignable(
                    toRawClazz,
                    toTypeArguments,
                    bind(anInterface, key -> typeBindings.getOrDefault(key, wildcardTypeAny())),
                    false)) {
                return true;
            }
        }
        Type superclass = fromRawClazz.getGenericSuperclass();
        return superclass != null && isAssignable(toRawClazz, toTypeArguments, bind(superclass, typeBindings), false);
    }

    public static final class ParameterizedTypeImpl implements ParameterizedType {
        private final @Nullable Type ownerType;
        private final Type rawType;
        private final Type[] actualTypeArguments;

        ParameterizedTypeImpl(@Nullable Type ownerType, Type rawType, Type[] actualTypeArguments) {
            this.ownerType = ownerType;
            this.rawType = rawType;
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public @Nullable Type getOwnerType() {
            return ownerType;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(ownerType) ^ Arrays.hashCode(actualTypeArguments) ^ rawType.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ParameterizedType that)) {
                return false;
            }
            return this.getRawType().equals(that.getRawType())
                    && Objects.equals(this.getOwnerType(), that.getOwnerType())
                    && Arrays.equals(this.getActualTypeArguments(), that.getActualTypeArguments());
        }

        @Override
        public String toString() {
            return rawType.getTypeName()
                    + Arrays.stream(actualTypeArguments).map(Types::toString).collect(joining(", ", "<", ">"));
        }
    }

    /**
     * Creates an instance of {@link WildcardType} bound by upper and lower bounds
     *
     * @param upperBounds a wildcard upper bound types
     * @param lowerBounds a wildcard lower bound types
     * @return an instance of {@link WildcardType}
     */
    public static WildcardType wildcardType(Type[] upperBounds, Type[] lowerBounds) {
        return new WildcardTypeImpl(upperBounds, lowerBounds);
    }

    /**
     * Returns an instance of {@link WildcardType} that matches any type
     * <p>
     * E.g. {@code <?>}
     *
     * @see #wildcardType(Type[], Type[])
     */
    public static WildcardType wildcardTypeAny() {
        return WILDCARD_TYPE_ANY;
    }

    /**
     * Creates an instance of {@link WildcardType} bound by a single upper bound
     * <p>
     * E.g. {@code <? extends UpperBound>}
     *
     * @param upperBound a wildcard upper bound type
     * @return an instance of {@link WildcardType}
     * @see #wildcardType(Type[], Type[])
     */
    public static WildcardType wildcardTypeExtends(Type upperBound) {
        return new WildcardTypeImpl(new Type[] {upperBound}, NO_TYPES);
    }

    /**
     * Creates an instance of {@link WildcardType} bound by a single lower bound
     * <p>
     * E.g. {@code <? super LowerBound>}
     *
     * @param lowerBound a wildcard lower bound type
     * @return an instance of {@link WildcardType}
     * @see #wildcardType(Type[], Type[])
     */
    public static WildcardType wildcardTypeSuper(Type lowerBound) {
        return new WildcardTypeImpl(NO_TYPES, new Type[] {lowerBound});
    }

    public static class WildcardTypeImpl implements WildcardType {
        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds;
            this.lowerBounds = lowerBounds;
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds;
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof WildcardType that)) {
                return false;
            }
            return Arrays.equals(this.getUpperBounds(), that.getUpperBounds())
                    && Arrays.equals(this.getLowerBounds(), that.getLowerBounds());
        }

        @Override
        public String toString() {
            return "?"
                    + (upperBounds.length == 0
                            ? ""
                            : " extends "
                                    + Arrays.stream(upperBounds)
                                            .map(Types::toString)
                                            .collect(joining(" & ")))
                    + (lowerBounds.length == 0
                            ? ""
                            : " super "
                                    + Arrays.stream(lowerBounds)
                                            .map(Types::toString)
                                            .collect(joining(" & ")));
        }
    }

    /**
     * Creates an instance of {@link GenericArrayType} with a given component type
     * <p>
     * Same as {@code T[]}
     *
     * @param componentType a component type of generic array
     * @return an instance of {@link GenericArrayType}
     * @see #wildcardType(Type[], Type[])
     */
    public static GenericArrayType genericArrayType(Type componentType) {
        return new GenericArrayTypeImpl(componentType);
    }

    public static final class GenericArrayTypeImpl implements GenericArrayType {
        private final Type componentType;

        GenericArrayTypeImpl(Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public int hashCode() {
            return componentType.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericArrayType that)) {
                return false;
            }
            return this.getGenericComponentType().equals(that.getGenericComponentType());
        }

        @Override
        public String toString() {
            return Types.toString(componentType) + "[]";
        }
    }

    private static String toString(Type type) {
        return type instanceof Class<?> clazz ? clazz.getName() : type.toString();
    }

    /**
     * Returns a simple name for a given {@link Type}
     *
     * @see Class#getSimpleName()
     */
    public static String getSimpleName(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        } else if (type instanceof ParameterizedType parameterizedType) {
            return Arrays.stream(parameterizedType.getActualTypeArguments())
                    .map(Types::getSimpleName)
                    .collect(joining(",", "<", ">"));
        } else if (type instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            Type[] lowerBounds = wildcardType.getLowerBounds();
            return "?"
                    + (upperBounds.length == 0
                            ? ""
                            : " extends "
                                    + Arrays.stream(upperBounds)
                                            .map(Types::getSimpleName)
                                            .collect(joining(" & ")))
                    + (lowerBounds.length == 0
                            ? ""
                            : " super "
                                    + Arrays.stream(lowerBounds)
                                            .map(Types::getSimpleName)
                                            .collect(joining(" & ")));
        } else if (type instanceof GenericArrayType genericArrayType) {
            return Types.getSimpleName(genericArrayType.getGenericComponentType()) + "[]";
        }

        return type.getTypeName();
    }

    public static class TypeNotBoundException extends IllegalArgumentException {
        public TypeNotBoundException(String s) {
            super(s);
        }
    }
}
