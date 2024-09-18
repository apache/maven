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
package org.apache.maven.internal.impl.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ModelTransformer;
import org.apache.maven.api.services.ModelTransformerContext;

/**
 * ModelSourceTransformer for the build pom
 *
 * @since 4.0.0
 */
@Named
@Singleton
public class BuildModelTransformer implements ModelTransformer {

    @Override
    public Model transform(ModelTransformerContext context, Model model, Path path) {
        Model.Builder builder = Model.newBuilder(model);
        handleParent(context, model, path, builder);
        handleReactorDependencies(context, model, path, builder);
        handleCiFriendlyVersion(context, model, path, builder);
        return builder.build();
    }

    //
    // Infer parent information
    //
    void handleParent(ModelTransformerContext context, Model model, Path pomFile, Model.Builder builder) {
        Parent parent = model.getParent();
        if (parent != null) {
            String version = parent.getVersion();

            // CI Friendly version for parent
            String modVersion = replaceCiFriendlyVersion(context, version);

            // Update parent
            builder.parent(parent.with().version(modVersion).build());
        }
    }

    //
    // CI friendly versions
    //
    void handleCiFriendlyVersion(ModelTransformerContext context, Model model, Path pomFile, Model.Builder builder) {
        String version = model.getVersion();
        String modVersion = replaceCiFriendlyVersion(context, version);
        builder.version(modVersion);
    }

    //
    // Infer inner reactor dependencies version
    //
    void handleReactorDependencies(ModelTransformerContext context, Model model, Path pomFile, Model.Builder builder) {
        List<Dependency> newDeps = new ArrayList<>();
        boolean modified = false;
        for (Dependency dep : model.getDependencies()) {
            Dependency.Builder depBuilder = null;
            if (dep.getVersion() == null) {
                Model depModel = context.getRawModel(model.getPomFile(), dep.getGroupId(), dep.getArtifactId());
                if (depModel != null) {
                    String version = depModel.getVersion();
                    InputLocation versionLocation = depModel.getLocation("version");
                    if (version == null && depModel.getParent() != null) {
                        version = depModel.getParent().getVersion();
                        versionLocation = depModel.getParent().getLocation("version");
                    }
                    depBuilder = Dependency.newBuilder(dep);
                    depBuilder.version(version).location("version", versionLocation);
                    if (dep.getGroupId() == null) {
                        String depGroupId = depModel.getGroupId();
                        InputLocation groupIdLocation = depModel.getLocation("groupId");
                        if (depGroupId == null && depModel.getParent() != null) {
                            depGroupId = depModel.getParent().getGroupId();
                            groupIdLocation = depModel.getParent().getLocation("groupId");
                        }
                        depBuilder.groupId(depGroupId).location("groupId", groupIdLocation);
                    }
                }
            }
            if (depBuilder != null) {
                newDeps.add(depBuilder.build());
                modified = true;
            } else {
                newDeps.add(dep);
            }
        }
        if (modified) {
            builder.dependencies(newDeps);
        }
    }

    protected String replaceCiFriendlyVersion(ModelTransformerContext context, String version) {
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
            Path pomFile, ModelTransformerContext context, Path relativePath, String groupId, String artifactId) {
        Path pomPath = pomFile.resolveSibling(relativePath).normalize();
        if (Files.isDirectory(pomPath)) {
            pomPath = context.locate(pomPath);
        }

        if (pomPath == null || !Files.isRegularFile(pomPath)) {
            return Optional.empty();
        }

        return Optional.ofNullable(context.getRawModel(pomFile, pomPath.normalize()))
                .map(BuildModelTransformer::toRelativeProject);
    }

    private static RelativeProject toRelativeProject(final Model m) {
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

    protected static class RelativeProject {
        private final String groupId;

        private final String artifactId;

        private final String version;

        protected RelativeProject(String groupId, String artifactId, String version) {
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
