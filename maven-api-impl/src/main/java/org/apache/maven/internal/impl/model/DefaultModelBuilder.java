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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Constants;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.services.model.DependencyManagementImporter;
import org.apache.maven.api.services.model.DependencyManagementInjector;
import org.apache.maven.api.services.model.InheritanceAssembler;
import org.apache.maven.api.services.model.ModelCache;
import org.apache.maven.api.services.model.ModelCacheFactory;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.ModelNormalizer;
import org.apache.maven.api.services.model.ModelPathTranslator;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.api.services.model.ModelUrlNormalizer;
import org.apache.maven.api.services.model.ModelValidator;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.api.services.model.PluginConfigurationExpander;
import org.apache.maven.api.services.model.PluginManagementInjector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileInjector;
import org.apache.maven.api.services.model.ProfileSelector;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.api.spi.ModelTransformer;
import org.apache.maven.internal.impl.util.PhasingExecutor;
import org.apache.maven.model.v4.MavenTransformer;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Named
@Singleton
public class DefaultModelBuilder implements ModelBuilder {

    public static final String NAMESPACE_PREFIX = "http://maven.apache.org/POM/";
    private static final String RAW = "raw";
    private static final String FILE = "file";
    private static final String IMPORT = "import";
    private static final String PARENT = "parent";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModelProcessor modelProcessor;
    private final ModelValidator modelValidator;
    private final ModelNormalizer modelNormalizer;
    private final ModelInterpolator modelInterpolator;
    private final ModelPathTranslator modelPathTranslator;
    private final ModelUrlNormalizer modelUrlNormalizer;
    private final SuperPomProvider superPomProvider;
    private final InheritanceAssembler inheritanceAssembler;
    private final ProfileSelector profileSelector;
    private final ProfileInjector profileInjector;
    private final PluginManagementInjector pluginManagementInjector;
    private final DependencyManagementInjector dependencyManagementInjector;
    private final DependencyManagementImporter dependencyManagementImporter;
    private final PluginConfigurationExpander pluginConfigurationExpander;
    private final ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;
    private final ModelVersionParser versionParser;
    private final List<ModelTransformer> transformers;
    private final ModelCacheFactory modelCacheFactory;
    private final ModelResolver modelResolver;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    public DefaultModelBuilder(
            ModelProcessor modelProcessor,
            ModelValidator modelValidator,
            ModelNormalizer modelNormalizer,
            ModelInterpolator modelInterpolator,
            ModelPathTranslator modelPathTranslator,
            ModelUrlNormalizer modelUrlNormalizer,
            SuperPomProvider superPomProvider,
            InheritanceAssembler inheritanceAssembler,
            ProfileSelector profileSelector,
            ProfileInjector profileInjector,
            PluginManagementInjector pluginManagementInjector,
            DependencyManagementInjector dependencyManagementInjector,
            DependencyManagementImporter dependencyManagementImporter,
            PluginConfigurationExpander pluginConfigurationExpander,
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator,
            ModelVersionParser versionParser,
            List<ModelTransformer> transformers,
            ModelCacheFactory modelCacheFactory,
            ModelResolver modelResolver) {
        this.modelProcessor = modelProcessor;
        this.modelValidator = modelValidator;
        this.modelNormalizer = modelNormalizer;
        this.modelInterpolator = modelInterpolator;
        this.modelPathTranslator = modelPathTranslator;
        this.modelUrlNormalizer = modelUrlNormalizer;
        this.superPomProvider = superPomProvider;
        this.inheritanceAssembler = inheritanceAssembler;
        this.profileSelector = profileSelector;
        this.profileInjector = profileInjector;
        this.pluginManagementInjector = pluginManagementInjector;
        this.dependencyManagementInjector = dependencyManagementInjector;
        this.dependencyManagementImporter = dependencyManagementImporter;
        this.pluginConfigurationExpander = pluginConfigurationExpander;
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        this.versionParser = versionParser;
        this.transformers = transformers;
        this.modelCacheFactory = modelCacheFactory;
        this.modelResolver = modelResolver;
    }

    public ModelBuilderSession newSession() {
        return new ModelBuilderSession() {
            DefaultModelBuilderSession mainSession;

            /**
             * Builds a model based on the provided ModelBuilderRequest.
             *
             * @param request The request containing the parameters for building the model.
             * @return The result of the model building process.
             * @throws ModelBuilderException If an error occurs during model building.
             */
            @Override
            public ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException {
                // Create or derive a session based on the request
                DefaultModelBuilderSession session;
                if (mainSession == null) {
                    mainSession = new DefaultModelBuilderSession(request);
                    session = mainSession;
                } else {
                    session = mainSession.derive(request, new DefaultModelBuilderResult());
                }
                // Build the request
                if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
                    // build the build poms
                    session.buildBuildPom();
                } else {
                    // simply build the effective model
                    session.buildEffectiveModel(new LinkedHashSet<>());
                }
                return session.result();
            }
        };
    }

    protected final class DefaultModelBuilderSession implements ModelProblemCollector {
        final Session session;
        final ModelBuilderRequest request;
        final DefaultModelBuilderResult result;
        final ModelCache cache;
        final Graph dag;
        final Map<GAKey, Set<ModelSource>> mappedSources;

        String source;
        Model sourceModel;
        Model rootModel;

        List<RemoteRepository> pomRepositories;
        List<RemoteRepository> externalRepositories;
        List<RemoteRepository> repositories;

        DefaultModelBuilderSession(ModelBuilderRequest request) {
            this(request, new DefaultModelBuilderResult());
        }

        DefaultModelBuilderSession(ModelBuilderRequest request, DefaultModelBuilderResult result) {
            this(request.getSession(), request, result);
        }

        DefaultModelBuilderSession(Session session, ModelBuilderRequest request, DefaultModelBuilderResult result) {
            this(
                    session,
                    request,
                    result,
                    session.getData()
                            .computeIfAbsent(SessionData.key(ModelCache.class), modelCacheFactory::newInstance));
        }

        DefaultModelBuilderSession(
                Session session, ModelBuilderRequest request, DefaultModelBuilderResult result, ModelCache cache) {
            this(session, request, result, cache, new Graph(), new ConcurrentHashMap<>(64), null, null, null);
        }

        @SuppressWarnings("checkstyle:ParameterNumber")
        private DefaultModelBuilderSession(
                Session session,
                ModelBuilderRequest request,
                DefaultModelBuilderResult result,
                ModelCache cache,
                Graph dag,
                Map<GAKey, Set<ModelSource>> mappedSources,
                List<RemoteRepository> pomRepositories,
                List<RemoteRepository> externalRepositories,
                List<RemoteRepository> repositories) {
            this.session = session;
            this.request = request;
            this.result = result;
            this.cache = cache;
            this.dag = dag;
            this.mappedSources = mappedSources;
            if (pomRepositories == null) {
                this.pomRepositories = List.of();
                this.externalRepositories = List.copyOf(
                        request.getRepositories() != null
                                ? request.getRepositories()
                                : session.getRemoteRepositories());
                this.repositories = this.externalRepositories;
            } else {
                this.pomRepositories = pomRepositories;
                this.externalRepositories = externalRepositories;
                this.repositories = repositories;
            }
        }

        DefaultModelBuilderSession derive(ModelSource source) {
            return derive(source, new DefaultModelBuilderResult(result));
        }

        DefaultModelBuilderSession derive(ModelSource source, DefaultModelBuilderResult result) {
            return derive(ModelBuilderRequest.build(request, source), result);
        }

        DefaultModelBuilderSession derive(ModelBuilderRequest request, DefaultModelBuilderResult result) {
            if (session != request.getSession()) {
                throw new IllegalArgumentException("Session mismatch");
            }
            return new DefaultModelBuilderSession(
                    session,
                    request,
                    result,
                    cache,
                    dag,
                    mappedSources,
                    pomRepositories,
                    externalRepositories,
                    repositories);
        }

        public Session session() {
            return session;
        }

        public ModelBuilderRequest request() {
            return request;
        }

        public ModelBuilderResult result() {
            return result;
        }

        public ModelCache cache() {
            return cache;
        }

        @Override
        public String toString() {
            return "ModelBuilderSession[" + "session="
                    + session + ", " + "request="
                    + request + ", " + "result="
                    + result + ", " + "cache="
                    + cache + ']';
        }

        PhasingExecutor createExecutor() {
            return new PhasingExecutor(Executors.newFixedThreadPool(getParallelism()));
        }

        private int getParallelism() {
            int parallelism = Runtime.getRuntime().availableProcessors() / 2 + 1;
            try {
                String str = request.getUserProperties().get(Constants.MAVEN_MODEL_BUILDER_PARALLELISM);
                if (str != null) {
                    parallelism = Integer.parseInt(str);
                }
            } catch (Exception e) {
                // ignore
            }
            return Math.max(1, Math.min(parallelism, Runtime.getRuntime().availableProcessors()));
        }

        public Model getRawModel(Path from, String groupId, String artifactId) {
            ModelSource source = getSource(groupId, artifactId);
            if (source != null) {
                if (!addEdge(from, source.getPath())) {
                    return null;
                }
                try {
                    return derive(source).readRawModel();
                } catch (ModelBuilderException e) {
                    // gathered with problem collector
                }
            }
            return null;
        }

        public Model getRawModel(Path from, Path path) {
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Not a regular file: " + path);
            }
            if (!addEdge(from, path)) {
                return null;
            }
            try {
                return derive(ModelSource.fromPath(path)).readRawModel();
            } catch (ModelBuilderException e) {
                // gathered with problem collector
            }
            return null;
        }

        private boolean addEdge(Path from, Path p) {
            try {
                dag.addEdge(from.toString(), p.toString());
                return true;
            } catch (Graph.CycleDetectedException e) {
                add(
                        Severity.FATAL,
                        ModelProblem.Version.BASE,
                        "Cycle detected between models at " + from + " and " + p,
                        null,
                        e);
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
            if (groupId != null) {
                mappedSources
                        .computeIfAbsent(new GAKey(null, artifactId), k -> new HashSet<>())
                        .add(source);
            }
        }

        public boolean hasFatalErrors() {
            return result.getProblems().stream().anyMatch(p -> p.getSeverity() == ModelProblem.Severity.FATAL);
        }

        public boolean hasErrors() {
            return result.getProblems().stream()
                    .anyMatch(p -> p.getSeverity() == ModelProblem.Severity.FATAL
                            || p.getSeverity() == ModelProblem.Severity.ERROR);
        }

        @Override
        public List<ModelProblem> getProblems() {
            return result.getProblems();
        }

        public void setSource(String source) {
            this.source = source;
            this.sourceModel = null;
        }

        public void setSource(Model source) {
            this.sourceModel = source;
            this.source = null;

            if (rootModel == null) {
                rootModel = source;
            }
        }

        public String getSource() {
            if (source == null && sourceModel != null) {
                source = ModelProblemUtils.toPath(sourceModel);
            }
            return source;
        }

        private String getModelId() {
            return ModelProblemUtils.toId(sourceModel);
        }

        public void setRootModel(Model rootModel) {
            this.rootModel = rootModel;
        }

        public Model getRootModel() {
            return rootModel;
        }

        @Override
        public void add(ModelProblem problem) {
            result.addProblem(problem);
        }

        @Override
        public void add(BuilderProblem.Severity severity, ModelProblem.Version version, String message) {
            add(severity, version, message, null, null);
        }

        @Override
        public void add(
                BuilderProblem.Severity severity,
                ModelProblem.Version version,
                String message,
                InputLocation location) {
            add(severity, version, message, location, null);
        }

        @Override
        public void add(
                BuilderProblem.Severity severity, ModelProblem.Version version, String message, Exception exception) {
            add(severity, version, message, null, exception);
        }

        public void add(
                BuilderProblem.Severity severity,
                ModelProblem.Version version,
                String message,
                InputLocation location,
                Exception exception) {
            int line = -1;
            int column = -1;
            String source = null;
            String modelId = null;

            if (location != null) {
                line = location.getLineNumber();
                column = location.getColumnNumber();
                if (location.getSource() != null) {
                    modelId = location.getSource().getModelId();
                    source = location.getSource().getLocation();
                }
            }

            if (modelId == null) {
                modelId = getModelId();
                source = getSource();
            }

            if (line <= 0 && column <= 0 && exception instanceof ModelParserException e) {
                line = e.getLineNumber();
                column = e.getColumnNumber();
            }

            ModelProblem problem =
                    new DefaultModelProblem(message, severity, version, source, line, column, modelId, exception);

            add(problem);
        }

        public ModelBuilderException newModelBuilderException() {
            //            ModelBuilderResult result = this.result;
            //            if (result.getEffectiveModel() == null && result.getParentModel() == null) {
            //                DefaultModelBuilderResult tmp = new DefaultModelBuilderResult();
            //                tmp.setParentModel(result.getParentModel());
            //                tmp.setEffectiveModel(result.getEffectiveModel());
            //                tmp.setProblems(getProblems());
            //                tmp.setActiveExternalProfiles(result.getActiveExternalProfiles());
            //                String id = getRootModelId();
            //                tmp.setRawModel(id, getRootModel());
            //                result = tmp;
            //            }
            return new ModelBuilderException(result);
        }

        public List<RemoteRepository> getRepositories() {
            return repositories;
        }

        /**
         * TODO: this is not thread safe and the session is mutated
         */
        public void mergeRepositories(List<Repository> toAdd, boolean replace) {
            List<RemoteRepository> repos =
                    toAdd.stream().map(session::createRemoteRepository).toList();
            if (replace) {
                Set<String> ids = repos.stream().map(RemoteRepository::getId).collect(Collectors.toSet());
                repositories = repositories.stream()
                        .filter(r -> !ids.contains(r.getId()))
                        .toList();
                pomRepositories = pomRepositories.stream()
                        .filter(r -> !ids.contains(r.getId()))
                        .toList();
            } else {
                Set<String> ids =
                        pomRepositories.stream().map(RemoteRepository::getId).collect(Collectors.toSet());
                repos = repos.stream().filter(r -> !ids.contains(r.getId())).toList();
            }

            RepositoryFactory repositoryFactory = session.getService(RepositoryFactory.class);
            if (request.getRepositoryMerging() == ModelBuilderRequest.RepositoryMerging.REQUEST_DOMINANT) {
                repositories = repositoryFactory.aggregate(session, repositories, repos, true);
                pomRepositories = repositories;
            } else {
                pomRepositories = repositoryFactory.aggregate(session, pomRepositories, repos, true);
                repositories = repositoryFactory.aggregate(session, pomRepositories, externalRepositories, false);
            }
        }

        //
        // Transform raw model to build pom
        //
        Model transformFileToRaw(Model model) {
            Model.Builder builder = Model.newBuilder(model);
            builder = handleParent(model, builder);
            builder = handleReactorDependencies(model, builder);
            builder = handleCiFriendlyVersion(model, builder);
            return builder.build();
        }

        //
        // Infer parent information
        //
        Model.Builder handleParent(Model model, Model.Builder builder) {
            Parent parent = model.getParent();
            if (parent != null) {
                String version = parent.getVersion();
                String modVersion = replaceCiFriendlyVersion(version);
                if (!Objects.equals(version, modVersion)) {
                    if (builder == null) {
                        builder = Model.newBuilder(model);
                    }
                    builder.parent(parent.withVersion(modVersion));
                }
            }
            return builder;
        }

        //
        // CI friendly versions
        //
        Model.Builder handleCiFriendlyVersion(Model model, Model.Builder builder) {
            String version = model.getVersion();
            String modVersion = replaceCiFriendlyVersion(version);
            if (!Objects.equals(version, modVersion)) {
                if (builder == null) {
                    builder = Model.newBuilder(model);
                }
                builder.version(modVersion);
            }
            return builder;
        }

        //
        // Infer inner reactor dependencies version
        //
        Model.Builder handleReactorDependencies(Model model, Model.Builder builder) {
            List<Dependency> newDeps = new ArrayList<>();
            boolean modified = false;
            for (Dependency dep : model.getDependencies()) {
                Dependency.Builder depBuilder = null;
                if (dep.getVersion() == null) {
                    Model depModel = getRawModel(model.getPomFile(), dep.getGroupId(), dep.getArtifactId());
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
                if (builder == null) {
                    builder = Model.newBuilder(model);
                }
                builder.dependencies(newDeps);
            }
            return builder;
        }

        String replaceCiFriendlyVersion(String version) {
            if (version != null) {
                for (String key : Arrays.asList("changelist", "revision", "sha1")) {
                    String val = request.getUserProperties().get(key);
                    if (val != null) {
                        version = version.replace("${" + key + "}", val);
                    }
                }
            }
            return version;
        }

        private void buildBuildPom() throws ModelBuilderException {
            // Retrieve and normalize the source path, ensuring it's non-null and in absolute form
            Path top = request.getSource().getPath();
            if (top == null) {
                throw new IllegalStateException("Recursive build requested but source has no path");
            }
            top = top.toAbsolutePath().normalize();

            // Obtain the root directory, resolving it if necessary
            Path rootDirectory;
            try {
                rootDirectory = session.getRootDirectory();
            } catch (IllegalStateException e) {
                rootDirectory = session.getService(RootLocator.class).findRoot(top);
            }

            // Locate and normalize the root POM if it exists, fallback to top otherwise
            Path root = getModelProcessor().locateExistingPom(rootDirectory);
            if (root != null) {
                root = root.toAbsolutePath().normalize();
            } else {
                root = top;
            }

            // Load all models starting from the root
            loadFromRoot(root, top);

            // Check for errors after loading models
            if (hasErrors()) {
                throw newModelBuilderException();
            }

            // For the top model and all its children, build the effective model.
            // This is done through the phased executor
            var allResults = results(result).toList();
            try (PhasingExecutor executor = createExecutor()) {
                for (DefaultModelBuilderResult r : allResults) {
                    executor.execute(() -> {
                        DefaultModelBuilderSession mbs = derive(r.getSource(), r);
                        try {
                            mbs.buildEffectiveModel(new LinkedHashSet<>());
                        } catch (ModelBuilderException e) {
                            // gathered with problem collector
                        } catch (Exception t) {
                            mbs.add(Severity.FATAL, ModelProblem.Version.BASE, t.getMessage(), t);
                        }
                    });
                }
            }

            // Check for errors again after execution
            if (hasErrors()) {
                throw newModelBuilderException();
            }
        }

        /**
         * Generates a stream of DefaultModelBuilderResult objects, starting with the provided
         * result and recursively including all its child results.
         *
         * @param r The initial DefaultModelBuilderResult object from which to generate the stream.
         * @return A Stream of DefaultModelBuilderResult objects, starting with the provided result
         *         and including all its child results.
         */
        Stream<DefaultModelBuilderResult> results(DefaultModelBuilderResult r) {
            return Stream.concat(Stream.of(r), r.getChildren().stream().flatMap(this::results));
        }

        @SuppressWarnings("checkstyle:MethodLength")
        private void loadFromRoot(Path root, Path top) {
            try (PhasingExecutor executor = createExecutor()) {
                loadFilePom(executor, top, root, Set.of(), null);
            }
            if (result.getFileModel() == null && !Objects.equals(top, root)) {
                logger.warn(
                        "The top project ({}) cannot be found in the reactor from root project ({}). "
                                + "Make sure the root directory is correct (a missing '.mvn' directory in the root "
                                + "project is the most common cause) and the project is correctly included "
                                + "in the reactor (missing activated profiles, command line options, etc.). For this "
                                + "build, the top project will be used as the root project.",
                        top,
                        root);
                cache.clear();
                mappedSources.clear();
                loadFromRoot(top, top);
            }
        }

        private void loadFilePom(
                Executor executor, Path top, Path pom, Set<Path> parents, DefaultModelBuilderResult parent) {
            DefaultModelBuilderResult r;
            if (pom.equals(top)) {
                r = result;
            } else {
                r = new DefaultModelBuilderResult(parent);
                if (parent != null) {
                    parent.getChildren().add(r);
                }
            }
            try {
                Path pomDirectory = Files.isDirectory(pom) ? pom : pom.getParent();
                ModelSource src = ModelSource.fromPath(pom);
                Model model = derive(src, r).readFileModel();
                putSource(getGroupId(model), model.getArtifactId(), src);
                Model activated = activateFileModel(model);
                List<String> subprojects = activated.getSubprojects();
                if (subprojects.isEmpty()) {
                    subprojects = activated.getModules();
                }
                for (String subproject : subprojects) {
                    if (subproject == null || subproject.isEmpty()) {
                        continue;
                    }

                    subproject = subproject.replace('\\', File.separatorChar).replace('/', File.separatorChar);

                    Path rawSubprojectFile = modelProcessor.locateExistingPom(pomDirectory.resolve(subproject));

                    if (rawSubprojectFile == null) {
                        ModelProblem problem = new DefaultModelProblem(
                                "Child subproject " + subproject + " of " + pomDirectory + " does not exist",
                                Severity.ERROR,
                                ModelProblem.Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        r.addProblem(problem);
                        continue;
                    }

                    Path subprojectFile = rawSubprojectFile.toAbsolutePath().normalize();

                    if (parents.contains(subprojectFile)) {
                        StringBuilder buffer = new StringBuilder(256);
                        for (Path aggregatorFile : parents) {
                            buffer.append(aggregatorFile).append(" -> ");
                        }
                        buffer.append(subprojectFile);

                        ModelProblem problem = new DefaultModelProblem(
                                "Child subproject " + subprojectFile + " of " + pom + " forms aggregation cycle "
                                        + buffer,
                                Severity.ERROR,
                                ModelProblem.Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        r.addProblem(problem);
                        continue;
                    }

                    executor.execute(() -> loadFilePom(
                            executor,
                            top,
                            subprojectFile,
                            concat(parents, pom),
                            (parent != null || Objects.equals(pom, top)) && request.isRecursive() ? r : null));
                }
            } catch (ModelBuilderException e) {
                // gathered with problem collector
                add(Severity.ERROR, ModelProblem.Version.V40, "Failed to load project " + pom, e);
            }
            if (r != result) {
                r.getProblems().forEach(result::addProblem);
            }
        }

        static <T> Set<T> concat(Set<T> a, T b) {
            Set<T> result = new HashSet<>(a);
            result.add(b);
            return Set.copyOf(result);
        }

        void buildEffectiveModel(Collection<String> importIds) throws ModelBuilderException {
            Model resultModel = readEffectiveModel();
            setSource(resultModel);
            setRootModel(resultModel);

            // model path translation
            resultModel =
                    modelPathTranslator.alignToBaseDirectory(resultModel, resultModel.getProjectDirectory(), request);

            // plugin management injection
            resultModel = pluginManagementInjector.injectManagement(resultModel, request, this);

            // lifecycle bindings injection
            if (request.getRequestType() != ModelBuilderRequest.RequestType.DEPENDENCY) {
                org.apache.maven.api.services.ModelTransformer lifecycleBindingsInjector =
                        request.getLifecycleBindingsInjector();
                if (lifecycleBindingsInjector != null) {
                    resultModel = lifecycleBindingsInjector.transform(resultModel, request, this);
                }
            }

            // dependency management import
            resultModel = importDependencyManagement(resultModel, importIds);

            // dependency management injection
            resultModel = dependencyManagementInjector.injectManagement(resultModel, request, this);

            resultModel = modelNormalizer.injectDefaultValues(resultModel, request, this);

            if (request.getRequestType() != ModelBuilderRequest.RequestType.DEPENDENCY) {
                // plugins configuration
                resultModel = pluginConfigurationExpander.expandPluginConfiguration(resultModel, request, this);
            }

            for (var transformer : transformers) {
                resultModel = transformer.transformEffectiveModel(resultModel);
            }

            result.setEffectiveModel(resultModel);

            // effective model validation
            modelValidator.validateEffectiveModel(
                    resultModel,
                    request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM
                            ? ModelValidator.VALIDATION_LEVEL_STRICT
                            : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    request,
                    this);

            if (hasErrors()) {
                throw newModelBuilderException();
            }
        }

        Model readParent(Model childModel, ModelSource childSource) throws ModelBuilderException {
            Model parentModel = null;

            Parent parent = childModel.getParent();
            if (parent != null) {
                parentModel = readParentLocally(childModel, childSource);
                if (parentModel == null) {
                    parentModel = resolveAndReadParentExternally(childModel);
                }

                if (!"pom".equals(parentModel.getPackaging())) {
                    add(
                            Severity.ERROR,
                            ModelProblem.Version.BASE,
                            "Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                    + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"",
                            parentModel.getLocation("packaging"));
                }
            }

            return parentModel;
        }

        private Model readParentLocally(Model childModel, ModelSource childSource) throws ModelBuilderException {
            ModelSource candidateSource = getParentPomFile(childModel, childSource);
            if (candidateSource == null) {
                return null;
            }

            Model candidateModel = derive(candidateSource).readParentModel();

            //
            // TODO jvz Why isn't all this checking the job of the duty of the workspace resolver, we know that we
            // have a model that is suitable, yet more checks are done here and the one for the version is problematic
            // before because with parents as ranges it will never work in this scenario.
            //

            String groupId = getGroupId(candidateModel);
            String artifactId = candidateModel.getArtifactId();

            Parent parent = childModel.getParent();
            if (groupId == null
                    || !groupId.equals(parent.getGroupId())
                    || artifactId == null
                    || !artifactId.equals(parent.getArtifactId())) {
                StringBuilder buffer = new StringBuilder(256);
                buffer.append("'parent.relativePath'");
                if (childModel != getRootModel()) {
                    buffer.append(" of POM ").append(ModelProblemUtils.toSourceHint(childModel));
                }
                buffer.append(" points at ").append(groupId).append(':').append(artifactId);
                buffer.append(" instead of ").append(parent.getGroupId()).append(':');
                buffer.append(parent.getArtifactId()).append(", please verify your project structure");

                setSource(childModel);
                add(Severity.WARNING, ModelProblem.Version.BASE, buffer.toString(), parent.getLocation(""));
                return null;
            }

            String version = getVersion(candidateModel);
            if (version != null && parent.getVersion() != null && !version.equals(parent.getVersion())) {
                try {
                    VersionRange parentRange = versionParser.parseVersionRange(parent.getVersion());
                    if (!parentRange.contains(versionParser.parseVersion(version))) {
                        // version skew drop back to resolution from the repository
                        return null;
                    }

                    // Validate versions aren't inherited when using parent ranges the same way as when read externally.
                    String rawChildModelVersion = childModel.getVersion();

                    if (rawChildModelVersion == null) {
                        // Message below is checked for in the MNG-2199 core IT.
                        add(
                                Severity.FATAL,
                                ModelProblem.Version.V31,
                                "Version must be a constant",
                                childModel.getLocation(""));

                    } else {
                        if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                            // Message below is checked for in the MNG-2199 core IT.
                            add(
                                    Severity.FATAL,
                                    ModelProblem.Version.V31,
                                    "Version must be a constant",
                                    childModel.getLocation("version"));
                        }
                    }

                    // MNG-2199: What else to check here ?
                } catch (VersionParserException e) {
                    // invalid version range, so drop back to resolution from the repository
                    return null;
                }
            }

            //
            // Here we just need to know that a version is fine to use but this validation we can do in our workspace
            // resolver.
            //

            return candidateModel;
        }

        Model resolveAndReadParentExternally(Model childModel) throws ModelBuilderException {
            ModelBuilderRequest request = this.request;
            setSource(childModel);

            Parent parent = childModel.getParent();

            String groupId = parent.getGroupId();
            String artifactId = parent.getArtifactId();
            String version = parent.getVersion();

            // add repositories specified by the current model so that we can resolve the parent
            if (!childModel.getRepositories().isEmpty()) {
                List<String> oldRepos =
                        getRepositories().stream().map(Object::toString).toList();
                mergeRepositories(childModel.getRepositories(), false);
                List<String> newRepos =
                        getRepositories().stream().map(Object::toString).toList();
                if (!Objects.equals(oldRepos, newRepos)) {
                    logger.debug("Merging repositories from " + childModel.getId() + "\n"
                            + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
                }
            }

            ModelSource modelSource;
            try {
                modelSource = resolveReactorModel(groupId, artifactId, version);
                if (modelSource == null) {
                    AtomicReference<Parent> modified = new AtomicReference<>();
                    modelSource = modelResolver.resolveModel(request.getSession(), getRepositories(), parent, modified);
                    if (modified.get() != null) {
                        parent = modified.get();
                    }
                }
            } catch (ModelResolverException e) {
                // Message below is checked for in the MNG-2199 core IT.
                StringBuilder buffer = new StringBuilder(256);
                buffer.append("Non-resolvable parent POM");
                if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                    buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
                }
                if (childModel != getRootModel()) {
                    buffer.append(" for ").append(ModelProblemUtils.toId(childModel));
                }
                buffer.append(": ").append(e.getMessage());
                if (childModel.getProjectDirectory() != null) {
                    if (parent.getRelativePath() == null
                            || parent.getRelativePath().isEmpty()) {
                        buffer.append(" and 'parent.relativePath' points at no local POM");
                    } else {
                        buffer.append(" and 'parent.relativePath' points at wrong local POM");
                    }
                }

                add(Severity.FATAL, ModelProblem.Version.BASE, buffer.toString(), parent.getLocation(""), e);
                throw newModelBuilderException();
            }

            ModelBuilderRequest lenientRequest = ModelBuilderRequest.builder(request)
                    .requestType(ModelBuilderRequest.RequestType.PARENT_POM)
                    .source(modelSource)
                    .build();

            DefaultModelBuilderResult r = new DefaultModelBuilderResult(this.result);
            Model parentModel = new DefaultModelBuilderSession(lenientRequest, r).readParentModel();

            if (!parent.getVersion().equals(version)) {
                String rawChildModelVersion = childModel.getVersion();

                if (rawChildModelVersion == null) {
                    // Message below is checked for in the MNG-2199 core IT.
                    add(
                            Severity.FATAL,
                            ModelProblem.Version.V31,
                            "Version must be a constant",
                            childModel.getLocation(""));
                } else {
                    if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                        // Message below is checked for in the MNG-2199 core IT.
                        add(
                                Severity.FATAL,
                                ModelProblem.Version.V31,
                                "Version must be a constant",
                                childModel.getLocation("version"));
                    }
                }

                // MNG-2199: What else to check here ?
            }

            return parentModel;
        }

        Model activateFileModel(Model inputModel) throws ModelBuilderException {
            setRootModel(inputModel);

            // profile activation
            DefaultProfileActivationContext profileActivationContext = getProfileActivationContext(request, inputModel);

            setSource("(external profiles)");
            List<Profile> activeExternalProfiles =
                    profileSelector.getActiveProfiles(request.getProfiles(), profileActivationContext, this);

            result.setActiveExternalProfiles(activeExternalProfiles);

            if (!activeExternalProfiles.isEmpty()) {
                Properties profileProps = new Properties();
                for (Profile profile : activeExternalProfiles) {
                    profileProps.putAll(profile.getProperties());
                }
                profileProps.putAll(profileActivationContext.getUserProperties());
                profileActivationContext.setUserProperties(profileProps);
            }

            profileActivationContext.setProjectProperties(inputModel.getProperties());
            setSource(inputModel);
            List<Profile> activePomProfiles =
                    profileSelector.getActiveProfiles(inputModel.getProfiles(), profileActivationContext, this);

            // model normalization
            setSource(inputModel);
            inputModel = modelNormalizer.mergeDuplicates(inputModel, request, this);

            Map<String, Activation> interpolatedActivations = getProfileActivations(inputModel);
            inputModel = injectProfileActivations(inputModel, interpolatedActivations);

            // profile injection
            inputModel = profileInjector.injectProfiles(inputModel, activePomProfiles, request, this);
            inputModel = profileInjector.injectProfiles(inputModel, activeExternalProfiles, request, this);

            return inputModel;
        }

        @SuppressWarnings("checkstyle:methodlength")
        private Model readEffectiveModel() throws ModelBuilderException {
            Model inputModel = readRawModel();
            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            inputModel = activateFileModel(inputModel);

            setRootModel(inputModel);

            // profile activation
            DefaultProfileActivationContext profileActivationContext = getProfileActivationContext(request, inputModel);

            List<Profile> activeExternalProfiles = result.getActiveExternalProfiles();

            if (!activeExternalProfiles.isEmpty()) {
                Properties profileProps = new Properties();
                for (Profile profile : activeExternalProfiles) {
                    profileProps.putAll(profile.getProperties());
                }
                profileProps.putAll(profileActivationContext.getUserProperties());
                profileActivationContext.setUserProperties(profileProps);
            }

            Model parentModel = readParent(inputModel, request.getSource());
            if (parentModel == null) {
                String superModelVersion =
                        inputModel.getModelVersion() != null ? inputModel.getModelVersion() : MODEL_VERSION_4_0_0;
                if (!VALID_MODEL_VERSIONS.contains(superModelVersion)) {
                    // Maven 3.x is always using 4.0.0 version to load the supermodel, so
                    // do the same when loading a dependency.  The model validator will also
                    // check that field later.
                    superModelVersion = MODEL_VERSION_4_0_0;
                }
                parentModel = getSuperModel(superModelVersion);
            } else {
                result.setParentModel(parentModel);
            }

            List<Profile> parentInterpolatedProfiles =
                    interpolateActivations(parentModel.getProfiles(), profileActivationContext, this);
            // profile injection
            List<Profile> parentActivePomProfiles =
                    profileSelector.getActiveProfiles(parentInterpolatedProfiles, profileActivationContext, this);
            Model injectedParentModel = profileInjector
                    .injectProfiles(parentModel, parentActivePomProfiles, request, this)
                    .withProfiles(List.of());

            Model model = inheritanceAssembler.assembleModelInheritance(inputModel, injectedParentModel, request, this);

            // model normalization
            model = modelNormalizer.mergeDuplicates(model, request, this);

            // profile activation
            profileActivationContext.setProjectProperties(model.getProperties());

            List<Profile> interpolatedProfiles =
                    interpolateActivations(model.getProfiles(), profileActivationContext, this);

            // profile injection
            List<Profile> activePomProfiles =
                    profileSelector.getActiveProfiles(interpolatedProfiles, profileActivationContext, this);
            result.setActivePomProfiles(activePomProfiles);
            model = profileInjector.injectProfiles(model, activePomProfiles, request, this);
            model = profileInjector.injectProfiles(model, activeExternalProfiles, request, this);

            // model interpolation
            Model resultModel = model;
            resultModel = interpolateModel(resultModel, request, this);

            // url normalization
            resultModel = modelUrlNormalizer.normalize(resultModel, request);

            result.setEffectiveModel(resultModel);

            // Now the fully interpolated model is available: reconfigure the resolver
            if (!resultModel.getRepositories().isEmpty()) {
                List<String> oldRepos =
                        getRepositories().stream().map(Object::toString).toList();
                mergeRepositories(resultModel.getRepositories(), true);
                List<String> newRepos =
                        getRepositories().stream().map(Object::toString).toList();
                if (!Objects.equals(oldRepos, newRepos)) {
                    logger.debug("Replacing repositories from " + resultModel.getId() + "\n"
                            + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
                }
            }

            return resultModel;
        }

        Model readFileModel() throws ModelBuilderException {
            result.setSource(request.getSource());
            Model model = cache(request.getSource(), FILE, this::doReadFileModel);
            result.setFileModel(model);
            return model;
        }

        @SuppressWarnings("checkstyle:methodlength")
        Model doReadFileModel() throws ModelBuilderException {
            ModelSource modelSource = request.getSource();
            Model model;
            setSource(modelSource.getLocation());
            logger.debug("Reading file model from " + modelSource.getLocation());
            try {
                boolean strict = request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM;
                // TODO: we do cache, but what if strict does not have the same value?
                Path rootDirectory;
                try {
                    rootDirectory = request.getSession().getRootDirectory();
                } catch (IllegalStateException ignore) {
                    rootDirectory = modelSource.getPath();
                }
                try (InputStream is = modelSource.openStream()) {
                    model = modelProcessor.read(XmlReaderRequest.builder()
                            .strict(strict)
                            .location(modelSource.getLocation())
                            .path(modelSource.getPath())
                            .rootDirectory(rootDirectory)
                            .inputStream(is)
                            .build());
                } catch (XmlReaderException e) {
                    if (!strict) {
                        throw e;
                    }
                    try (InputStream is = modelSource.openStream()) {
                        model = modelProcessor.read(XmlReaderRequest.builder()
                                .strict(false)
                                .location(modelSource.getLocation())
                                .path(modelSource.getPath())
                                .rootDirectory(rootDirectory)
                                .inputStream(is)
                                .build());
                    } catch (XmlReaderException ne) {
                        // still unreadable even in non-strict mode, rethrow original error
                        throw e;
                    }

                    Severity severity = request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM
                            ? Severity.ERROR
                            : Severity.WARNING;
                    add(
                            severity,
                            ModelProblem.Version.V20,
                            "Malformed POM " + modelSource.getLocation() + ": " + e.getMessage(),
                            e);
                }

                InputLocation loc = model.getLocation("");
                InputSource v4src = loc != null ? loc.getSource() : null;
                if (v4src != null) {
                    try {
                        Field field = InputSource.class.getDeclaredField("modelId");
                        field.setAccessible(true);
                        field.set(v4src, ModelProblemUtils.toId(model));
                    } catch (Throwable t) {
                        // TODO: use a lazy source ?
                        throw new IllegalStateException("Unable to set modelId on InputSource", t);
                    }
                }
            } catch (XmlReaderException e) {
                add(
                        Severity.FATAL,
                        ModelProblem.Version.BASE,
                        "Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage(),
                        e);
                throw newModelBuilderException();
            } catch (IOException e) {
                String msg = e.getMessage();
                if (msg == null || msg.isEmpty()) {
                    // NOTE: There's java.nio.charset.MalformedInputException and sun.io.MalformedInputException
                    if (e.getClass().getName().endsWith("MalformedInputException")) {
                        msg = "Some input bytes do not match the file encoding.";
                    } else {
                        msg = e.getClass().getSimpleName();
                    }
                }
                add(
                        Severity.FATAL,
                        ModelProblem.Version.BASE,
                        "Non-readable POM " + modelSource.getLocation() + ": " + msg,
                        e);
                throw newModelBuilderException();
            }

            if (model.getModelVersion() == null) {
                String namespace = model.getNamespaceUri();
                if (namespace != null && namespace.startsWith(NAMESPACE_PREFIX)) {
                    model = model.withModelVersion(namespace.substring(NAMESPACE_PREFIX.length()));
                }
            }

            if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
                model = model.withPomFile(modelSource.getPath());

                Parent parent = model.getParent();
                if (parent != null) {
                    String groupId = parent.getGroupId();
                    String artifactId = parent.getArtifactId();
                    String version = parent.getVersion();
                    String path = Optional.ofNullable(parent.getRelativePath()).orElse("..");
                    if (version == null && !path.isEmpty()) {
                        Path pomFile = model.getPomFile();
                        Path relativePath = Paths.get(path);
                        Path pomPath = pomFile.resolveSibling(relativePath).normalize();
                        if (Files.isDirectory(pomPath)) {
                            pomPath = getModelProcessor().locateExistingPom(pomPath);
                        }
                        if (pomPath != null && Files.isRegularFile(pomPath)) {
                            Model parentModel =
                                    derive(ModelSource.fromPath(pomPath)).readFileModel();
                            if (parentModel != null) {
                                String parentGroupId = getGroupId(parentModel);
                                String parentArtifactId = parentModel.getArtifactId();
                                String parentVersion = getVersion(parentModel);
                                if ((groupId == null || groupId.equals(parentGroupId))
                                        && (artifactId == null || artifactId.equals(parentArtifactId))) {
                                    model = model.withParent(parent.with()
                                            .groupId(parentGroupId)
                                            .artifactId(parentArtifactId)
                                            .version(parentVersion)
                                            .build());
                                }
                            }
                        }
                    }
                }

                // subprojects discovery
                if (model.getSubprojects().isEmpty()
                        && model.getModules().isEmpty()
                        // only discover subprojects if POM > 4.0.0
                        && !MODEL_VERSION_4_0_0.equals(model.getModelVersion())
                        // and if packaging is POM (we check type, but the session is not yet available,
                        // we would require the project realm if we want to support extensions
                        && Type.POM.equals(model.getPackaging())) {
                    List<String> subprojects = new ArrayList<>();
                    try (Stream<Path> files = Files.list(model.getProjectDirectory())) {
                        for (Path f : files.toList()) {
                            if (Files.isDirectory(f)) {
                                Path subproject = modelProcessor.locateExistingPom(f);
                                if (subproject != null) {
                                    subprojects.add(f.getFileName().toString());
                                }
                            }
                        }
                        if (!subprojects.isEmpty()) {
                            model = model.withSubprojects(subprojects);
                        }
                    } catch (IOException e) {
                        add(Severity.FATAL, ModelProblem.Version.V41, "Error discovering subprojects", e);
                    }
                }
            }

            for (var transformer : transformers) {
                model = transformer.transformFileModel(model);
            }

            setSource(model);
            modelValidator.validateFileModel(
                    model,
                    request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM
                            ? ModelValidator.VALIDATION_LEVEL_STRICT
                            : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    request,
                    this);
            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            return model;
        }

        Model readRawModel() throws ModelBuilderException {
            readFileModel();
            Model model = cache(request.getSource(), RAW, this::doReadRawModel);
            result.setRawModel(model);
            return model;
        }

        private Model doReadRawModel() throws ModelBuilderException {
            ModelBuilderRequest request = this.request;
            Model rawModel = readFileModel();

            if (!MODEL_VERSION_4_0_0.equals(rawModel.getModelVersion())
                    && request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
                rawModel = transformFileToRaw(rawModel);
            }

            for (var transformer : transformers) {
                rawModel = transformer.transformRawModel(rawModel);
            }

            modelValidator.validateRawModel(
                    rawModel,
                    request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM
                            ? ModelValidator.VALIDATION_LEVEL_STRICT
                            : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    request,
                    this);

            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            return rawModel;
        }

        Model readParentModel() {
            return cache(request.getSource(), PARENT, this::doReadParentModel);
        }

        private Model doReadParentModel() {
            Model raw = readRawModel();

            Model parentData;
            if (raw.getParent() != null) {
                parentData = readParent(raw, request.getSource());
            } else {
                String superModelVersion = raw.getModelVersion() != null ? raw.getModelVersion() : "4.0.0";
                if (!VALID_MODEL_VERSIONS.contains(superModelVersion)) {
                    // Maven 3.x is always using 4.0.0 version to load the supermodel, so
                    // do the same when loading a dependency.  The model validator will also
                    // check that field later.
                    superModelVersion = MODEL_VERSION_4_0_0;
                }
                parentData = getSuperModel(superModelVersion);
            }

            Model parent = new DefaultInheritanceAssembler(new DefaultInheritanceAssembler.InheritanceModelMerger() {
                        @Override
                        protected void mergeModel_Modules(
                                Model.Builder builder,
                                Model target,
                                Model source,
                                boolean sourceDominant,
                                Map<Object, Object> context) {}

                        @Override
                        protected void mergeModel_Subprojects(
                                Model.Builder builder,
                                Model target,
                                Model source,
                                boolean sourceDominant,
                                Map<Object, Object> context) {}

                        @Override
                        protected void mergeModel_Profiles(
                                Model.Builder builder,
                                Model target,
                                Model source,
                                boolean sourceDominant,
                                Map<Object, Object> context) {
                            builder.profiles(Stream.concat(source.getProfiles().stream(), target.getProfiles().stream())
                                    .map(p -> p.withModules(null).withSubprojects(null))
                                    .toList());
                        }
                    })
                    .assembleModelInheritance(raw, parentData, request, this);

            return parent.withParent(null);
        }

        private Model importDependencyManagement(Model model, Collection<String> importIds) {
            DependencyManagement depMgmt = model.getDependencyManagement();

            if (depMgmt == null) {
                return model;
            }

            String importing = model.getGroupId() + ':' + model.getArtifactId() + ':' + model.getVersion();

            importIds.add(importing);

            List<DependencyManagement> importMgmts = null;

            List<Dependency> deps = new ArrayList<>(depMgmt.getDependencies());
            for (Iterator<Dependency> it = deps.iterator(); it.hasNext(); ) {
                Dependency dependency = it.next();

                if (!("pom".equals(dependency.getType()) && "import".equals(dependency.getScope()))
                        || "bom".equals(dependency.getType())) {
                    continue;
                }

                it.remove();

                DependencyManagement importMgmt = loadDependencyManagement(dependency, importIds);

                if (importMgmt != null) {
                    if (importMgmts == null) {
                        importMgmts = new ArrayList<>();
                    }

                    importMgmts.add(importMgmt);
                }
            }

            importIds.remove(importing);

            model = model.withDependencyManagement(
                    model.getDependencyManagement().withDependencies(deps));

            return dependencyManagementImporter.importManagement(model, importMgmts, request, this);
        }

        private DependencyManagement loadDependencyManagement(Dependency dependency, Collection<String> importIds) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();

            if (groupId == null || groupId.isEmpty()) {
                add(
                        Severity.ERROR,
                        ModelProblem.Version.BASE,
                        "'dependencyManagement.dependencies.dependency.groupId' for " + dependency.getManagementKey()
                                + " is missing.",
                        dependency.getLocation(""));
                return null;
            }
            if (artifactId == null || artifactId.isEmpty()) {
                add(
                        Severity.ERROR,
                        ModelProblem.Version.BASE,
                        "'dependencyManagement.dependencies.dependency.artifactId' for " + dependency.getManagementKey()
                                + " is missing.",
                        dependency.getLocation(""));
                return null;
            }
            if (version == null || version.isEmpty()) {
                add(
                        Severity.ERROR,
                        ModelProblem.Version.BASE,
                        "'dependencyManagement.dependencies.dependency.version' for " + dependency.getManagementKey()
                                + " is missing.",
                        dependency.getLocation(""));
                return null;
            }

            String imported = groupId + ':' + artifactId + ':' + version;

            if (importIds.contains(imported)) {
                StringBuilder message =
                        new StringBuilder("The dependencies of type=pom and with scope=import form a cycle: ");
                for (String modelId : importIds) {
                    message.append(modelId).append(" -> ");
                }
                message.append(imported);
                add(Severity.ERROR, ModelProblem.Version.BASE, message.toString());
                return null;
            }

            Model importModel = cache(
                    groupId,
                    artifactId,
                    version,
                    IMPORT,
                    () -> doLoadDependencyManagement(dependency, groupId, artifactId, version, importIds));
            DependencyManagement importMgmt = importModel != null ? importModel.getDependencyManagement() : null;
            if (importMgmt == null) {
                importMgmt = DependencyManagement.newInstance();
            }

            // [MNG-5600] Dependency management import should support exclusions.
            List<Exclusion> exclusions = dependency.getExclusions();
            if (importMgmt != null && !exclusions.isEmpty()) {
                // Dependency excluded from import.
                List<Dependency> dependencies = importMgmt.getDependencies().stream()
                        .filter(candidate -> exclusions.stream().noneMatch(exclusion -> match(exclusion, candidate)))
                        .map(candidate -> addExclusions(candidate, exclusions))
                        .collect(Collectors.toList());
                importMgmt = importMgmt.withDependencies(dependencies);
            }

            return importMgmt;
        }

        @SuppressWarnings("checkstyle:parameternumber")
        private Model doLoadDependencyManagement(
                Dependency dependency,
                String groupId,
                String artifactId,
                String version,
                Collection<String> importIds) {
            Model importModel;
            ModelSource importSource;
            try {
                importSource = resolveReactorModel(groupId, artifactId, version);
                if (importSource == null) {
                    importSource = modelResolver.resolveModel(
                            request.getSession(), getRepositories(), dependency, new AtomicReference<>());
                }
            } catch (ModelBuilderException e) {
                StringBuilder buffer = new StringBuilder(256);
                buffer.append("Non-resolvable import POM");
                if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                    buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
                }
                buffer.append(": ").append(e.getMessage());

                add(Severity.ERROR, ModelProblem.Version.BASE, buffer.toString(), dependency.getLocation(""), e);
                return null;
            }

            Path rootDirectory;
            try {
                rootDirectory = request.getSession().getRootDirectory();
            } catch (IllegalStateException e) {
                rootDirectory = null;
            }
            if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM && rootDirectory != null) {
                Path sourcePath = importSource.getPath();
                if (sourcePath != null && sourcePath.startsWith(rootDirectory)) {
                    add(
                            Severity.WARNING,
                            ModelProblem.Version.BASE,
                            "BOM imports from within reactor should be avoided",
                            dependency.getLocation(""));
                }
            }

            final ModelBuilderResult importResult;
            try {
                ModelBuilderRequest importRequest = ModelBuilderRequest.builder()
                        .session(request.getSession())
                        .requestType(ModelBuilderRequest.RequestType.DEPENDENCY)
                        .systemProperties(request.getSystemProperties())
                        .userProperties(request.getUserProperties())
                        .source(importSource)
                        .repositories(getRepositories())
                        .build();
                DefaultModelBuilderSession modelBuilderSession = new DefaultModelBuilderSession(importRequest);
                // build the effective model
                modelBuilderSession.buildEffectiveModel(importIds);
                importResult = modelBuilderSession.result();
            } catch (ModelBuilderException e) {
                e.getResult().getProblems().forEach(this::add);
                return null;
            }

            importResult.getProblems().forEach(this::add);

            importModel = importResult.getEffectiveModel();

            return importModel;
        }

        ModelSource resolveReactorModel(String groupId, String artifactId, String version) {
            Set<ModelSource> sources = mappedSources.get(new GAKey(groupId, artifactId));
            if (sources != null) {
                for (ModelSource source : sources) {
                    Model model = derive(source).readRawModel();
                    if (Objects.equals(model.getVersion(), version)) {
                        return source;
                    }
                }
                // TODO: log a warning ?
            }
            return null;
        }

        private <T> T cache(String groupId, String artifactId, String version, String tag, Callable<T> supplier) {
            return cache.computeIfAbsent(groupId, artifactId, version, tag, asSupplier(supplier));
        }

        private <T> T cache(Source source, String tag, Callable<T> supplier) {
            return cache.computeIfAbsent(source, tag, asSupplier(supplier));
        }
    }

    private List<Profile> interpolateActivations(
            List<Profile> profiles, DefaultProfileActivationContext context, ModelProblemCollector problems) {
        if (profiles.stream()
                .map(org.apache.maven.api.model.Profile::getActivation)
                .noneMatch(Objects::nonNull)) {
            return profiles;
        }
        final Interpolator xform = new RegexBasedInterpolator();
        xform.setCacheAnswers(true);
        Stream.of(context.getUserProperties(), context.getSystemProperties())
                .map(MapBasedValueSource::new)
                .forEach(xform::addValueSource);

        class ProfileInterpolator extends MavenTransformer implements UnaryOperator<Profile> {
            ProfileInterpolator() {
                super(s -> {
                    if (isNotEmpty(s)) {
                        try {
                            return xform.interpolate(s);
                        } catch (InterpolationException e) {
                            problems.add(Severity.ERROR, ModelProblem.Version.BASE, e.getMessage(), e);
                        }
                    }
                    return s;
                });
            }

            @Override
            public Profile apply(Profile p) {
                return Profile.newBuilder(p)
                        .activation(transformActivation(p.getActivation()))
                        .build();
            }

            @Override
            protected ActivationFile.Builder transformActivationFile_Missing(
                    Supplier<? extends ActivationFile.Builder> creator,
                    ActivationFile.Builder builder,
                    ActivationFile target) {
                String path = target.getMissing();
                String xformed = transformPath(path, target, "missing");
                return xformed != path ? (builder != null ? builder : creator.get()).missing(xformed) : builder;
            }

            @Override
            protected ActivationFile.Builder transformActivationFile_Exists(
                    Supplier<? extends ActivationFile.Builder> creator,
                    ActivationFile.Builder builder,
                    ActivationFile target) {
                final String path = target.getExists();
                final String xformed = transformPath(path, target, "exists");
                return xformed != path ? (builder != null ? builder : creator.get()).exists(xformed) : builder;
            }

            private String transformPath(String path, ActivationFile target, String locationKey) {
                if (isNotEmpty(path)) {
                    try {
                        return profileActivationFilePathInterpolator.interpolate(path, context);
                    } catch (InterpolationException e) {
                        problems.add(
                                Severity.ERROR,
                                ModelProblem.Version.BASE,
                                "Failed to interpolate file location " + path + ": " + e.getMessage(),
                                target.getLocation(locationKey),
                                e);
                    }
                }
                return path;
            }
        }
        return profiles.stream().map(new ProfileInterpolator()).toList();
    }

    private static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException {
        DefaultModelBuilderSession build = new DefaultModelBuilderSession(request);
        Model model = build.readRawModel();
        if (((ModelProblemCollector) build).hasErrors()) {
            throw build.newModelBuilderException();
        }
        return model;
    }

    static String getGroupId(Model model) {
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    static String getVersion(Model model) {
        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }
        return version;
    }

    private DefaultProfileActivationContext getProfileActivationContext(ModelBuilderRequest request, Model model) {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();

        context.setActiveProfileIds(request.getActiveProfileIds());
        context.setInactiveProfileIds(request.getInactiveProfileIds());
        context.setSystemProperties(request.getSystemProperties());
        // enrich user properties with project packaging
        Map<String, String> userProperties = new HashMap<>(request.getUserProperties());
        if (!userProperties.containsKey(ProfileActivationContext.PROPERTY_NAME_PACKAGING)) {
            userProperties.put(ProfileActivationContext.PROPERTY_NAME_PACKAGING, model.getPackaging());
        }
        context.setUserProperties(userProperties);
        context.setProjectDirectory(model.getProjectDirectory());

        return context;
    }

    private void checkPluginVersions(List<Model> lineage, ModelBuilderRequest request, ModelProblemCollector problems) {
        if (request.getRequestType() != ModelBuilderRequest.RequestType.BUILD_POM) {
            return;
        }

        Map<String, Plugin> plugins = new HashMap<>();
        Map<String, String> versions = new HashMap<>();
        Map<String, String> managedVersions = new HashMap<>();

        for (int i = lineage.size() - 1; i >= 0; i--) {
            Model model = lineage.get(i);
            Build build = model.getBuild();
            if (build != null) {
                for (Plugin plugin : build.getPlugins()) {
                    String key = plugin.getKey();
                    if (versions.get(key) == null) {
                        versions.put(key, plugin.getVersion());
                        plugins.put(key, plugin);
                    }
                }
                PluginManagement mgmt = build.getPluginManagement();
                if (mgmt != null) {
                    for (Plugin plugin : mgmt.getPlugins()) {
                        String key = plugin.getKey();
                        managedVersions.computeIfAbsent(key, k -> plugin.getVersion());
                    }
                }
            }
        }

        for (String key : versions.keySet()) {
            if (versions.get(key) == null && managedVersions.get(key) == null) {
                InputLocation location = plugins.get(key).getLocation("");
                problems.add(
                        Severity.WARNING,
                        ModelProblem.Version.V20,
                        "'build.plugins.plugin.version' for " + key + " is missing.",
                        location);
            }
        }
    }

    private Model assembleInheritance(
            List<Model> lineage, ModelBuilderRequest request, ModelProblemCollector problems) {
        Model parent = lineage.get(lineage.size() - 1);
        for (int i = lineage.size() - 2; i >= 0; i--) {
            Model child = lineage.get(i);
            parent = inheritanceAssembler.assembleModelInheritance(child, parent, request, problems);
        }
        return parent;
    }

    private Map<String, Activation> getProfileActivations(Model model) {
        return model.getProfiles().stream()
                .filter(p -> p.getActivation() != null)
                .collect(Collectors.toMap(Profile::getId, Profile::getActivation));
    }

    private Model injectProfileActivations(Model model, Map<String, Activation> activations) {
        List<Profile> profiles = new ArrayList<>();
        boolean modified = false;
        for (Profile profile : model.getProfiles()) {
            Activation activation = profile.getActivation();
            if (activation != null) {
                // restore activation
                profile = profile.withActivation(activations.get(profile.getId()));
                modified = true;
            }
            profiles.add(profile);
        }
        return modified ? model.withProfiles(profiles) : model;
    }

    private Model interpolateModel(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        Model interpolatedModel =
                modelInterpolator.interpolateModel(model, model.getProjectDirectory(), request, problems);
        if (interpolatedModel.getParent() != null) {
            StringSearchInterpolator ssi = new StringSearchInterpolator();
            ssi.addValueSource(new MapBasedValueSource(request.getSession().getUserProperties()));
            ssi.addValueSource(new MapBasedValueSource(model.getProperties()));
            ssi.addValueSource(new MapBasedValueSource(request.getSession().getSystemProperties()));
            try {
                String interpolated =
                        ssi.interpolate(interpolatedModel.getParent().getVersion());
                interpolatedModel = interpolatedModel.withParent(
                        interpolatedModel.getParent().withVersion(interpolated));
            } catch (Exception e) {
                problems.add(
                        Severity.ERROR,
                        ModelProblem.Version.BASE,
                        "Failed to interpolate field: "
                                + interpolatedModel.getParent().getVersion()
                                + " on class: ",
                        e);
            }
        }
        interpolatedModel = interpolatedModel.withPomFile(model.getPomFile());
        return interpolatedModel;
    }

    private boolean rawChildVersionReferencesParent(String rawChildModelVersion) {
        return rawChildModelVersion.equals("${pom.version}")
                || rawChildModelVersion.equals("${project.version}")
                || rawChildModelVersion.equals("${pom.parent.version}")
                || rawChildModelVersion.equals("${project.parent.version}");
    }

    private ModelSource getParentPomFile(Model childModel, ModelSource source) {
        String parentPath = childModel.getParent().getRelativePath();
        if (parentPath == null || parentPath.isEmpty()) {
            return null;
        } else {
            return source.resolve(modelProcessor::locateExistingPom, parentPath);
        }
    }

    private Model getSuperModel(String modelVersion) {
        return superPomProvider.getSuperPom(modelVersion);
    }

    private static org.apache.maven.api.model.Dependency addExclusions(
            org.apache.maven.api.model.Dependency candidate, List<Exclusion> exclusions) {
        return candidate.withExclusions(Stream.concat(candidate.getExclusions().stream(), exclusions.stream())
                .toList());
    }

    private boolean match(Exclusion exclusion, Dependency candidate) {
        return match(exclusion.getGroupId(), candidate.getGroupId())
                && match(exclusion.getArtifactId(), candidate.getArtifactId());
    }

    private boolean match(String match, String text) {
        return match.equals("*") || match.equals(text);
    }

    private static <T> Supplier<T> asSupplier(Callable<T> supplier) {
        return () -> {
            try {
                return supplier.call();
            } catch (Exception e) {
                uncheckedThrow(e);
                return null;
            }
        };
    }

    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t; // rely on vacuous cast
    }

    private boolean containsCoordinates(String message, String groupId, String artifactId, String version) {
        return message != null
                && (groupId == null || message.contains(groupId))
                && (artifactId == null || message.contains(artifactId))
                && (version == null || message.contains(version));
    }

    ModelProcessor getModelProcessor() {
        return modelProcessor;
    }

    record GAKey(String groupId, String artifactId) {}
}
