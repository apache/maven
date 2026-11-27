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
package org.apache.maven.configuration.internal;

import java.io.File;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Determines whether a Mojo parameter type can safely be returned
 * without exposing plugin-specific classes.
 */
final class MojoParameterType {

    private MojoParameterType() {}

    static boolean isSafe(Type type) {
        if (type instanceof Class<?> clazz) {
            return isSafeClass(clazz);
        }

        if (type instanceof ParameterizedType parameterizedType) {
            if (!(parameterizedType.getRawType() instanceof Class<?> rawType)) {
                return false;
            }

            if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
                if (rawType.getClassLoader() != null) {
                    return false;
                }
            } else if (!isSafeClass(rawType)) {
                return false;
            }

            for (Type argument : parameterizedType.getActualTypeArguments()) {
                if (!isSafe(argument)) {
                    return false;
                }
            }

            return true;
        }

        if (type instanceof GenericArrayType arrayType) {
            return isSafe(arrayType.getGenericComponentType());
        }

        if (type instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();
            return upperBounds.length == 1 && isSafe(upperBounds[0]);
        }

        return false;
    }

    private static boolean isSafeClass(Class<?> type) {
        if (type.isPrimitive()) {
            return true;
        }

        if (type.isArray()) {
            return isSafe(type.getComponentType());
        }

        if (type == String.class
                || type == Boolean.class
                || type == Character.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == BigInteger.class
                || type == BigDecimal.class
                || type == File.class
                || type == Path.class
                || type == URI.class
                || type == URL.class
                || type == UUID.class) {
            return true;
        }

        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            return false;
        }

        return false;
    }
}
