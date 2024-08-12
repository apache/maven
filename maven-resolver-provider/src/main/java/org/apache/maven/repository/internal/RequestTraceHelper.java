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
package org.apache.maven.repository.internal;

import java.util.stream.Collectors;

import org.apache.maven.model.Plugin;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectStepData;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.DependencyRequest;

/**
 * Helper class to manage {@link RequestTrace} for better error logging.
 *
 * @since 3.9.9
 */
public final class RequestTraceHelper {

    /**
     * Method that creates some informational string based on passed in {@link RequestTrace}. The contents of request
     * trace can literally be anything, but this class tries to cover "most common" cases that are happening in Maven.
     */
    public static String interpretTrace(boolean detailed, RequestTrace requestTrace) {
        while (requestTrace != null) {
            Object data = requestTrace.getData();
            if (data instanceof DependencyRequest) {
                DependencyRequest request = (DependencyRequest) data;
                return "dependency resolution for " + request;
            } else if (data instanceof CollectRequest) {
                CollectRequest request = (CollectRequest) data;
                return "dependency collection for " + request;
            } else if (data instanceof CollectStepData) {
                CollectStepData stepData = (CollectStepData) data;
                String msg = "dependency collection step for " + stepData.getContext();
                if (detailed) {
                    msg += ". Path to offending node from root:\n";
                    msg += stepData.getPath().stream()
                            .map(n -> " -> " + n.toString())
                            .collect(Collectors.joining("\n"));
                    msg += "\n => " + stepData.getNode();
                }
                return msg;
            } else if (data instanceof ArtifactDescriptorRequest) {
                ArtifactDescriptorRequest request = (ArtifactDescriptorRequest) data;
                return "artifact descriptor request for " + request.getArtifact();
            } else if (data instanceof ArtifactRequest) {
                ArtifactRequest request = (ArtifactRequest) data;
                return "artifact request for " + request.getArtifact();
            } else if (data instanceof Plugin) {
                Plugin plugin = (Plugin) data;
                return "plugin request " + plugin.getId();
            }
            requestTrace = requestTrace.getParent();
        }

        return "n/a";
    }
}
