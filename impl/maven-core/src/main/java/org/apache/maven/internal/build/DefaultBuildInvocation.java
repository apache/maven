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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.MakeBehavior;
import org.apache.maven.api.ReactorFailureBehavior;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal immutable implementation of {@link BuildInvocation}.
 *
 * @param goals the requested goals or lifecycle phases
 * @param userProperties user-provided properties (sensitive values redacted)
 * @param systemInfo captured JVM and OS system properties
 * @param activeProfiles active Maven profiles
 * @param selectedProjects selected projects for the reactor
 * @param excludedProjects excluded projects from the reactor
 * @param makeBehavior upstream/downstream make behavior flag, or {@code null} if unset
 * @param resumeFrom project to resume the build from, or {@code null} if unset
 * @param reactorFailureBehavior how the reactor responds to build failures
 * @param offline whether the build runs in offline mode
 * @param updateSnapshots whether snapshot update checks are forced
 * @param batchMode whether Maven runs in non-interactive (batch) mode
 * @param recursive whether sub-modules are built recursively
 * @param resume whether the build resumes from a previous failure point
 * @param threads degree of build concurrency
 */
public record DefaultBuildInvocation(
        List<String> goals,
        Map<String, String> userProperties,
        Map<String, String> systemInfo,
        List<String> activeProfiles,
        List<String> selectedProjects,
        List<String> excludedProjects,
        MakeBehavior makeBehavior,
        String resumeFrom,
        ReactorFailureBehavior reactorFailureBehavior,
        boolean offline,
        boolean updateSnapshots,
        boolean batchMode,
        boolean recursive,
        boolean resume,
        int threads)
        implements BuildInvocation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBuildInvocation.class);

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS =
            Set.of("password", "passwd", "secret", "token", "apikey", "api_key", "credential", "passphrase");

    public DefaultBuildInvocation {
        goals = List.copyOf(goals);
        userProperties = Map.copyOf(userProperties);
        systemInfo = Map.copyOf(systemInfo);
        activeProfiles = List.copyOf(activeProfiles);
        selectedProjects = List.copyOf(selectedProjects);
        excludedProjects = List.copyOf(excludedProjects);
    }

    /**
     * Creates a {@link BuildInvocation} snapshot from the current session state.
     *
     * @param session the Maven session
     * @return a new immutable build invocation instance
     */
    public static BuildInvocation from(MavenSession session) {
        var request = session.getRequest();

        List<String> goals = request.getGoals() != null ? List.copyOf(request.getGoals()) : List.of();

        Map<String, String> userProps = new LinkedHashMap<>();
        if (request.getUserProperties() != null) {
            request.getUserProperties().forEach((k, v) -> {
                String key = String.valueOf(k);
                String value = isSensitiveKey(key) ? "***" : String.valueOf(v);
                userProps.put(key, value);
            });
        }

        Map<String, String> sysInfo = new TreeMap<>();
        addSystemProp(sysInfo, "os.name");
        addSystemProp(sysInfo, "os.arch");
        addSystemProp(sysInfo, "os.version");
        addSystemProp(sysInfo, "java.vendor");
        addSystemProp(sysInfo, "java.vm.name");
        addSystemProp(sysInfo, "java.vm.version");
        addSystemProp(sysInfo, "maven.home");
        // Note: user.home and user.name are captured for diagnostics (e.g. identifying
        // CI vs developer builds). The report is written to target/build-reports/ which
        // is local and gitignored. Do not include these properties in reports that may
        // be shared externally.
        addSystemProp(sysInfo, "user.home");
        addSystemProp(sysInfo, "user.name");
        sysInfo.put("available.processors", String.valueOf(Runtime.getRuntime().availableProcessors()));

        List<String> activeProfiles =
                request.getActiveProfiles() != null ? List.copyOf(request.getActiveProfiles()) : List.of();

        List<String> selectedProjects =
                request.getSelectedProjects() != null ? List.copyOf(request.getSelectedProjects()) : List.of();

        List<String> excludedProjects =
                request.getExcludedProjects() != null ? List.copyOf(request.getExcludedProjects()) : List.of();

        MakeBehavior makeBehavior = null;
        if (request.getMakeBehavior() != null) {
            try {
                makeBehavior = MakeBehavior.fromString(request.getMakeBehavior());
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Unknown make behavior '{}', ignoring", request.getMakeBehavior());
            }
        }

        String resumeFrom = request.getResumeFrom();

        ReactorFailureBehavior failureBehavior = ReactorFailureBehavior.FAIL_FAST;
        if (request.getReactorFailureBehavior() != null) {
            try {
                failureBehavior = ReactorFailureBehavior.fromString(request.getReactorFailureBehavior());
            } catch (IllegalArgumentException e) {
                LOGGER.debug(
                        "Unknown reactor failure behavior '{}', defaulting to FAIL_FAST",
                        request.getReactorFailureBehavior());
            }
        }

        return new DefaultBuildInvocation(
                goals,
                Map.copyOf(userProps),
                Map.copyOf(sysInfo),
                activeProfiles,
                selectedProjects,
                excludedProjects,
                makeBehavior,
                resumeFrom,
                failureBehavior,
                request.isOffline(),
                request.isUpdateSnapshots(),
                !request.isInteractiveMode(),
                request.isRecursive(),
                request.isResume(),
                request.getDegreeOfConcurrency());
    }

    static boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (String fragment : SENSITIVE_KEY_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static void addSystemProp(Map<String, String> map, String key) {
        String value = System.getProperty(key);
        if (value != null) {
            map.put(key, value);
        }
    }
}
