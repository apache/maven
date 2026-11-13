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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.services.Request;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectStepData;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.DependencyRequest;

/**
 * Helper for tracking Maven operation requests and their Resolver trace representations.
 * This class provides utilities to:
 * - Track nested Maven operations, including bootstrap requests
 * - Convert between Maven and Resolver trace formats
 * - Generate human-readable interpretations of trace data
 */
public final class RequestTraceHelper {
    private static final ThreadLocal<Map<ProtoSession, org.apache.maven.api.services.RequestTrace>> BOOTSTRAP_TRACES =
            ThreadLocal.withInitial(IdentityHashMap::new);

    /**
     * Returns the active trace, including during bootstrap before a full session exists.
     *
     * @param session the session
     * @return the current trace, or null
     */
    public static org.apache.maven.api.services.RequestTrace getCurrentTrace(ProtoSession session) {
        return session instanceof InternalSession fullSession
                ? fullSession.getCurrentTrace()
                : BOOTSTRAP_TRACES.get().get(session);
    }

    private static void setCurrentTrace(ProtoSession session, org.apache.maven.api.services.RequestTrace trace) {
        if (session instanceof InternalSession fullSession) {
            fullSession.setCurrentTrace(trace);
        } else {
            Map<ProtoSession, org.apache.maven.api.services.RequestTrace> traces = BOOTSTRAP_TRACES.get();
            if (trace != null) {
                traces.put(session, trace);
            } else {
                traces.remove(session);
                if (traces.isEmpty()) {
                    BOOTSTRAP_TRACES.remove();
                }
            }
        }
    }

    /**
     * Represents a resolver trace containing both Maven and Resolver-specific trace information
     * @param session The current Maven session
     * @param context The trace context
     * @param trace The Resolver-specific trace
     * @param mvnTrace The Maven-specific trace
     * @param previousMvnTrace The Maven trace to restore when leaving this scope
     */
    public record ResolverTrace(
            ProtoSession session,
            String context,
            RequestTrace trace,
            org.apache.maven.api.services.RequestTrace mvnTrace,
            org.apache.maven.api.services.RequestTrace previousMvnTrace) {}

    /**
     * Enters a trace scope, using the request's explicit trace when present.
     * Otherwise, creates a trace from the session's current trace and the supplied data.
     * @param session The current full or bootstrap session
     * @param data The data object to associate with the trace
     * @return A new ResolverTrace containing both Maven and Resolver trace information
     */
    public static ResolverTrace enter(ProtoSession session, Object data) {
        org.apache.maven.api.services.RequestTrace previousTrace = getCurrentTrace(session);
        org.apache.maven.api.services.RequestTrace trace = data instanceof Request<?> req && req.getTrace() != null
                ? req.getTrace()
                : new org.apache.maven.api.services.RequestTrace(previousTrace, data);
        setCurrentTrace(session, trace);
        return new ResolverTrace(session, trace.context(), toResolver(trace), trace, previousTrace);
    }

    /**
     * Restores the trace that was current before entering this scope.
     * @param trace The current resolver trace to exit from
     */
    public static void exit(ResolverTrace trace) {
        setCurrentTrace(trace.session(), trace.previousMvnTrace());
    }

    /**
     * Converts a Resolver trace to a Maven trace
     * @param context The context string for the new Maven trace
     * @param trace The Resolver trace to convert
     * @return A new Maven trace, or null if the input trace was null
     */
    public static org.apache.maven.api.services.RequestTrace toMaven(String context, RequestTrace trace) {
        if (trace != null) {
            return new org.apache.maven.api.services.RequestTrace(
                    context, toMaven(context, trace.getParent()), trace.getData());
        } else {
            return null;
        }
    }

    /**
     * Converts a Maven trace to a Resolver trace
     * @param trace The Maven trace to convert
     * @return A new Resolver trace, or null if the input trace was null
     */
    public static RequestTrace toResolver(org.apache.maven.api.services.RequestTrace trace) {
        if (trace != null) {
            return RequestTrace.newChild(toResolver(trace.parent()), trace.data());
        } else {
            return null;
        }
    }

    /**
     * Creates a human-readable interpretation of a request trace
     * @param detailed If true, includes additional details such as dependency paths
     * @param requestTrace The trace to interpret
     * @return A string describing the trace context and relevant details
     */
    public static String interpretTrace(boolean detailed, RequestTrace requestTrace) {
        while (requestTrace != null) {
            Object data = requestTrace.getData();
            if (data instanceof DependencyRequest request) {
                return "dependency resolution for " + request;
            } else if (data instanceof CollectRequest request) {
                return "dependency collection for " + request;
            } else if (data instanceof CollectStepData stepData) {
                String msg = "dependency collection step for " + stepData.getContext();
                if (detailed) {
                    msg += ". Path to offending node from root:\n";
                    msg += stepData.getPath().stream()
                            .map(n -> " -> " + n.toString())
                            .collect(Collectors.joining("\n"));
                    msg += "\n => " + stepData.getNode();
                }
                return msg;
            } else if (data instanceof ArtifactDescriptorRequest request) {
                return "artifact descriptor request for " + request.getArtifact();
            } else if (data instanceof ArtifactRequest request) {
                return "artifact request for " + request.getArtifact();
            } else if (data instanceof org.apache.maven.api.model.Plugin plugin) {
                return "plugin " + plugin.getId();
            }
            requestTrace = requestTrace.getParent();
        }

        return "n/a";
    }
}
