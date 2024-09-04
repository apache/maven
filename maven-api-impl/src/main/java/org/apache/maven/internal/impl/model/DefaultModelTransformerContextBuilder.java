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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ModelTransformerContext;
import org.apache.maven.api.services.ModelTransformerContextBuilder;
import org.apache.maven.internal.impl.model.DefaultModelTransformerContext.GAKey;
import org.apache.maven.internal.impl.model.DefaultModelTransformerContext.Holder;

/**
 * Builds up the transformer context.
 * After the buildplan is ready, the build()-method returns the immutable context useful during distribution.
 * This is an inner class, as it must be able to call readRawModel()
 *
 * @since 4.0.0
 */
class DefaultModelTransformerContextBuilder implements ModelTransformerContextBuilder {
    private final Graph dag = new Graph();
    private final DefaultModelBuilder defaultModelBuilder;
    private final DefaultModelTransformerContext context;
    private volatile boolean fullReactorLoaded;

    DefaultModelTransformerContextBuilder(DefaultModelBuilder defaultModelBuilder) {
        this.defaultModelBuilder = defaultModelBuilder;
        this.context = new DefaultModelTransformerContext(defaultModelBuilder.getModelProcessor());
    }

    /**
     * If an interface could be extracted, DefaultModelProblemCollector should be ModelProblemCollectorExt
     */
    @Override
    public ModelTransformerContext initialize(ModelBuilderRequest request, ModelProblemCollector problems) {
        // We must assume the TransformerContext was created using this.newTransformerContextBuilder()
        return new ModelTransformerContext() {

            @Override
            public Path locate(Path path) {
                return context.locate(path);
            }

            @Override
            public String getUserProperty(String key) {
                return context.userProperties.computeIfAbsent(
                        key, k -> request.getUserProperties().get(key));
            }

            @Override
            public Model getRawModel(Path from, String gId, String aId) {
                Model model = findRawModel(from, gId, aId);
                if (model != null) {
                    String groupId = DefaultModelBuilder.getGroupId(model);
                    context.modelByGA.put(new GAKey(groupId, model.getArtifactId()), new Holder(model));
                    context.modelByPath.put(model.getPomFile(), new Holder(model));
                }
                return model;
            }

            @Override
            public Model getRawModel(Path from, Path path) {
                Model model = findRawModel(from, path);
                if (model != null) {
                    String groupId = DefaultModelBuilder.getGroupId(model);
                    context.modelByGA.put(
                            new DefaultModelTransformerContext.GAKey(groupId, model.getArtifactId()),
                            new Holder(model));
                    context.modelByPath.put(path, new Holder(model));
                }
                return model;
            }

            private Model findRawModel(Path from, String groupId, String artifactId) {
                ModelSource source = getSource(groupId, artifactId);
                if (source == null) {
                    // we need to check the whole reactor in case it's a dependency
                    loadFullReactor();
                    source = getSource(groupId, artifactId);
                }
                if (source != null) {
                    if (!addEdge(from, source.getPath(), problems)) {
                        return null;
                    }
                    try {
                        ModelBuilderRequest gaBuildingRequest = ModelBuilderRequest.build(request, source);
                        return defaultModelBuilder.readRawModel(gaBuildingRequest, problems);
                    } catch (ModelBuilderException e) {
                        // gathered with problem collector
                    }
                }
                return null;
            }

            private void loadFullReactor() {
                if (!fullReactorLoaded) {
                    synchronized (DefaultModelTransformerContextBuilder.this) {
                        if (!fullReactorLoaded) {
                            doLoadFullReactor();
                            fullReactorLoaded = true;
                        }
                    }
                }
            }

            private void doLoadFullReactor() {
                Path rootDirectory;
                try {
                    rootDirectory = request.getSession().getRootDirectory();
                } catch (IllegalStateException e) {
                    // if no root directory, bail out
                    return;
                }
                List<Path> toLoad = new ArrayList<>();
                Path root = defaultModelBuilder.getModelProcessor().locateExistingPom(rootDirectory);
                toLoad.add(root);
                while (!toLoad.isEmpty()) {
                    Path pom = toLoad.remove(0);
                    try {
                        ModelBuilderRequest gaBuildingRequest =
                                ModelBuilderRequest.build(request, ModelSource.fromPath(pom));
                        Model rawModel = defaultModelBuilder.readFileModel(gaBuildingRequest, problems);
                        List<String> subprojects = rawModel.getSubprojects();
                        if (subprojects.isEmpty()) {
                            subprojects = rawModel.getModules();
                        }
                        for (String subproject : subprojects) {
                            Path subprojectFile = defaultModelBuilder
                                    .getModelProcessor()
                                    .locateExistingPom(pom.getParent().resolve(subproject));
                            if (subprojectFile != null) {
                                toLoad.add(subprojectFile);
                            }
                        }
                    } catch (ModelBuilderException e) {
                        // gathered with problem collector
                        problems.add(
                                ModelProblem.Severity.ERROR,
                                ModelProblem.Version.V40,
                                "Failed to load project " + pom,
                                e);
                    }
                }
            }

            private Model findRawModel(Path from, Path p) {
                if (!Files.isRegularFile(p)) {
                    throw new IllegalArgumentException("Not a regular file: " + p);
                }

                if (!addEdge(from, p, problems)) {
                    return null;
                }

                ModelBuilderRequest req = ModelBuilderRequest.build(request, ModelSource.fromPath(p));

                try {
                    return defaultModelBuilder.readRawModel(req, problems);
                } catch (ModelBuilderException e) {
                    // gathered with problem collector
                }
                return null;
            }
        };
    }

    private boolean addEdge(Path from, Path p, ModelProblemCollector problems) {
        try {
            dag.addEdge(from.toString(), p.toString());
            return true;
        } catch (Graph.CycleDetectedException e) {
            problems.add(new DefaultModelProblem(
                    "Cycle detected between models at " + from + " and " + p,
                    ModelProblem.Severity.FATAL,
                    null,
                    null,
                    0,
                    0,
                    null,
                    e));
            return false;
        }
    }

    public ModelSource getSource(String groupId, String artifactId) {
        Set<ModelSource> sources;
        if (groupId != null) {
            sources = context.mappedSources.get(new GAKey(groupId, artifactId));
            if (sources == null) {
                return null;
            }
        } else if (artifactId != null) {
            sources = context.mappedSources.get(new GAKey(null, artifactId));
            if (sources == null) {
                return null;
            }
        } else {
            return null;
        }
        return sources.stream()
                .reduce((a, b) -> {
                    throw new IllegalStateException(String.format(
                            "No unique Source for %s:%s: %s and %s",
                            groupId, artifactId, a.getLocation(), b.getLocation()));
                })
                .orElse(null);
    }

    public void putSource(String groupId, String artifactId, ModelSource source) {
        context.mappedSources
                .computeIfAbsent(new GAKey(groupId, artifactId), k -> new HashSet<>())
                .add(source);
        context.mappedSources
                .computeIfAbsent(new GAKey(null, artifactId), k -> new HashSet<>())
                .add(source);
    }
}
