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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.ReactorFailureBehavior;
import org.apache.maven.execution.MavenSession;

/**
 * Internal immutable implementation of {@link BuildInvocation}.
 *
 * @param goals                  requested goals/phases
 * @param userProperties         user properties with sensitive values redacted
 * @param systemInfo             curated system properties for reproducibility
 * @param localRepository        local repository path
 * @param activeProfiles         explicitly activated profiles
 * @param selectedProjects       projects selected via {@code -pl}
 * @param resumeFrom             resume-from selector, or {@code null}
 * @param reactorFailureBehavior reactor failure behavior
 * @param offline                whether offline mode was active
 * @param updateSnapshots        whether snapshot updates were forced
 * @param noTransferProgress     whether transfer progress was suppressed
 * @param batchMode              whether batch mode was active
 * @param threads                degree of concurrency
 */
public record DefaultBuildInvocation(
        List<String> goals,
        Map<String, String> userProperties,
        Map<String, String> systemInfo,
        Path localRepository,
        List<String> activeProfiles,
        List<String> selectedProjects,
        String resumeFrom,
        ReactorFailureBehavior reactorFailureBehavior,
        boolean offline,
        boolean updateSnapshots,
        boolean noTransferProgress,
        boolean batchMode,
        int threads)
        implements BuildInvocation {

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS =
            Set.of("password", "passwd", "secret", "token", "apikey", "api_key", "credential", "passphrase");

    public DefaultBuildInvocation {
        goals = List.copyOf(goals);
        userProperties = Map.copyOf(userProperties);
        systemInfo = Map.copyOf(systemInfo);
        activeProfiles = List.copyOf(activeProfiles);
        selectedProjects = List.copyOf(selectedProjects);
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
        addSystemProp(sysInfo, "user.home");
        addSystemProp(sysInfo, "user.name");
        sysInfo.put("available.processors", String.valueOf(Runtime.getRuntime().availableProcessors()));

        Path localRepo = request.getLocalRepositoryPath() != null
                ? request.getLocalRepositoryPath().toPath()
                : Path.of("");

        List<String> activeProfiles =
                request.getActiveProfiles() != null ? List.copyOf(request.getActiveProfiles()) : List.of();

        List<String> selectedProjects =
                request.getSelectedProjects() != null ? List.copyOf(request.getSelectedProjects()) : List.of();

        String resumeFrom = request.getResumeFrom();

        ReactorFailureBehavior failureBehavior = ReactorFailureBehavior.FAIL_FAST;
        if (request.getReactorFailureBehavior() != null) {
            try {
                failureBehavior = ReactorFailureBehavior.fromString(request.getReactorFailureBehavior());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new DefaultBuildInvocation(
                goals,
                Map.copyOf(userProps),
                Map.copyOf(sysInfo),
                localRepo,
                activeProfiles,
                selectedProjects,
                resumeFrom,
                failureBehavior,
                request.isOffline(),
                request.isUpdateSnapshots(),
                request.isNoTransferProgress(),
                !request.isInteractiveMode(),
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
