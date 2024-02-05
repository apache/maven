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

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

/**
 * A type token for defining complex types (annotated, parameterized)
 * <p>
 * Usage example:
 * <p>
 * {@code Type listOfStringsType = new TypeT<List<String>>(){}.getType()}
 *
 * @param <T> actual type
 */
public abstract class TypeT<T> {
    private final AnnotatedType annotatedType;

    /**
     * Creates a new type token. A type argument {@link T} <b>must</b> be specified.
     * A typical usage is:
     * <p>
     * {@code TypeT<List<Integer>> integerListTypeT = new TypeT<List<Integer>>(){};}
     *
     * @throws AssertionError if a {@link TypeT} is created with a raw type
     */
    protected TypeT() {
        this.annotatedType = getSuperclassTypeParameter(this.getClass());
    }

    private static AnnotatedType getSuperclassTypeParameter(Class<?> subclass) {
        AnnotatedType superclass = subclass.getAnnotatedSuperclass();
        if (superclass instanceof AnnotatedParameterizedType) {
            return ((AnnotatedParameterizedType) superclass).getAnnotatedActualTypeArguments()[0];
        }
        throw new AssertionError();
    }

    /**
     * Returns an {@link AnnotatedType} of a {@link T}
     */
    public final AnnotatedType getAnnotatedType() {
        return annotatedType;
    }

    /**
     * Returns a {@link Type} of a {@link T}
     */
    public final Type getType() {
        return annotatedType.getType();
    }

    /**
     * Returns a raw type (e.g {@link Class}) of a {@link T}
     */
    @SuppressWarnings("unchecked")
    public final Class<T> getRawType() {
        return (Class<T>) Types.getRawType(annotatedType.getType());
    }

    @Override
    public final String toString() {
        return annotatedType.toString();
    }
}
