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
package org.apache.maven.api;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Checks that a compatibility accessor has an implementation before delegating to it. */
final class AccessorBridge {
    private static final ClassValue<Map<String, Class<?>>> DECLARATIONS = new ClassValue<>() {
        @Override
        protected Map<String, Class<?>> computeValue(Class<?> type) {
            Map<String, Class<?>> declarations = new HashMap<>();
            for (Method method : type.getMethods()) {
                if (method.getParameterCount() == 0) {
                    declarations.put(method.getName(), method.getDeclaringClass());
                }
            }
            return Map.copyOf(declarations);
        }
    };

    private AccessorBridge() {}

    static void requireOverride(Object instance, Class<?> owner, String legacyName, String nounName) {
        if (DECLARATIONS.get(instance.getClass()).get(legacyName) == owner) {
            throw new UnsupportedOperationException(
                    "Implementations must override either " + nounName + "() or " + legacyName + "()");
        }
    }
}
