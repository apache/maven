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

import java.lang.reflect.*;
import java.util.*;
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
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return getRawType(getUppermostType(upperBounds));
        } else if (type instanceof GenericArrayType) {
            Class<?> rawComponentType = getRawType(((GenericArrayType) type).getGenericComponentType());
            try {
                return Class.forName("[L" + rawComponentType.getName() + ";");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (type instanceof TypeVariable) {
            return getRawType(getUppermostType(((TypeVariable<?>) type).getBounds()));
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
            if (TypeUtils.isAssignable(type, result)) {
                result = type;
                continue;
            } else if (TypeUtils.isAssignable(result, type)) {
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
        if (type instanceof Class) {
            return ((Class<?>) type).isArray() ? new Type[] {((Class<?>) type).getComponentType()} : NO_TYPES;
        } else if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        } else if (type instanceof GenericArrayType) {
            return new Type[] {((GenericArrayType) type).getGenericComponentType()};
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

        if (type instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
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
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            Type actualType = bindings.apply(typeVariable);
            if (actualType == null) {
                throw new IllegalArgumentException("Type variable not found: " + typeVariable + " ( "
                        + typeVariable.getGenericDeclaration() + " ) ");
            }
            return actualType;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Type[] typeArguments2 = new Type[typeArguments.length];
            for (int i = 0; i < typeArguments.length; i++) {
                typeArguments2[i] = bind(typeArguments[i], bindings);
            }
            return new ParameterizedTypeImpl(
                    parameterizedType.getOwnerType(), parameterizedType.getRawType(), typeArguments2);
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return new GenericArrayTypeImpl(bind(componentType, bindings));
        }
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
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
            if (!(other instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType that = (ParameterizedType) other;
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
            if (!(other instanceof WildcardType)) {
                return false;
            }
            WildcardType that = (WildcardType) other;
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
            if (!(other instanceof GenericArrayType)) {
                return false;
            }
            GenericArrayType that = (GenericArrayType) other;
            return this.getGenericComponentType().equals(that.getGenericComponentType());
        }

        @Override
        public String toString() {
            return Types.toString(componentType) + "[]";
        }
    }

    private static String toString(Type type) {
        return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
    }

    /**
     * Returns a simple name for a given {@link Type}
     *
     * @see Class#getSimpleName()
     */
    public static String getSimpleName(Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).getSimpleName();
        } else if (type instanceof ParameterizedType) {
            return Arrays.stream(((ParameterizedType) type).getActualTypeArguments())
                    .map(Types::getSimpleName)
                    .collect(joining(",", "<", ">"));
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
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
        } else if (type instanceof GenericArrayType) {
            return Types.getSimpleName(((GenericArrayType) type).getGenericComponentType()) + "[]";
        }

        return type.getTypeName();
    }
}
