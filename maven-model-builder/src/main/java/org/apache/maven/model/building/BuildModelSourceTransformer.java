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
package org.apache.maven.model.building;

import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

/**
 * ModelSourceTransformer for the build pom
 *
 * @since 4.0.0
 */
@Named
@Singleton
class BuildModelSourceTransformer implements ModelSourceTransformer {

    public static final String NAMESPACE_PREFIX = "http://maven.apache.org/POM/";

    @Override
    public void transform(Path pomFile, TransformerContext context, Model model) {
        handleModelVersion(model);
        handleParent(pomFile, context, model);
        handleReactorDependencies(context, model);
        handleCiFriendlyVersion(context, model);
    }

    //
    // Infer modelVersion from namespace URI
    //
    void handleModelVersion(Model model) {
        String namespace = model.getDelegate().getNamespaceUri();
        if (model.getModelVersion() == null && namespace != null && namespace.startsWith(NAMESPACE_PREFIX)) {
            model.setModelVersion(namespace.substring(NAMESPACE_PREFIX.length()));
        }
    }

    //
    // Infer parent information
    //
    void handleParent(Path pomFile, TransformerContext context, Model model) {
        Parent parent = model.getParent();
        if (parent != null) {
            String version = parent.getVersion();
            String path = Optional.ofNullable(parent.getRelativePath()).orElse("..");
            if (version == null && !path.isEmpty()) {
                Optional<RelativeProject> resolvedParent = resolveRelativePath(
                        pomFile, context, Paths.get(path), parent.getGroupId(), parent.getArtifactId());
                resolvedParent.ifPresent(relativeProject -> parent.setVersion(relativeProject.getVersion()));
            }
        }
    }

    //
    // CI friendly versions
    //
    void handleCiFriendlyVersion(TransformerContext context, Model model) {
        String version = model.getVersion();
        String modVersion = replaceCiFriendlyVersion(context, version);
        model.setVersion(modVersion);

        Parent parent = model.getParent();
        if (parent != null) {
            version = parent.getVersion();
            modVersion = replaceCiFriendlyVersion(context, version);
            parent.setVersion(modVersion);
        }
    }

    //
    // Infer inner reactor dependencies version
    //
    void handleReactorDependencies(TransformerContext context, Model model) {
        for (Dependency dep : model.getDependencies()) {
            if (dep.getVersion() == null) {
                Model depModel =
                        context.getRawModel(model.getDelegate().getPomFile(), dep.getGroupId(), dep.getArtifactId());
                if (depModel != null) {
                    String v = depModel.getVersion();
                    if (v == null && depModel.getParent() != null) {
                        v = depModel.getParent().getVersion();
                    }
                    dep.setVersion(v);
                }
            }
        }
    }

    protected String replaceCiFriendlyVersion(TransformerContext context, String version) {
        if (version != null) {
            for (String key : Arrays.asList("changelist", "revision", "sha1")) {
                String val = context.getUserProperty(key);
                if (val != null) {
                    version = version.replace("${" + key + "}", val);
                }
            }
        }
        return version;
    }

    protected Optional<RelativeProject> resolveRelativePath(
            Path pomFile, TransformerContext context, Path relativePath, String groupId, String artifactId) {
        Path pomPath = pomFile.resolveSibling(relativePath).normalize();
        if (Files.isDirectory(pomPath)) {
            pomPath = context.locate(pomPath);
        }

        if (pomPath == null || !Files.isRegularFile(pomPath)) {
            return Optional.empty();
        }

        Optional<RelativeProject> mappedProject = Optional.ofNullable(context.getRawModel(pomFile, pomPath.normalize()))
                .map(BuildModelSourceTransformer::toRelativeProject);

        if (mappedProject.isPresent()) {
            RelativeProject project = mappedProject.get();

            if (Objects.equals(groupId, project.getGroupId()) && Objects.equals(artifactId, project.getArtifactId())) {
                return mappedProject;
            }
        }
        return Optional.empty();
    }

    private static RelativeProject toRelativeProject(final org.apache.maven.model.Model m) {
        String groupId = m.getGroupId();
        if (groupId == null && m.getParent() != null) {
            groupId = m.getParent().getGroupId();
        }

        String version = m.getVersion();
        if (version == null && m.getParent() != null) {
            version = m.getParent().getVersion();
        }

        return new RelativeProject(groupId, m.getArtifactId(), version);
    }

    static class RelativeProject {
        private final String groupId;

        private final String artifactId;

        private final String version;

        RelativeProject(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }
    }
}
