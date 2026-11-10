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
package org.apache.maven.impl;

import java.util.function.Function;

import org.apache.maven.api.cache.RequestCache;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.Result;
import org.apache.maven.impl.cache.DefaultRequestCacheFactory;

import static java.util.Objects.requireNonNull;

/**
 * Submits operations to the session pipeline, including requests made during bootstrap.
 * Normal sessions and bootstrap requests converge in {@link #execute(Request, Function, RequestCache)}.
 * This is the shared execution point for operation lifecycle handling. Services enter and exit
 * their trace scope around the request call.
 */
public final class RequestExecutor {
    private RequestExecutor() {}

    public static <REQ extends Request<?>, REP extends Result<REQ>> REP request(
            REQ request, Function<REQ, REP> supplier) {
        requireNonNull(request, "request");
        if (request.getSession() instanceof InternalSession session) {
            return session.request(request, supplier);
        }
        return execute(request, supplier, Bootstrap.CACHE);
    }

    static <REQ extends Request<?>, REP extends Result<REQ>> REP execute(
            REQ request, Function<REQ, REP> supplier, RequestCache cache) {
        return cache.request(request, supplier);
    }

    private static final class Bootstrap {
        private static final RequestCache CACHE = new DefaultRequestCacheFactory().createCache();
    }
}
