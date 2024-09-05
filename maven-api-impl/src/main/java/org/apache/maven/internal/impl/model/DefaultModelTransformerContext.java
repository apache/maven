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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.ModelSource;

class DefaultModelTransformerContext {

    final DefaultModelBuilder modelBuilder;
    final ModelBuilderRequest request;
    final ModelProblemCollector problems;
    final Graph dag = new Graph();
    final Map<Path, Holder> modelByPath = new ConcurrentHashMap<>();
    final Map<GAKey, Holder> modelByGA = new ConcurrentHashMap<>();
    final Map<GAKey, Set<ModelSource>> mappedSources = new ConcurrentHashMap<>(64);
    volatile boolean fullReactorLoaded;

    static class Holder {
        private volatile boolean set;
        private volatile Model model;

        Holder(Model model) {
            this.model = Objects.requireNonNull(model);
            this.set = true;
        }

        public static Model deref(Holder holder) {
            return holder != null ? holder.get() : null;
        }

        public Model get() {
            if (!set) {
                synchronized (this) {
                    if (!set) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                }
            }
            return model;
        }

        public Model computeIfAbsent(Supplier<Model> supplier) {
            if (!set) {
                synchronized (this) {
                    if (!set) {
                        this.set = true;
                        this.model = supplier.get();
                        this.notifyAll();
                    }
                }
            }
            return model;
        }
    }

    DefaultModelTransformerContext(
            DefaultModelBuilder modelBuilder, ModelBuilderRequest request, ModelProblemCollector problems) {
        this.modelBuilder = modelBuilder;
        this.request = request;
        this.problems = problems;
    }

    public String getUserProperty(String key) {
        return request.getUserProperties().get(key);
    }

    public Model getRawModel(Path from, String gId, String aId) {
        Model model = findRawModel(from, gId, aId);
        if (model != null) {
            modelByGA.put(new GAKey(gId, aId), new Holder(model));
            if (model.getPomFile() != null) {
                modelByPath.put(model.getPomFile(), new Holder(model));
            }
        }
        return model;
    }

    public Model getRawModel(Path from, Path path) {
        Model model = findRawModel(from, path);
        if (model != null) {
            String groupId = DefaultModelBuilder.getGroupId(model);
            modelByGA.put(new GAKey(groupId, model.getArtifactId()), new Holder(model));
            modelByPath.put(path, new Holder(model));
        }
        return model;
    }

    public Path locate(Path path) {
        return modelBuilder.getModelProcessor().locateExistingPom(path);
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
                return modelBuilder.readRawModel(gaBuildingRequest, problems);
            } catch (ModelBuilderException e) {
                // gathered with problem collector
            }
        }
        return null;
    }

    private void loadFullReactor() {
        if (!fullReactorLoaded) {
            synchronized (this) {
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
        Path root = modelBuilder.getModelProcessor().locateExistingPom(rootDirectory);
        toLoad.add(root);
        while (!toLoad.isEmpty()) {
            Path pom = toLoad.remove(0);
            try {
                ModelBuilderRequest gaBuildingRequest = ModelBuilderRequest.build(request, ModelSource.fromPath(pom));
                Model rawModel = modelBuilder.readFileModel(gaBuildingRequest, problems);
                List<String> subprojects = rawModel.getSubprojects();
                if (subprojects == null) {
                    subprojects = rawModel.getModules();
                }
                for (String subproject : subprojects) {
                    Path subprojectFile = modelBuilder
                            .getModelProcessor()
                            .locateExistingPom(pom.getParent().resolve(subproject));
                    if (subprojectFile != null) {
                        toLoad.add(subprojectFile);
                    }
                }
            } catch (ModelBuilderException e) {
                // gathered with problem collector
                problems.add(ModelProblem.Severity.ERROR, ModelProblem.Version.V40, "Failed to load project " + pom, e);
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
            return modelBuilder.readRawModel(req, problems);
        } catch (ModelBuilderException e) {
            // gathered with problem collector
        }
        return null;
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
            sources = mappedSources.get(new GAKey(groupId, artifactId));
            if (sources == null) {
                return null;
            }
        } else if (artifactId != null) {
            sources = mappedSources.get(new GAKey(null, artifactId));
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
        mappedSources
                .computeIfAbsent(new GAKey(groupId, artifactId), k -> new HashSet<>())
                .add(source);
        mappedSources
                .computeIfAbsent(new GAKey(null, artifactId), k -> new HashSet<>())
                .add(source);
    }

    record GAKey(String groupId, String artifactId) {}
}
