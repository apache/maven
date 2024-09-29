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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;

/**
 * Maven G level metadata.
 *
 * @deprecated since 4.0.0, use {@code maven-api-impl} jar instead
 */
@Deprecated(since = "4.0.0")
final class PluginsMetadata extends MavenMetadata {
    static final class PluginInfo {
        @Nonnull
        final String groupId;

        @Nonnull
        private final String artifactId;

        @Nullable
        private final String goalPrefix;

        @Nullable
        private final String name;

        PluginInfo(String groupId, String artifactId, String goalPrefix, String name) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.goalPrefix = goalPrefix;
            this.name = name;
        }
    }

    private final PluginInfo pluginInfo;

    PluginsMetadata(PluginInfo pluginInfo, Date timestamp) {
        super(createRepositoryMetadata(pluginInfo), (Path) null, timestamp);
        this.pluginInfo = pluginInfo;
    }

    PluginsMetadata(PluginInfo pluginInfo, Path path, Date timestamp) {
        super(createRepositoryMetadata(pluginInfo), path, timestamp);
        this.pluginInfo = pluginInfo;
    }

    private static Metadata createRepositoryMetadata(PluginInfo pluginInfo) {
        Metadata result = new Metadata();
        Plugin plugin = new Plugin();
        plugin.setPrefix(pluginInfo.goalPrefix);
        plugin.setArtifactId(pluginInfo.artifactId);
        plugin.setName(pluginInfo.name);
        result.getPlugins().add(plugin);
        return result;
    }

    @Override
    protected void merge(Metadata recessive) {
        List<Plugin> recessivePlugins = recessive.getPlugins();
        List<Plugin> plugins = metadata.getPlugins();
        if (!recessivePlugins.isEmpty() || !plugins.isEmpty()) {
            LinkedHashMap<String, Plugin> mergedPlugins = new LinkedHashMap<>();
            recessivePlugins.forEach(p -> mergedPlugins.put(p.getPrefix(), p));
            plugins.forEach(p -> mergedPlugins.put(p.getPrefix(), p));
            metadata.setPlugins(new ArrayList<>(mergedPlugins.values()));
        }

        // just carry-on as-is
        if (recessive.getVersioning() != null) {
            metadata.setVersioning(recessive.getVersioning());
        }
    }

    @Deprecated
    @Override
    public MavenMetadata setFile(File file) {
        return new PluginsMetadata(pluginInfo, file.toPath(), timestamp);
    }

    @Override
    public MavenMetadata setPath(Path path) {
        return new PluginsMetadata(pluginInfo, path, timestamp);
    }

    @Override
    public String getGroupId() {
        return pluginInfo.groupId;
    }

    @Override
    public String getArtifactId() {
        return "";
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public Nature getNature() {
        return Nature.RELEASE_OR_SNAPSHOT;
    }
}
