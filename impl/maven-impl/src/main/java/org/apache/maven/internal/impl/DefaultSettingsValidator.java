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
package org.apache.maven.internal.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.settings.Mirror;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;

/**
 */
public class DefaultSettingsValidator {

    private static final String ID = "[\\w.-]+";
    private static final Pattern ID_REGEX = Pattern.compile(ID);

    private static final String ILLEGAL_REPO_ID_CHARS = "\\/:\"<>|?*"; // ILLEGAL_FS_CHARS

    @SuppressWarnings("checkstyle:MethodLength")
    public void validate(Settings settings, boolean isProjectSettings, List<BuilderProblem> problems) {
        if (isProjectSettings) {
            String msgS = "is not supported on project settings.";
            String msgP = "are not supported on project settings.";
            if (settings.getLocalRepository() != null
                    && !settings.getLocalRepository().isEmpty()) {
                addViolation(problems, BuilderProblem.Severity.WARNING, "localRepository", null, msgS);
            }
            if (!settings.isInteractiveMode()) {
                addViolation(problems, BuilderProblem.Severity.WARNING, "interactiveMode", null, msgS);
            }
            if (settings.isOffline()) {
                addViolation(problems, BuilderProblem.Severity.WARNING, "offline", null, msgS);
            }
            if (!settings.getProxies().isEmpty()) {
                addViolation(problems, BuilderProblem.Severity.WARNING, "proxies", null, msgP);
            }
            if (settings.isUsePluginRegistry()) {
                addViolation(problems, BuilderProblem.Severity.WARNING, "usePluginRegistry", null, msgS);
            }
            List<Server> servers = settings.getServers();
            for (int i = 0; i < servers.size(); i++) {
                Server server = servers.get(i);
                String serverField = "servers.server[" + i + "]";
                validateStringEmpty(problems, serverField + ".username", server.getUsername(), msgS);
                validateStringEmpty(problems, serverField + ".password", server.getPassword(), msgS);
                validateStringEmpty(problems, serverField + ".privateKey", server.getPrivateKey(), msgS);
                validateStringEmpty(problems, serverField + ".passphrase", server.getPassphrase(), msgS);
                validateStringEmpty(problems, serverField + ".filePermissions", server.getFilePermissions(), msgS);
                validateStringEmpty(
                        problems, serverField + ".directoryPermissions", server.getDirectoryPermissions(), msgS);
            }
        }

        if (settings.isUsePluginRegistry()) {
            addViolation(
                    problems,
                    BuilderProblem.Severity.WARNING,
                    "usePluginRegistry",
                    null,
                    "is deprecated and has no effect.");
        }

        List<String> pluginGroups = settings.getPluginGroups();

        if (pluginGroups != null) {
            for (int i = 0; i < pluginGroups.size(); i++) {
                String pluginGroup = pluginGroups.get(i);

                validateStringNotEmpty(problems, "pluginGroups.pluginGroup[" + i + "]", pluginGroup, null);

                if (!ID_REGEX.matcher(pluginGroup).matches()) {
                    addViolation(
                            problems,
                            BuilderProblem.Severity.ERROR,
                            "pluginGroups.pluginGroup[" + i + "]",
                            null,
                            "must denote a valid group id and match the pattern " + ID);
                }
            }
        }

        List<Server> servers = settings.getServers();

        if (servers != null) {
            Set<String> serverIds = new HashSet<>();

            for (int i = 0; i < servers.size(); i++) {
                Server server = servers.get(i);

                validateStringNotEmpty(problems, "servers.server[" + i + "].id", server.getId(), null);

                if (!serverIds.add(server.getId())) {
                    addViolation(
                            problems,
                            BuilderProblem.Severity.WARNING,
                            "servers.server.id",
                            null,
                            "must be unique but found duplicate server with id " + server.getId());
                }
            }
        }

        List<Mirror> mirrors = settings.getMirrors();

        if (mirrors != null) {
            for (Mirror mirror : mirrors) {
                validateStringNotEmpty(problems, "mirrors.mirror.id", mirror.getId(), mirror.getUrl());

                validateBannedCharacters(
                        problems,
                        "mirrors.mirror.id",
                        BuilderProblem.Severity.WARNING,
                        mirror.getId(),
                        null,
                        ILLEGAL_REPO_ID_CHARS);

                if ("local".equals(mirror.getId())) {
                    addViolation(
                            problems,
                            BuilderProblem.Severity.WARNING,
                            "mirrors.mirror.id",
                            null,
                            "must not be 'local'"
                                    + ", this identifier is reserved for the local repository"
                                    + ", using it for other repositories will corrupt your repository metadata.");
                }

                validateStringNotEmpty(problems, "mirrors.mirror.url", mirror.getUrl(), mirror.getId());

                validateStringNotEmpty(problems, "mirrors.mirror.mirrorOf", mirror.getMirrorOf(), mirror.getId());
            }
        }

        List<Profile> profiles = settings.getProfiles();

        if (profiles != null) {
            Set<String> profileIds = new HashSet<>();

            for (Profile profile : profiles) {
                if (!profileIds.add(profile.getId())) {
                    addViolation(
                            problems,
                            BuilderProblem.Severity.WARNING,
                            "profiles.profile.id",
                            null,
                            "must be unique but found duplicate profile with id " + profile.getId());
                }

                String prefix = "profiles.profile[" + profile.getId() + "].";

                validateRepositories(problems, profile.getRepositories(), prefix + "repositories.repository");
                validateRepositories(
                        problems, profile.getPluginRepositories(), prefix + "pluginRepositories.pluginRepository");
            }
        }

        List<Proxy> proxies = settings.getProxies();

        if (proxies != null) {
            Set<String> proxyIds = new HashSet<>();

            for (Proxy proxy : proxies) {
                if (!proxyIds.add(proxy.getId())) {
                    addViolation(
                            problems,
                            BuilderProblem.Severity.WARNING,
                            "proxies.proxy.id",
                            null,
                            "must be unique but found duplicate proxy with id " + proxy.getId());
                }
                validateStringNotEmpty(problems, "proxies.proxy.host", proxy.getHost(), proxy.getId());

                try {
                    Integer.parseInt(proxy.getPortString());
                } catch (NumberFormatException e) {
                    addViolation(
                            problems,
                            BuilderProblem.Severity.ERROR,
                            "proxies.proxy[" + proxy.getId() + "].port",
                            null,
                            "must be a valid integer but found '" + proxy.getPortString() + "'");
                }
            }
        }
    }

    private void validateRepositories(List<BuilderProblem> problems, List<Repository> repositories, String prefix) {
        Set<String> repoIds = new HashSet<>();

        for (Repository repository : repositories) {
            validateStringNotEmpty(problems, prefix + ".id", repository.getId(), repository.getUrl());

            validateBannedCharacters(
                    problems,
                    prefix + ".id",
                    BuilderProblem.Severity.WARNING,
                    repository.getId(),
                    null,
                    ILLEGAL_REPO_ID_CHARS);

            if ("local".equals(repository.getId())) {
                addViolation(
                        problems,
                        BuilderProblem.Severity.WARNING,
                        prefix + ".id",
                        null,
                        "must not be 'local'"
                                + ", this identifier is reserved for the local repository"
                                + ", using it for other repositories will corrupt your repository metadata.");
            }

            if (!repoIds.add(repository.getId())) {
                addViolation(
                        problems,
                        BuilderProblem.Severity.WARNING,
                        prefix + ".id",
                        null,
                        "must be unique but found duplicate repository with id " + repository.getId());
            }

            validateStringNotEmpty(problems, prefix + ".url", repository.getUrl(), repository.getId());

            if ("legacy".equals(repository.getLayout())) {
                addViolation(
                        problems,
                        BuilderProblem.Severity.WARNING,
                        prefix + ".layout",
                        repository.getId(),
                        "uses the unsupported value 'legacy', artifact resolution might fail.");
            }
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length == null</code>
     * <li><code>string.length == 0</code>
     * </ul>
     */
    private static boolean validateStringEmpty(
            List<BuilderProblem> problems, String fieldName, String string, String message) {
        if (string == null || string.length() == 0) {
            return true;
        }

        addViolation(problems, BuilderProblem.Severity.WARNING, fieldName, null, message);

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string.length != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    private static boolean validateStringNotEmpty(
            List<BuilderProblem> problems, String fieldName, String string, String sourceHint) {
        if (!validateNotNull(problems, fieldName, string, sourceHint)) {
            return false;
        }

        if (!string.isEmpty()) {
            return true;
        }

        addViolation(problems, BuilderProblem.Severity.ERROR, fieldName, sourceHint, "is missing");

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    private static boolean validateNotNull(
            List<BuilderProblem> problems, String fieldName, Object object, String sourceHint) {
        if (object != null) {
            return true;
        }

        addViolation(problems, BuilderProblem.Severity.ERROR, fieldName, sourceHint, "is missing");

        return false;
    }

    private static boolean validateBannedCharacters(
            List<BuilderProblem> problems,
            String fieldName,
            BuilderProblem.Severity severity,
            String string,
            String sourceHint,
            String banned) {
        if (string != null) {
            for (int i = string.length() - 1; i >= 0; i--) {
                if (banned.indexOf(string.charAt(i)) >= 0) {
                    addViolation(
                            problems,
                            severity,
                            fieldName,
                            sourceHint,
                            "must not contain any of these characters " + banned + " but found " + string.charAt(i));
                    return false;
                }
            }
        }

        return true;
    }

    private static void addViolation(
            List<BuilderProblem> problems,
            BuilderProblem.Severity severity,
            String fieldName,
            String sourceHint,
            String message) {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append('\'').append(fieldName).append('\'');

        if (sourceHint != null) {
            buffer.append(" for ").append(sourceHint);
        }

        buffer.append(' ').append(message);

        problems.add(new DefaultBuilderProblem(null, -1, -1, null, buffer.toString(), severity));
    }
}
