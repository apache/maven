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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultTransformerContext.GAKey;
import org.apache.maven.model.building.DefaultTransformerContext.Holder;

/**
 * Builds up the transformer context.
 * After the buildplan is ready, the build()-method returns the immutable context useful during distribution.
 * This is an inner class, as it must be able to call readRawModel()
 *
 * @since 4.0.0
 */
class DefaultTransformerContextBuilder implements TransformerContextBuilder {
    private final Graph dag = new Graph();
    private final DefaultModelBuilder defaultModelBuilder;
    private final DefaultTransformerContext context;

    private final Map<String, Set<FileModelSource>> mappedSources = new ConcurrentHashMap<>(64);

    private volatile boolean fullReactorLoaded;

    DefaultTransformerContextBuilder(DefaultModelBuilder defaultModelBuilder) {
        this.defaultModelBuilder = defaultModelBuilder;
        this.context = new DefaultTransformerContext(defaultModelBuilder.getModelProcessor());
    }

    /**
     * If an interface could be extracted, DefaultModelProblemCollector should be ModelProblemCollectorExt
     */
    @Override
    public TransformerContext initialize(ModelBuildingRequest request, ModelProblemCollector collector) {
        // We must assume the TransformerContext was created using this.newTransformerContextBuilder()
        DefaultModelProblemCollector problems = (DefaultModelProblemCollector) collector;
        return new TransformerContext() {

            @Override
            public Path locate(Path path) {
                return context.locate(path);
            }

            @Override
            public String getUserProperty(String key) {
                return context.userProperties.computeIfAbsent(
                        key, k -> request.getUserProperties().getProperty(key));
            }

            @Override
            public Model getRawModel(Path from, String gId, String aId) {
                Model model = findRawModel(from, gId, aId);
                if (model != null) {
                    context.modelByGA.put(new GAKey(gId, aId), new Holder(model));
                    context.modelByPath.put(model.getPomFile().toPath(), new Holder(model));
                }
                return model;
            }

            @Override
            public Model getRawModel(Path from, Path path) {
                Model model = findRawModel(from, path);
                if (model != null) {
                    String groupId = defaultModelBuilder.getGroupId(model);
                    context.modelByGA.put(
                            new DefaultTransformerContext.GAKey(groupId, model.getArtifactId()), new Holder(model));
                    context.modelByPath.put(path, new Holder(model));
                }
                return model;
            }

            private Model findRawModel(Path from, String groupId, String artifactId) {
                FileModelSource source = getSource(groupId, artifactId);
                if (source == null) {
                    // we need to check the whole reactor in case it's a dependency
                    loadFullReactor();
                    source = getSource(groupId, artifactId);
                }
                if (source != null) {
                    if (!addEdge(from, source.getFile().toPath(), problems)) {
                        return null;
                    }
                    try {
                        ModelBuildingRequest gaBuildingRequest =
                                new DefaultModelBuildingRequest(request).setModelSource(source);
                        return defaultModelBuilder.readRawModel(gaBuildingRequest, problems);
                    } catch (ModelBuildingException e) {
                        // gathered with problem collector
                    }
                }
                return null;
            }

            private void loadFullReactor() {
                if (!fullReactorLoaded) {
                    synchronized (DefaultTransformerContextBuilder.this) {
                        if (!fullReactorLoaded) {
                            doLoadFullReactor();
                            fullReactorLoaded = true;
                        }
                    }
                }
            }

            private void doLoadFullReactor() {
                Path rootDirectory = request.getRootDirectory();
                if (rootDirectory == null) {
                    return;
                }
                List<File> toLoad = new ArrayList<>();
                File root = defaultModelBuilder.getModelProcessor().locateExistingPom(rootDirectory.toFile());
                toLoad.add(root);
                while (!toLoad.isEmpty()) {
                    File pom = toLoad.remove(0);
                    try {
                        ModelBuildingRequest gaBuildingRequest =
                                new DefaultModelBuildingRequest(request).setModelSource(new FileModelSource(pom));
                        Model rawModel = defaultModelBuilder.readFileModel(gaBuildingRequest, problems);
                        for (String module : rawModel.getModules()) {
                            File moduleFile = defaultModelBuilder
                                    .getModelProcessor()
                                    .locateExistingPom(new File(pom.getParent(), module));
                            if (moduleFile != null) {
                                toLoad.add(moduleFile);
                            }
                        }
                    } catch (ModelBuildingException e) {
                        // gathered with problem collector
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

                DefaultModelBuildingRequest req = new DefaultModelBuildingRequest(request)
                        .setPomFile(p.toFile())
                        .setModelSource(new FileModelSource(p.toFile()));

                try {
                    return defaultModelBuilder.readRawModel(req, problems);
                } catch (ModelBuildingException e) {
                    // gathered with problem collector
                }
                return null;
            }
        };
    }

    private boolean addEdge(Path from, Path p, DefaultModelProblemCollector problems) {
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

    @Override
    public TransformerContext build() {
        return context;
    }

    public FileModelSource getSource(String groupId, String artifactId) {
        Set<FileModelSource> sources = mappedSources.get(groupId + ":" + artifactId);
        if (sources == null) {
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

    public void putSource(String groupId, String artifactId, FileModelSource source) {
        mappedSources
                .computeIfAbsent(groupId + ":" + artifactId, k -> new HashSet<>())
                .add(source);
    }
}
