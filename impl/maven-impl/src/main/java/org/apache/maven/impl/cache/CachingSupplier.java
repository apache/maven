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
public class CachingSupplier<REQ, REP> implements Function<REQ, REP> {
    protected final Function<REQ, REP> supplier;
    protected volatile Object value;

    public CachingSupplier(Function<REQ, REP> supplier) {
        this.supplier = supplier;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Directly sets the cached value without invoking the supplier, then notifies
     * any threads blocked in {@link #apply(Object)} waiting for the result.
     * <p>
     * This is used by {@link AbstractRequestCache#requests} to set batch-resolved results
     * on CachingSupplier instances. Concurrent callers blocked in {@code apply()} will
     * be woken up and see this value instead of invoking the supplier redundantly.
     *
     * @param result the result to cache (may be a normal result or an {@link AltRes} for errors)
     */
    public void complete(Object result) {
        synchronized (this) {
            if (value == null) {
                value = result;
            }
            this.notifyAll();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:InnerAssignment"})
    public REP apply(REQ req) {
        Object v;
        if ((v = value) == null) {
            synchronized (this) {
                if ((v = value) == null) {
                    // If a batch resolution is in progress on another thread, wait for
                    // complete() rather than invoking the supplier redundantly.
                    // Re-entrant calls on the SAME thread must NOT wait (that would deadlock),
                    // so AbstractRequestCache marks CachingSuppliers it is currently resolving
                    // in a ThreadLocal; those are excluded from waiting.
                    if (batchResolving && !AbstractRequestCache.isResolvingOnCurrentThread(this)) {
                        while ((v = value) == null && batchResolving) {
                            try {
                                this.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    if ((v = value) == null) {
                        try {
                            v = value = supplier.apply(req);
                        } catch (Exception e) {
                            v = value = new AltRes(e);
                        }
                    }
                }
            }
        }
        if (v instanceof AltRes altRes) {
            DefaultRequestCache.uncheckedThrow(altRes.throwable);
        }
        return (REP) v;
    }

    /**
     * Marks this supplier as having a batch resolution in progress.
     * Concurrent threads calling {@link #apply} will wait for {@link #complete}
     * instead of invoking the supplier independently.
     * <p>
     * When clearing the flag ({@code resolving = false}), any threads still blocked
     * in {@link #apply(Object)} are notified so they can proceed to the fallback
     * supplier. This handles edge cases where {@link #complete} was never called
     * (e.g., batch supplier returned fewer results than expected).
     *
     * @param resolving {@code true} when batch resolution starts, {@code false} when it ends
     */
    void setBatchResolving(boolean resolving) {
        this.batchResolving = resolving;
        if (!resolving) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    private volatile boolean batchResolving;

    /**
     * Special holder class for exceptions that occur during supplier execution.
     * Allows caching and re-throwing of exceptions on subsequent calls.
     */
    public static class AltRes {
        protected final Throwable throwable;

        /**
         * Creates a new AltRes with the given throwable.
         *
         * @param throwable The throwable to store
         */
        public AltRes(Throwable throwable) {
            this.throwable = throwable;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
