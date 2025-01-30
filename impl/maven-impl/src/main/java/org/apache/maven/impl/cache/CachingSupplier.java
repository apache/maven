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
package org.apache.maven.impl.cache;

import java.util.function.Function;

/**
 * A caching supplier wrapper that caches results and exceptions from the underlying supplier.
 * Used internally to cache expensive computations in the session.
 *
 * @param <REQ> The request type
 * @param <REP> The response type
 */
class CachingSupplier<REQ, REP> implements Function<REQ, REP> {
    final Function<REQ, REP> supplier;
    volatile Object value;

    CachingSupplier(Function<REQ, REP> supplier) {
        this.supplier = supplier;
    }

    Object getValue() {
        return value;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:InnerAssignment"})
    public REP apply(REQ req) {
        Object v;
        if ((v = value) == null) {
            synchronized (this) {
                if ((v = value) == null) {
                    try {
                        v = value = supplier.apply(req);
                    } catch (Exception e) {
                        v = value = new AltRes(e);
                    }
                }
            }
        }
        if (v instanceof AltRes altRes) {
            DefaultRequestCache.uncheckedThrow(altRes.t);
        }
        return (REP) v;
    }

    /**
     * Special holder class for exceptions that occur during supplier execution.
     * Allows caching and re-throwing of exceptions on subsequent calls.
     */
    static class AltRes {
        final Throwable t;

        /**
         * Creates a new AltRes with the given throwable.
         *
         * @param t The throwable to store
         */
        AltRes(Throwable t) {
            this.t = t;
        }
    }
}
