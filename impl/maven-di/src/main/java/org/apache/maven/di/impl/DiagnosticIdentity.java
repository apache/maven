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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/** Weak identity handles shared by captures in one diagnostic history. */
final class DiagnosticIdentity {
    private final ReferenceQueue<Object> collected = new ReferenceQueue<>();
    private final Map<IdentityReference, Long> tokens = new HashMap<>();
    private final ToIntFunction<Object> hash;
    private final Map<Long, String> loaderLabels = new HashMap<>();
    private final Map<String, String> loaders = new LinkedHashMap<>();
    private long nextToken;

    DiagnosticIdentity() {
        this(System::identityHashCode);
    }

    // A hash function seam makes collisions reproducible without allocating until the JVM happens to collide.
    DiagnosticIdentity(ToIntFunction<Object> hash) {
        this.hash = hash;
    }

    long token(Object object) {
        for (Object reference; (reference = collected.poll()) != null; ) {
            tokens.remove(reference);
        }
        IdentityReference lookup = new IdentityReference(object, null, hash.applyAsInt(object));
        Long token = tokens.get(lookup);
        if (token == null) {
            token = ++nextToken;
            tokens.put(new IdentityReference(object, collected, lookup.hashCode()), token);
        }
        return token;
    }

    String loader(ClassLoader loader) {
        if (loader == null) {
            return "bootstrap";
        }
        long token = token(loader);
        return loaderLabels.computeIfAbsent(token, value -> {
            String label = "L" + (loaderLabels.size() + 1);
            loaders.put(label, Binding.identityString(loader));
            return label;
        });
    }

    Map<String, String> loaders() {
        return loaders;
    }

    private static final class IdentityReference extends WeakReference<Object> {
        private final int hash;

        IdentityReference(Object referent, ReferenceQueue<Object> queue, int hash) {
            super(referent, queue);
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            Object referent = get();
            return referent != null && object instanceof IdentityReference other && referent == other.get();
        }
    }
}
