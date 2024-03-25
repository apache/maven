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
import java.util.function.Function;
import java.util.stream.Collectors;

class Utils {
    static <T> T nonNull(T t) {
        if (t == null) {
            throw new IllegalArgumentException();
        }
        return t;
    }

    static <T> T nonNull(T t, String name) {
        if (t == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return t;
    }

    static <T> T cast(Class<T> clazz, Object o, String name) {
        if (!clazz.isInstance(o)) {
            throw new IllegalArgumentException(name + " is not an instance of " + clazz.getName());
        }
        return clazz.cast(o);
    }

    static <U, V> List<V> map(Collection<U> list, Function<U, V> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }
}
