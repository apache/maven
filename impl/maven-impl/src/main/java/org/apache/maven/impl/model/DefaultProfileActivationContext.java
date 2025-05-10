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
package org.apache.maven.impl.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.model.PathTranslator;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.RootLocator;

/**
 * Describes the environmental context used to determine the activation status of profiles.
 */
public class DefaultProfileActivationContext implements ProfileActivationContext {

    record ExistRequest(String path, boolean enableGlob) {}

    enum ModelInfo {
        ArtifactId,
        Packaging,
        BaseDirectory,
        RootDirectory
    }

    /**
     * This class keeps track of information that are used during profile activation.
     * This allows to cache the activated parent and check if the result of the
     * activation will be the same by verifying that the used keys are the same.
     */
    static class Record {
        private final Map<String, Boolean> usedActiveProfiles = new HashMap<>();
        private final Map<String, Boolean> usedInactiveProfiles = new HashMap<>();
        private final Map<String, String> usedSystemProperties = new HashMap<>();
        private final Map<String, String> usedUserProperties = new HashMap<>();
        private final Map<String, String> usedModelProperties = new HashMap<>();
        private final Map<ModelInfo, String> usedModelInfos = new HashMap<>();
        private final Map<ExistRequest, Boolean> usedExists = new HashMap<>();

        @Override
        public boolean equals(Object o) {
            if (o instanceof Record record) {
                return Objects.equals(usedActiveProfiles, record.usedActiveProfiles)
                        && Objects.equals(usedInactiveProfiles, record.usedInactiveProfiles)
                        && Objects.equals(usedSystemProperties, record.usedSystemProperties)
                        && Objects.equals(usedUserProperties, record.usedUserProperties)
                        && Objects.equals(usedModelProperties, record.usedModelProperties)
                        && Objects.equals(usedModelInfos, record.usedModelInfos)
                        && Objects.equals(usedExists, record.usedExists);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    usedActiveProfiles,
                    usedInactiveProfiles,
                    usedSystemProperties,
                    usedUserProperties,
                    usedModelProperties,
                    usedModelInfos,
                    usedExists);
        }

        boolean matches(DefaultProfileActivationContext context) {
            return matchesProfiles(usedActiveProfiles, context.activeProfileIds)
                    && matchesProfiles(usedInactiveProfiles, context.inactiveProfileIds)
                    && matchesProperties(usedSystemProperties, context.systemProperties)
                    && matchesProperties(usedUserProperties, context.userProperties)
                    && matchesProperties(usedModelProperties, context.model.getProperties())
                    && matchesModelInfos(usedModelInfos, context)
                    && matchesExists(usedExists, context);
        }

        private boolean matchesProfiles(Map<String, Boolean> expected, List<String> actual) {
            return expected.entrySet().stream()
                    .allMatch(e -> Objects.equals(e.getValue(), actual.contains(e.getKey())));
        }

        private boolean matchesProperties(Map<String, String> expected, Map<String, String> actual) {
            return expected.entrySet().stream().allMatch(e -> Objects.equals(e.getValue(), actual.get(e.getKey())));
        }

        private boolean matchesModelInfos(Map<ModelInfo, String> infos, DefaultProfileActivationContext context) {
            return infos.entrySet().stream()
                    .allMatch(e -> Objects.equals(e.getValue(), getModelValue(e.getKey(), context)));
        }

        private String getModelValue(ModelInfo key, DefaultProfileActivationContext context) {
            return switch (key) {
                case ArtifactId -> context.model.getArtifactId();
                case Packaging -> context.model.getPackaging();
                case BaseDirectory -> context.doGetModelBaseDirectory();
                case RootDirectory -> context.doGetModelRootDirectory();
            };
        }

        private boolean matchesExists(Map<ExistRequest, Boolean> exists, DefaultProfileActivationContext context) {
            return exists.entrySet().stream()
                    .allMatch(e -> Objects.equals(
                            e.getValue(),
                            context.doExists(e.getKey().path(), e.getKey().enableGlob())));
        }
    }

    private final PathTranslator pathTranslator;
    private final RootLocator rootLocator;
    private final Interpolator interpolator;

    private List<String> activeProfileIds = Collections.emptyList();
    private List<String> inactiveProfileIds = Collections.emptyList();
    private Map<String, String> systemProperties = Collections.emptyMap();
    private Map<String, String> userProperties = Collections.emptyMap();
    private Model model;

    private final ThreadLocal<Record> records = new ThreadLocal<>();

    public DefaultProfileActivationContext(
            PathTranslator pathTranslator, RootLocator rootLocator, Interpolator interpolator) {
        this.pathTranslator = pathTranslator;
        this.rootLocator = rootLocator;
        this.interpolator = interpolator;
    }

    Record start() {
        Record record = records.get();
        records.set(new Record());
        return record;
    }

    Record stop(Record previous) {
        Record record = records.get();
        records.set(previous);
        // only keep keys for which the value is `true`
        record.usedActiveProfiles.values().removeIf(value -> !value);
        record.usedInactiveProfiles.values().removeIf(value -> !value);
        return record;
    }

    @Override
    public boolean isProfileActive(String profileId) {
        Record record = records.get();
        if (record != null) {
            return record.usedActiveProfiles.computeIfAbsent(profileId, activeProfileIds::contains);
        } else {
            return activeProfileIds.contains(profileId);
        }
    }

    /**
     * Sets the identifiers of those profiles that should be activated by explicit demand.
     *
     * @param activeProfileIds The identifiers of those profiles to activate, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setActiveProfileIds(List<String> activeProfileIds) {
        this.activeProfileIds = unmodifiable(activeProfileIds);
        return this;
    }

    @Override
    public boolean isProfileInactive(String profileId) {
        Record record = records.get();
        if (record != null) {
            return record.usedInactiveProfiles.computeIfAbsent(profileId, inactiveProfileIds::contains);
        } else {
            return inactiveProfileIds.contains(profileId);
        }
    }

    /**
     * Sets the identifiers of those profiles that should be deactivated by explicit demand.
     *
     * @param inactiveProfileIds The identifiers of those profiles to deactivate, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setInactiveProfileIds(List<String> inactiveProfileIds) {
        this.inactiveProfileIds = unmodifiable(inactiveProfileIds);
        return this;
    }

    @Override
    public String getSystemProperty(String key) {
        Record record = records.get();
        if (record != null) {
            return record.usedSystemProperties.computeIfAbsent(key, systemProperties::get);
        } else {
            return systemProperties.get(key);
        }
    }

    /**
     * Sets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @param systemProperties The system properties, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setSystemProperties(Map<String, String> systemProperties) {
        this.systemProperties = unmodifiable(systemProperties);
        return this;
    }

    @Override
    public String getUserProperty(String key) {
        Record record = records.get();
        if (record != null) {
            return record.usedUserProperties.computeIfAbsent(key, userProperties::get);
        } else {
            return userProperties.get(key);
        }
    }

    /**
     * Sets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @param userProperties The user properties, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setUserProperties(Map<String, String> userProperties) {
        this.userProperties = unmodifiable(userProperties);
        return this;
    }

    @Override
    public String getModelArtifactId() {
        Record record = records.get();
        if (record != null) {
            return record.usedModelInfos.computeIfAbsent(ModelInfo.ArtifactId, k -> model.getArtifactId());
        } else {
            return model.getArtifactId();
        }
    }

    @Override
    public String getModelPackaging() {
        Record record = records.get();
        if (record != null) {
            return record.usedModelInfos.computeIfAbsent(ModelInfo.Packaging, k -> model.getPackaging());
        } else {
            return model.getPackaging();
        }
    }

    @Override
    public String getModelProperty(String key) {
        Record record = records.get();
        if (record != null) {
            return record.usedModelProperties.computeIfAbsent(
                    key, k -> model.getProperties().get(k));
        } else {
            return model.getProperties().get(key);
        }
    }

    @Override
    public String getModelBaseDirectory() {
        Record record = records.get();
        if (record != null) {
            return record.usedModelInfos.computeIfAbsent(ModelInfo.BaseDirectory, k -> doGetModelBaseDirectory());
        } else {
            return doGetModelBaseDirectory();
        }
    }

    private String doGetModelBaseDirectory() {
        Path basedir = model.getProjectDirectory();
        return basedir != null ? basedir.toAbsolutePath().toString() : null;
    }

    @Override
    public String getModelRootDirectory() {
        Record record = records.get();
        if (record != null) {
            return record.usedModelInfos.computeIfAbsent(ModelInfo.RootDirectory, k -> doGetModelRootDirectory());
        } else {
            return doGetModelRootDirectory();
        }
    }

    private String doGetModelRootDirectory() {
        Path basedir = model != null ? model.getProjectDirectory() : null;
        Path rootdir = rootLocator != null ? rootLocator.findRoot(basedir) : null;
        return rootdir != null ? rootdir.toAbsolutePath().toString() : null;
    }

    public DefaultProfileActivationContext setModel(Model model) {
        this.model = model;
        return this;
    }

    @Override
    public String interpolatePath(String path) throws InterpolatorException {
        if (path == null) {
            return null;
        }
        String absolutePath = interpolator.interpolate(path, s -> {
            if ("basedir".equals(s) || "project.basedir".equals(s)) {
                return getModelBaseDirectory();
            }
            if ("project.rootDirectory".equals(s)) {
                return getModelRootDirectory();
            }
            String r = getModelProperty(s);
            if (r == null) {
                r = getUserProperty(s);
            }
            if (r == null) {
                r = getSystemProperty(s);
            }
            return r;
        });
        return pathTranslator.alignToBaseDirectory(absolutePath, model.getProjectDirectory());
    }

    @Override
    public boolean exists(String path, boolean enableGlob) throws ModelBuilderException {
        Record record = records.get();
        if (record != null) {
            return record.usedExists.computeIfAbsent(
                    new ExistRequest(path, enableGlob), r -> doExists(r.path, r.enableGlob));
        } else {
            return doExists(path, enableGlob);
        }
    }

    private boolean doExists(String path, boolean enableGlob) throws ModelBuilderException {
        String pattern = interpolatePath(path);
        String fixed, glob;
        if (enableGlob) {
            int asteriskIndex = pattern.indexOf('*');
            int questionMarkIndex = pattern.indexOf('?');
            int firstWildcardIndex = questionMarkIndex < 0
                    ? asteriskIndex
                    : asteriskIndex < 0 ? questionMarkIndex : Math.min(asteriskIndex, questionMarkIndex);
            if (firstWildcardIndex < 0) {
                fixed = pattern;
                glob = "";
            } else {
                int lastSep = pattern.substring(0, firstWildcardIndex).lastIndexOf(File.separatorChar);
                if (lastSep < 0) {
                    fixed = "";
                    glob = pattern;
                } else {
                    fixed = pattern.substring(0, lastSep);
                    glob = pattern.substring(lastSep + 1);
                }
            }
        } else {
            fixed = pattern;
            glob = "";
        }
        Path fixedPath = Path.of(fixed);
        return doExists(fixedPath, glob);
    }

    private static Boolean doExists(Path fixedPath, String glob) {
        if (fixedPath == null || !Files.exists(fixedPath)) {
            return false;
        }
        if (glob != null && !glob.isEmpty()) {
            try {
                PathMatcher matcher = fixedPath.getFileSystem().getPathMatcher("glob:" + glob);
                AtomicBoolean found = new AtomicBoolean(false);
                Files.walkFileTree(fixedPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (found.get() || matcher.matches(fixedPath.relativize(file))) {
                            found.set(true);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                return found.get();
            } catch (IOException e) {
                throw new ProjectBuilderException(
                        "Unable to verify file existence for '" + glob + "' inside '" + fixedPath + "'", e);
            }
        }
        return true;
    }

    private static List<String> unmodifiable(List<String> list) {
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    private static Map<String, String> unmodifiable(Map<String, String> map) {
        return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }
}
