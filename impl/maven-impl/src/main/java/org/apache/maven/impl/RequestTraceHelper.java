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

import java.util.stream.Collectors;

import org.apache.maven.api.Session;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.services.Request;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectStepData;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.DependencyRequest;

/**
 * Helper class to manage request tracing for improved error logging in Maven's dependency resolution.
 * This class provides utilities to:
 * - Track request traces through Maven's dependency resolution process
 * - Convert between Maven and Resolver trace formats
 * - Generate human-readable interpretations of trace data
 */
public final class RequestTraceHelper {

    /**
     * Represents a resolver trace containing both Maven and Resolver-specific trace information
     * @param session The current Maven session
     * @param context The trace context
     * @param trace The Resolver-specific trace
     * @param mvnTrace The Maven-specific trace
     */
    public record ResolverTrace(
            Session session, String context, RequestTrace trace, org.apache.maven.api.services.RequestTrace mvnTrace) {}

    /**
     * Creates a new trace entry and updates the session's current trace
     * @param session The current Maven session
     * @param data The data object to associate with the trace
     * @return A new ResolverTrace containing both Maven and Resolver trace information
     */
    public static ResolverTrace enter(Session session, Object data) {
        InternalSession iSession = InternalSession.from(session);
        org.apache.maven.api.services.RequestTrace trace = data instanceof Request<?> req && req.getTrace() != null
                ? req.getTrace()
                : new org.apache.maven.api.services.RequestTrace(iSession.getCurrentTrace(), data);
        iSession.setCurrentTrace(trace);
        return new ResolverTrace(session, trace.context(), toResolver(trace), trace);
    }

    /**
     * Restores the parent trace as the current trace in the session
     * @param trace The current resolver trace to exit from
     */
    public static void exit(ResolverTrace trace) {
        InternalSession iSession = InternalSession.from(trace.session());
        iSession.setCurrentTrace(trace.mvnTrace().parent());
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
            } else if (data instanceof Plugin plugin) {
                return "plugin " + plugin.getId();
            }
            requestTrace = requestTrace.getParent();
        }

        return "n/a";
    }
}
