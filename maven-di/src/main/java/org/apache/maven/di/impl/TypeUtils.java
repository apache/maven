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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class contains reflection utilities to work with Java types.
 * Its main use is for method {@link Types#parameterizedType Types.parameterized}.
 * However, just like with {@link ReflectionUtils}, other type utility
 * methods are pretty clean too, so they are left public.
 */
public final class TypeUtils {

    public static boolean isInheritedFrom(Type type, Type from, Map<Type, Type> dejaVu) {
        if (from == Object.class) {
            return true;
        }
        if (matches(type, from, dejaVu) || matches(from, type, dejaVu)) {
            return true;
        }
        if (!(type instanceof Class || type instanceof ParameterizedType || type instanceof GenericArrayType)) {
            return false;
        }
        Class<?> rawType = Types.getRawType(type);

        Type superclass = rawType.getGenericSuperclass();
        if (superclass != null && isInheritedFrom(superclass, from, dejaVu)) {
            return true;
        }
        return Arrays.stream(rawType.getGenericInterfaces()).anyMatch(iface -> isInheritedFrom(iface, from, dejaVu));
    }

    public static boolean matches(Type strict, Type pattern) {
        return matches(strict, pattern, new HashMap<>());
    }

    private static boolean matches(Type strict, Type pattern, Map<Type, Type> dejaVu) {
        if (strict.equals(pattern) || dejaVu.get(strict) == pattern) {
            return true;
        }
        dejaVu.put(strict, pattern);
        try {
            if (pattern instanceof WildcardType) {
                WildcardType wildcard = (WildcardType) pattern;
                return Arrays.stream(wildcard.getUpperBounds())
                                .allMatch(bound -> isInheritedFrom(strict, bound, dejaVu))
                        && Arrays.stream(wildcard.getLowerBounds())
                                .allMatch(bound -> isInheritedFrom(bound, strict, dejaVu));
            }
            if (pattern instanceof TypeVariable<?>) {
                TypeVariable<?> typevar = (TypeVariable<?>) pattern;
                return Arrays.stream(typevar.getBounds()).allMatch(bound -> isInheritedFrom(strict, bound, dejaVu));
            }
            if (strict instanceof GenericArrayType && pattern instanceof GenericArrayType) {
                return matches(
                        ((GenericArrayType) strict).getGenericComponentType(),
                        ((GenericArrayType) pattern).getGenericComponentType(),
                        dejaVu);
            }
            if (!(strict instanceof ParameterizedType) || !(pattern instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType parameterizedStrict = (ParameterizedType) strict;
            ParameterizedType parameterizedPattern = (ParameterizedType) pattern;
            if (parameterizedPattern.getOwnerType() != null) {
                if (parameterizedStrict.getOwnerType() == null) {
                    return false;
                }
                if (!matches(parameterizedPattern.getOwnerType(), parameterizedStrict.getOwnerType(), dejaVu)) {
                    return false;
                }
            }
            if (!matches(parameterizedPattern.getRawType(), parameterizedStrict.getRawType(), dejaVu)) {
                return false;
            }

            Type[] strictParams = parameterizedStrict.getActualTypeArguments();
            Type[] patternParams = parameterizedPattern.getActualTypeArguments();
            if (strictParams.length != patternParams.length) {
                return false;
            }
            for (int i = 0; i < strictParams.length; i++) {
                if (!matches(strictParams[i], patternParams[i], dejaVu)) {
                    return false;
                }
            }
            return true;
        } finally {
            dejaVu.remove(strict);
        }
    }

    public static boolean contains(Type type, Type sub) {
        if (type.equals(sub)) {
            return true;
        }
        if (type instanceof GenericArrayType) {
            return contains(((GenericArrayType) type).getGenericComponentType(), sub);
        }
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterized = (ParameterizedType) type;
        if (contains(parameterized.getRawType(), sub)) {
            return true;
        }
        if (parameterized.getOwnerType() != null && contains(parameterized.getOwnerType(), sub)) {
            return true;
        }
        return Arrays.stream(parameterized.getActualTypeArguments()).anyMatch(argument -> contains(argument, sub));
    }

    // pattern = Map<K, List<V>>
    // real    = Map<String, List<Integer>>
    //
    // result  = {K -> String, V -> Integer}
    public static Map<TypeVariable<?>, Type> extractMatchingGenerics(Type pattern, Type real) {
        Map<TypeVariable<?>, Type> result = new HashMap<>();
        extractMatchingGenerics(pattern, real, result);
        return result;
    }

    private static void extractMatchingGenerics(Type pattern, Type real, Map<TypeVariable<?>, Type> result) {
        if (pattern instanceof TypeVariable) {
            result.put((TypeVariable<?>) pattern, real);
            return;
        }
        if (pattern.equals(real)) {
            return;
        }
        if (pattern instanceof GenericArrayType && real instanceof GenericArrayType) {
            extractMatchingGenerics(
                    ((GenericArrayType) pattern).getGenericComponentType(),
                    ((GenericArrayType) real).getGenericComponentType(),
                    result);
            return;
        }
        if (!(pattern instanceof ParameterizedType) || !(real instanceof ParameterizedType)) {
            return;
        }
        ParameterizedType parameterizedPattern = (ParameterizedType) pattern;
        ParameterizedType parameterizedReal = (ParameterizedType) real;
        if (!parameterizedPattern.getRawType().equals(parameterizedReal.getRawType())) {
            return;
        }
        extractMatchingGenerics(parameterizedPattern.getRawType(), parameterizedReal.getRawType(), result);
        if (!Objects.equals(parameterizedPattern.getOwnerType(), parameterizedReal.getOwnerType())) {
            return;
        }
        if (parameterizedPattern.getOwnerType() != null) {
            extractMatchingGenerics(parameterizedPattern.getOwnerType(), parameterizedReal.getOwnerType(), result);
        }
        Type[] patternTypeArgs = parameterizedPattern.getActualTypeArguments();
        Type[] realTypeArgs = parameterizedReal.getActualTypeArguments();
        if (patternTypeArgs.length != realTypeArgs.length) {
            return;
        }
        for (int i = 0; i < patternTypeArgs.length; i++) {
            extractMatchingGenerics(patternTypeArgs[i], realTypeArgs[i], result);
        }
    }

    public static Type simplifyType(Type original) {
        if (original instanceof Class) {
            return original;
        }

        if (original instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) original).getGenericComponentType();
            Type repackedComponentType = simplifyType(componentType);
            if (componentType != repackedComponentType) {
                return Types.genericArrayType(repackedComponentType);
            }
            return original;
        }

        if (original instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) original;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Type[] repackedTypeArguments = simplifyTypes(typeArguments);

            if (isAllObjects(repackedTypeArguments)) {
                return parameterizedType.getRawType();
            }

            if (typeArguments != repackedTypeArguments) {
                return Types.parameterizedType(
                        parameterizedType.getOwnerType(), parameterizedType.getRawType(), repackedTypeArguments);
            }
            return original;
        }

        if (original instanceof TypeVariable) {
            throw new IllegalArgumentException("Key should not contain a type variable: " + original);
        }

        if (original instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) original;
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
        if (to instanceof Class && from instanceof Class) {
            return ((Class<?>) to).isAssignableFrom((Class<?>) from);
        }
        return isAssignable(to, from, false);
    }

    private static boolean isAssignable(Type to, Type from, boolean strict) {
        if (to instanceof WildcardType || from instanceof WildcardType) {
            Type[] toUppers, toLowers;
            if (to instanceof WildcardType) {
                WildcardType wildcardTo = (WildcardType) to;
                toUppers = wildcardTo.getUpperBounds();
                toLowers = wildcardTo.getLowerBounds();
            } else {
                toUppers = new Type[] {to};
                toLowers = strict ? toUppers : Types.NO_TYPES;
            }

            Type[] fromUppers, fromLowers;
            if (from instanceof WildcardType) {
                WildcardType wildcardTo = (WildcardType) to;
                fromUppers = wildcardTo.getUpperBounds();
                fromLowers = wildcardTo.getLowerBounds();
            } else {
                fromUppers = new Type[] {from};
                fromLowers = strict ? fromUppers : Types.NO_TYPES;
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
            to = Types.getRawType(to);
        }
        if (from instanceof GenericArrayType) {
            from = Types.getRawType(from);
        }
        if (!strict && to instanceof Class) {
            return ((Class<?>) to).isAssignableFrom(Types.getRawType(from));
        }
        Class<?> toRawClazz = Types.getRawType(to);
        Type[] toTypeArguments = Types.getActualTypeArguments(to);
        return isAssignable(toRawClazz, toTypeArguments, from, strict);
    }

    private static boolean isAssignable(Class<?> toRawClazz, Type[] toTypeArguments, Type from, boolean strict) {
        Class<?> fromRawClazz = Types.getRawType(from);
        if (strict && !toRawClazz.equals(fromRawClazz)) {
            return false;
        }
        if (!strict && !toRawClazz.isAssignableFrom(fromRawClazz)) {
            return false;
        }
        if (toRawClazz.isArray()) {
            return true;
        }
        Type[] fromTypeArguments = Types.getActualTypeArguments(from);
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
        Map<TypeVariable<?>, Type> typeBindings = Types.getTypeBindings(from);
        for (Type anInterface : fromRawClazz.getGenericInterfaces()) {
            if (isAssignable(
                    toRawClazz,
                    toTypeArguments,
                    Types.bind(anInterface, key -> typeBindings.getOrDefault(key, Types.wildcardTypeAny())),
                    false)) {
                return true;
            }
        }
        Type superclass = fromRawClazz.getGenericSuperclass();
        return superclass != null
                && isAssignable(toRawClazz, toTypeArguments, Types.bind(superclass, typeBindings), false);
    }
}
