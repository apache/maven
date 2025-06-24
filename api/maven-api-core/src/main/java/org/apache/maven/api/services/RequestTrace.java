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
package org.apache.maven.api.services;

import org.apache.maven.api.annotations.Nullable;

/**
 * Represents a hierarchical trace of nested requests within a session, enabling correlation between
 * session events and their originating operations in the application code. The trace structure
 * supports the following key features:
 *
 * <ul>
 *   <li>Maintains parent-child relationships between requests to track operation nesting</li>
 *   <li>Carries contextual data describing the current request or operation</li>
 *   <li>Supports both internal session operations and client-provided trace information</li>
 * </ul>
 *
 * <p>For internal session operations, the trace typically contains {@code *Request} objects
 * that represent the current processing state. Client code can also create traces with
 * application-specific data to provide context when invoking session methods.</p>
 *
 * <p>This trace information is particularly useful for:</p>
 * <ul>
 *   <li>Debugging and troubleshooting request flows</li>
 *   <li>Audit logging of session operations</li>
 *   <li>Performance monitoring of nested operations</li>
 * </ul>
 *
 * @param context The context identifier for this request trace, helping to identify the scope or purpose
 *                of the request. May be null if no specific context is needed.
 * @param parent The parent request trace that led to this request, establishing the chain of nested
 *               operations. May be null for top-level requests.
 * @param data Additional data associated with this request trace, typically containing the actual request
 *             object being processed or any application-specific state information. May be null if no
 *             additional data is needed.
 */
public record RequestTrace(@Nullable String context, @Nullable RequestTrace parent, @Nullable Object data) {

    public static final String CONTEXT_PLUGIN = "plugin";
    public static final String CONTEXT_PROJECT = "project";
    public static final String CONTEXT_BOOTSTRAP = "bootstrap";

    public RequestTrace(RequestTrace parent, Object data) {
        this(parent != null ? parent.context() : null, parent, data);
    }
}
