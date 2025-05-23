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
package org.apache.maven.internal.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

class CoreUtils {

    /**
     * Casts an object to the specified type, with validation and error handling.
     *
     * @param <T> the target type
     * @param clazz the class representing the target type
     * @param o the object to cast
     * @param name the name of the parameter for error messages
     * @return the cast object
     * @throws NullPointerException if the object is null
     * @throws ClassCastException if the object is not an instance of the target type
     */
    @Nonnull
    public static <T> T cast(@Nonnull Class<T> clazz, @Nullable Object o, @Nonnull String name) {
        if (!clazz.isInstance(o)) {
            if (o == null) {
                throw new NullPointerException(name + " is null");
            }
            throw new ClassCastException(name + " is not an instance of " + clazz.getName());
        }
        return clazz.cast(o);
    }

    @Nonnull
    public static <U, V> List<V> map(@Nonnull Collection<U> list, @Nonnull Function<U, V> mapper) {
        return list.stream().map(mapper).filter(Objects::nonNull).toList();
    }
}
