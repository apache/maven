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
package org.apache.maven.internal.build;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.BuildEnvironment;

/**
 * Internal immutable implementation of {@link BuildEnvironment}.
 */
record DefaultBuildEnvironment(
        List<String> goals,
        Map<String, String> userProperties,
        Map<String, String> systemInfo,
        String localRepository,
        List<String> activeProfiles,
        List<String> selectedProjects,
        String resumeFrom,
        String reactorFailureBehavior,
        boolean offline,
        boolean updateSnapshots,
        boolean noTransferProgress,
        boolean batchMode,
        int threads)
        implements BuildEnvironment {

    @Override
    public List<String> goals() {
        return List.copyOf(goals);
    }

    @Override
    public Map<String, String> userProperties() {
        return Map.copyOf(userProperties);
    }

    @Override
    public Map<String, String> systemInfo() {
        return Map.copyOf(systemInfo);
    }

    @Override
    public List<String> activeProfiles() {
        return List.copyOf(activeProfiles);
    }

    @Override
    public List<String> selectedProjects() {
        return List.copyOf(selectedProjects);
    }
}
