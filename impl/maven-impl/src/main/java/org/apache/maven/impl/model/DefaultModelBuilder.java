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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
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
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DeploymentRepository;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Mixin;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.services.model.DependencyManagementImporter;
import org.apache.maven.api.services.model.DependencyManagementInjector;
import org.apache.maven.api.services.model.InheritanceAssembler;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.ModelNormalizer;
import org.apache.maven.api.services.model.ModelPathTranslator;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.api.services.model.ModelUrlNormalizer;
import org.apache.maven.api.services.model.ModelValidator;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.api.services.model.PathTranslator;
import org.apache.maven.api.services.model.PluginConfigurationExpander;
import org.apache.maven.api.services.model.PluginManagementInjector;
import org.apache.maven.api.services.model.ProfileInjector;
import org.apache.maven.api.services.model.ProfileSelector;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.api.spi.ModelTransformer;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.RequestTraceHelper;
import org.apache.maven.impl.cache.Cache;
import org.apache.maven.impl.util.PhasingExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The model builder is responsible for building the {@link Model} from the POM file.
 * There are two ways to main use cases: the first one is to build the model from a POM file
 * on the file system in order to actually build the project. The second one is to build the
 * model for a dependency  or an external parent.
 */
@Named
@Singleton
public class DefaultModelBuilder implements ModelBuilder {

    public static final String NAMESPACE_PREFIX = "http://maven.apache.org/POM/";
    private static final String RAW = "raw";
    private static final String FILE = "file";
    private static final String IMPORT = "import";
    private static final String PARENT = "parent";
    private static final String MODEL = "model";

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
    private final ModelVersionParser versionParser;
    private final List<ModelTransformer> transformers;
    private final ModelResolver modelResolver;
    private final Interpolator interpolator;
    private final PathTranslator pathTranslator;
    private final RootLocator rootLocator;

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
            ModelVersionParser versionParser,
            @Nullable List<ModelTransformer> transformers,
            ModelResolver modelResolver,
            Interpolator interpolator,
            PathTranslator pathTranslator,
            RootLocator rootLocator) {
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
        this.versionParser = versionParser;
        this.transformers = transformers;
        this.modelResolver = modelResolver;
        this.interpolator = interpolator;
        this.pathTranslator = pathTranslator;
        this.rootLocator = rootLocator;
    }

    @Override
    public ModelBuilderSession newSession() {
        return new ModelBuilderSessionImpl();
    }

    protected class ModelBuilderSessionImpl implements ModelBuilderSession {
        ModelBuilderSessionState mainSession;

        /**
         * Builds a model based on the provided ModelBuilderRequest.
         *
         * @param request The request containing the parameters for building the model.
         * @return The result of the model building process.
         * @throws ModelBuilderException If an error occurs during model building.
         */
        @Override
        public ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException {
            RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(request.getSession(), request);
            try {
                // Create or derive a session based on the request
                ModelBuilderSessionState session;
                if (mainSession == null) {
                    mainSession = new ModelBuilderSessionState(request);
                    session = mainSession;
                } else {
                    session = mainSession.derive(
                            request,
                            new DefaultModelBuilderResult(request, ProblemCollector.create(mainSession.session)));
                }
                // Build the request
                if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT) {
                    // build the build poms
                    session.buildBuildPom();
                } else {
                    // simply build the effective model
                    session.buildEffectiveModel(new LinkedHashSet<>());
                }
                return session.result;
            } finally {
                // Clean up REQUEST_SCOPED cache entries to prevent memory leaks
                // This is especially important for BUILD_PROJECT requests which are top-level requests
                if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT) {
                    try {
                        clearRequestScopedCache(request);
                    } catch (Exception e) {
                        // Log but don't fail the build due to cache cleanup issues
                        logger.debug("Failed to clear REQUEST_SCOPED cache for request: {}", request, e);
                    }
                }
                RequestTraceHelper.exit(trace);
            }
        }
    }

    protected class ModelBuilderSessionState implements ModelProblemCollector {
        final Session session;
        final ModelBuilderRequest request;
        final DefaultModelBuilderResult result;
        final Graph dag;
        final Map<GAKey, Set<ModelSource>> mappedSources;

        String source;
        Model sourceModel;
        Model rootModel;

        List<RemoteRepository> pomRepositories;
        List<RemoteRepository> externalRepositories;
        List<RemoteRepository> repositories;

        // Cycle detection chain shared across all derived sessions
        // Contains both GAV coordinates (groupId:artifactId:version) and file paths
        final Set<String> parentChain;

        ModelBuilderSessionState(ModelBuilderRequest request) {
            this(
                    request.getSession(),
                    request,
                    new DefaultModelBuilderResult(request, ProblemCollector.create(request.getSession())),
                    new Graph(),
                    new ConcurrentHashMap<>(64),
                    List.of(),
                    repos(request),
                    repos(request),
                    new LinkedHashSet<>());
        }

        static List<RemoteRepository> repos(ModelBuilderRequest request) {
            return List.copyOf(
                    request.getRepositories() != null
                            ? request.getRepositories()
                            : request.getSession().getRemoteRepositories());
        }

        @SuppressWarnings("checkstyle:ParameterNumber")
        private ModelBuilderSessionState(
                Session session,
                ModelBuilderRequest request,
                DefaultModelBuilderResult result,
                Graph dag,
                Map<GAKey, Set<ModelSource>> mappedSources,
                List<RemoteRepository> pomRepositories,
                List<RemoteRepository> externalRepositories,
                List<RemoteRepository> repositories,
                Set<String> parentChain) {
            this.session = session;
            this.request = request;
            this.result = result;
            this.dag = dag;
            this.mappedSources = mappedSources;
            this.pomRepositories = pomRepositories;
            this.externalRepositories = externalRepositories;
            this.repositories = repositories;
            this.parentChain = parentChain;
            this.result.setSource(this.request.getSource());
        }

        ModelBuilderSessionState derive(ModelSource source) {
            return derive(source, new DefaultModelBuilderResult(request, ProblemCollector.create(session)));
        }

        ModelBuilderSessionState derive(ModelSource source, DefaultModelBuilderResult result) {
            return derive(ModelBuilderRequest.build(request, source), result);
        }

        /**
         * Creates a new session, sharing cached datas and propagating errors.
         */
        ModelBuilderSessionState derive(ModelBuilderRequest request) {
            return derive(request, new DefaultModelBuilderResult(request, ProblemCollector.create(session)));
        }

        ModelBuilderSessionState derive(ModelBuilderRequest request, DefaultModelBuilderResult result) {
            if (session != request.getSession()) {
                throw new IllegalArgumentException("Session mismatch");
            }
            // Create a new parentChain for each derived session to prevent cycle detection issues
            // The parentChain now contains both GAV coordinates and file paths
            return new ModelBuilderSessionState(
                    session,
                    request,
                    result,
                    dag,
                    mappedSources,
                    pomRepositories,
                    externalRepositories,
                    repositories,
                    new LinkedHashSet<>());
        }

        @Override
        public String toString() {
            return "ModelBuilderSession[" + "session="
                    + session + ", " + "request="
                    + request + ", " + "result="
                    + result + ']';
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
                if (addEdge(from, source.getPath())) {
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
            if (addEdge(from, path)) {
                return null;
            }
            try {
                return derive(Sources.buildSource(path)).readRawModel();
            } catch (ModelBuilderException e) {
                // gathered with problem collector
            }
            return null;
        }

        /**
         * Returns false if the edge was added, true if it caused a cycle.
         */
        private boolean addEdge(Path from, Path p) {
            try {
                dag.addEdge(from.toString(), p.toString());
                return false;
            } catch (Graph.CycleDetectedException e) {
                add(Severity.FATAL, Version.BASE, "Cycle detected between models at " + from + " and " + p, null, e);
                return true;
            }
        }

        public ModelSource getSource(String groupId, String artifactId) {
            Set<ModelSource> sources = mappedSources.get(new GAKey(groupId, artifactId));
            if (sources != null) {
                return sources.stream()
                        .reduce((a, b) -> {
                            throw new IllegalStateException(String.format(
                                    "No unique Source for %s:%s: %s and %s",
                                    groupId, artifactId, a.getLocation(), b.getLocation()));
                        })
                        .orElse(null);
            }
            return null;
        }

        public void putSource(String groupId, String artifactId, ModelSource source) {
            mappedSources
                    .computeIfAbsent(new GAKey(groupId, artifactId), k -> new HashSet<>())
                    .add(source);
            // Also  register the source under the null groupId
            if (groupId != null) {
                putSource(null, artifactId, source);
            }
        }

        @Override
        public ProblemCollector<ModelProblem> getProblemCollector() {
            return result.getProblemCollector();
        }

        @Override
        public void setSource(String source) {
            this.source = source;
            this.sourceModel = null;
        }

        @Override
        public void setSource(Model source) {
            this.sourceModel = source;
            this.source = null;

            if (rootModel == null) {
                rootModel = source;
            }
        }

        @Override
        public String getSource() {
            if (source == null && sourceModel != null) {
                source = ModelProblemUtils.toPath(sourceModel);
            }
            return source;
        }

        private String getModelId() {
            return ModelProblemUtils.toId(sourceModel);
        }

        @Override
        public void setRootModel(Model rootModel) {
            this.rootModel = rootModel;
        }

        @Override
        public Model getRootModel() {
            return rootModel;
        }

        @Override
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

        @Override
        public ModelBuilderException newModelBuilderException() {
            return new ModelBuilderException(result);
        }

        public void mergeRepositories(Model model, boolean replace) {
            if (model.getRepositories().isEmpty()
                    || InternalSession.from(session).getSession().isIgnoreArtifactDescriptorRepositories()) {
                return;
            }
            // We need to interpolate the repositories before we can use them
            Model interpolatedModel = interpolateModel(
                    Model.newBuilder()
                            .pomFile(model.getPomFile())
                            .properties(model.getProperties())
                            .repositories(model.getRepositories())
                            .build(),
                    request,
                    this);
            List<RemoteRepository> repos = interpolatedModel.getRepositories().stream()
                    .map(session::createRemoteRepository)
                    .toList();
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
        // Transform raw model to build pom.
        // Infer inner reactor dependencies version
        //
        Model transformFileToRaw(Model model) {
            if (model.getDependencies().isEmpty()) {
                return model;
            }
            List<Dependency> newDeps = new ArrayList<>(model.getDependencies().size());
            boolean changed = false;
            for (Dependency dep : model.getDependencies()) {
                Dependency newDep = null;
                if (dep.getVersion() == null) {
                    newDep = inferDependencyVersion(model, dep);
                    if (newDep != null) {
                        changed = true;
                    }
                } else if (dep.getGroupId() == null) {
                    // Handle missing groupId when version is present
                    newDep = inferDependencyGroupId(model, dep);
                    if (newDep != null) {
                        changed = true;
                    }
                }
                newDeps.add(newDep == null ? dep : newDep);
            }
            return changed ? model.withDependencies(newDeps) : model;
        }

        private Dependency inferDependencyVersion(Model model, Dependency dep) {
            Model depModel = getRawModel(model.getPomFile(), dep.getGroupId(), dep.getArtifactId());
            if (depModel == null) {
                return null;
            }
            Dependency.Builder depBuilder = Dependency.newBuilder(dep);
            String version = depModel.getVersion();
            InputLocation versionLocation = depModel.getLocation("version");
            if (version == null && depModel.getParent() != null) {
                version = depModel.getParent().getVersion();
                versionLocation = depModel.getParent().getLocation("version");
            }
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
            return depBuilder.build();
        }

        private Dependency inferDependencyGroupId(Model model, Dependency dep) {
            Model depModel = getRawModel(model.getPomFile(), dep.getGroupId(), dep.getArtifactId());
            if (depModel == null) {
                return null;
            }
            Dependency.Builder depBuilder = Dependency.newBuilder(dep);
            String depGroupId = depModel.getGroupId();
            InputLocation groupIdLocation = depModel.getLocation("groupId");
            if (depGroupId == null && depModel.getParent() != null) {
                depGroupId = depModel.getParent().getGroupId();
                groupIdLocation = depModel.getParent().getLocation("groupId");
            }
            depBuilder.groupId(depGroupId).location("groupId", groupIdLocation);
            return depBuilder.build();
        }

        String replaceCiFriendlyVersion(Map<String, String> properties, String version) {
            return version != null ? interpolator.interpolate(version, properties::get) : null;
        }

        /**
         * Get enhanced properties that include profile-aware property resolution.
         * This method activates profiles to ensure that properties defined in profiles
         * are available for CI-friendly version processing and repository URL interpolation.
         * It also includes directory-related properties that may be needed during profile activation.
         */
        private Map<String, String> getEnhancedProperties(Model model, Path rootDirectory) {
            Map<String, String> properties = new HashMap<>();

            // Add directory-specific properties first, as they may be needed for profile activation
            if (model.getProjectDirectory() != null) {
                String basedir = model.getProjectDirectory().toString();
                String basedirUri = model.getProjectDirectory().toUri().toString();
                properties.put("basedir", basedir);
                properties.put("project.basedir", basedir);
                properties.put("project.basedir.uri", basedirUri);
            }
            try {
                String root = rootDirectory.toString();
                String rootUri = rootDirectory.toUri().toString();
                properties.put("project.rootDirectory", root);
                properties.put("project.rootDirectory.uri", rootUri);
            } catch (IllegalStateException e) {
                // Root directory not available, continue without it
            }

            // Handle root vs non-root project properties with profile activation
            if (!Objects.equals(rootDirectory, model.getProjectDirectory())) {
                Path rootModelPath = modelProcessor.locateExistingPom(rootDirectory);
                if (rootModelPath != null) {
                    // Check if the root model path is within the root directory to prevent infinite loops
                    // This can happen when a .mvn directory exists in a subdirectory and parent inference
                    // tries to read models above the discovered root directory
                    if (isParentWithinRootDirectory(rootModelPath, rootDirectory)) {
                        Model rootModel =
                                derive(Sources.buildSource(rootModelPath)).readFileModel();
                        properties.putAll(getPropertiesWithProfiles(rootModel, properties));
                    }
                }
            } else {
                properties.putAll(getPropertiesWithProfiles(model, properties));
            }

            return properties;
        }

        /**
         * Get properties from a model including properties from activated profiles.
         * This performs lightweight profile activation to merge profile properties.
         *
         * @param model the model to get properties from
         * @param baseProperties base properties (including directory properties) to include in profile activation context
         */
        private Map<String, String> getPropertiesWithProfiles(Model model, Map<String, String> baseProperties) {
            Map<String, String> properties = new HashMap<>();

            // Start with base properties (including directory properties)
            properties.putAll(baseProperties);

            // Add model properties
            properties.putAll(model.getProperties());

            try {
                // Create a profile activation context for this model with base properties available
                DefaultProfileActivationContext profileContext = getProfileActivationContext(request, model);

                // Activate profiles and merge their properties
                List<Profile> activeProfiles = getActiveProfiles(model.getProfiles(), profileContext);

                for (Profile profile : activeProfiles) {
                    properties.putAll(profile.getProperties());
                }
            } catch (Exception e) {
                // If profile activation fails, log a warning but continue with base properties
                // This ensures that CI-friendly versions still work even if profile activation has issues
                logger.warn("Failed to activate profiles for CI-friendly version processing: {}", e.getMessage());
                logger.debug("Profile activation failure details", e);
            }

            // User properties override everything
            properties.putAll(session.getEffectiveProperties());

            return properties;
        }

        /**
         * Convenience method for getting properties with profiles without additional base properties.
         * This is a backward compatibility method that provides an empty base properties map.
         */
        private Map<String, String> getPropertiesWithProfiles(Model model) {
            return getPropertiesWithProfiles(model, new HashMap<>());
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
                rootDirectory = session.getService(RootLocator.class).findMandatoryRoot(top);
            }

            // Locate and normalize the root POM if it exists, fallback to top otherwise
            Path root = modelProcessor.locateExistingPom(rootDirectory);
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
            List<RuntimeException> exceptions = new CopyOnWriteArrayList<>();
            InternalSession session = InternalSession.from(this.session);
            RequestTrace trace = session.getCurrentTrace();
            try (PhasingExecutor executor = createExecutor()) {
                for (DefaultModelBuilderResult r : allResults) {
                    executor.execute(() -> {
                        ModelBuilderSessionState mbs = derive(r.getSource(), r);
                        session.setCurrentTrace(trace);
                        try {
                            mbs.buildEffectiveModel(new LinkedHashSet<>());
                        } catch (ModelBuilderException e) {
                            // gathered with problem collector
                            // Propagate problems from child session to parent session
                            for (var problem : e.getResult()
                                    .getProblemCollector()
                                    .problems()
                                    .toList()) {
                                getProblemCollector().reportProblem(problem);
                            }
                        } catch (RuntimeException t) {
                            exceptions.add(t);
                        } finally {
                            session.setCurrentTrace(null);
                        }
                    });
                }
            }

            // Check for errors again after execution
            if (exceptions.size() == 1) {
                throw exceptions.get(0);
            } else if (exceptions.size() > 1) {
                MavenException fatalException = new MavenException("Multiple fatal exceptions occurred");
                exceptions.forEach(fatalException::addSuppressed);
                throw fatalException;
            } else if (hasErrors()) {
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

        private void loadFromRoot(Path root, Path top) {
            try (PhasingExecutor executor = createExecutor()) {
                DefaultModelBuilderResult r = Objects.equals(top, root)
                        ? result
                        : new DefaultModelBuilderResult(request, ProblemCollector.create(session));
                loadFilePom(executor, top, root, Set.of(), r);
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
                mappedSources.clear();
                loadFromRoot(top, top);
            }
        }

        private void loadFilePom(
                Executor executor, Path top, Path pom, Set<Path> parents, DefaultModelBuilderResult r) {
            try {
                Path pomDirectory = Files.isDirectory(pom) ? pom : pom.getParent();
                ModelSource src = Sources.buildSource(pom);
                Model model = derive(src, r).readFileModel();
                // keep all loaded file models in memory, those will be needed
                // during the raw to build transformation
                putSource(getGroupId(model), model.getArtifactId(), src);
                Model activated = activateFileModel(model);
                for (String subproject : getSubprojects(activated)) {
                    if (subproject == null || subproject.isEmpty()) {
                        continue;
                    }

                    subproject = subproject.replace('\\', File.separatorChar).replace('/', File.separatorChar);

                    Path rawSubprojectFile = modelProcessor.locateExistingPom(pomDirectory.resolve(subproject));

                    if (rawSubprojectFile == null) {
                        ModelProblem problem = new DefaultModelProblem(
                                "Child subproject " + subproject + " of " + pomDirectory + " does not exist",
                                Severity.ERROR,
                                Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        r.getProblemCollector().reportProblem(problem);
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
                                Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        r.getProblemCollector().reportProblem(problem);
                        continue;
                    }

                    DefaultModelBuilderResult cr = Objects.equals(top, subprojectFile)
                            ? result
                            : new DefaultModelBuilderResult(request, ProblemCollector.create(session));
                    if (request.isRecursive()) {
                        r.getChildren().add(cr);
                    }

                    InternalSession session = InternalSession.from(this.session);
                    RequestTrace trace = session.getCurrentTrace();
                    executor.execute(() -> {
                        session.setCurrentTrace(trace);
                        try {
                            loadFilePom(executor, top, subprojectFile, concat(parents, pom), cr);
                        } finally {
                            session.setCurrentTrace(null);
                        }
                    });
                }
            } catch (ModelBuilderException e) {
                // gathered with problem collector
                add(Severity.ERROR, Version.V40, "Failed to load project " + pom, e);
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
            if (request.getRequestType() != ModelBuilderRequest.RequestType.CONSUMER_DEPENDENCY) {
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

            if (request.getRequestType() != ModelBuilderRequest.RequestType.CONSUMER_DEPENDENCY) {
                // plugins configuration
                resultModel = pluginConfigurationExpander.expandPluginConfiguration(resultModel, request, this);
            }

            for (var transformer : transformers) {
                resultModel = transformer.transformEffectiveModel(resultModel);
            }

            result.setEffectiveModel(resultModel);
            // Set the default relative path for the parent in the file model
            if (result.getFileModel().getParent() != null
                    && result.getFileModel().getParent().getRelativePath() == null) {
                result.setFileModel(result.getFileModel()
                        .withParent(result.getFileModel()
                                .getParent()
                                .withRelativePath(resultModel.getParent().getRelativePath())));
            }

            // effective model validation
            modelValidator.validateEffectiveModel(
                    session,
                    resultModel,
                    isBuildRequest() ? ModelValidator.VALIDATION_LEVEL_STRICT : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    this);

            if (hasErrors()) {
                throw newModelBuilderException();
            }
        }

        Model readParent(
                Model childModel,
                Parent parent,
                DefaultProfileActivationContext profileActivationContext,
                Set<String> parentChain) {
            Model parentModel;

            if (parent != null) {
                // Check for circular parent resolution using model IDs
                String parentId = parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion();
                if (!parentChain.add(parentId)) {
                    StringBuilder message = new StringBuilder("The parents form a cycle: ");
                    for (String id : parentChain) {
                        message.append(id).append(" -> ");
                    }
                    message.append(parentId);

                    add(Severity.FATAL, Version.BASE, message.toString());
                    throw newModelBuilderException();
                }

                try {
                    parentModel = resolveParent(childModel, parent, profileActivationContext, parentChain);

                    if (!"pom".equals(parentModel.getPackaging())) {
                        add(
                                Severity.ERROR,
                                Version.BASE,
                                "Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                        + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"",
                                parentModel.getLocation("packaging"));
                    }
                    result.setParentModel(parentModel);

                    // Recursively read the parent's parent
                    if (parentModel.getParent() != null) {
                        readParent(parentModel, parentModel.getParent(), profileActivationContext, parentChain);
                    }
                } finally {
                    // Remove from chain when done processing this parent
                    parentChain.remove(parentId);
                }
            } else {
                String superModelVersion = childModel.getModelVersion();
                if (superModelVersion == null || !KNOWN_MODEL_VERSIONS.contains(superModelVersion)) {
                    // Maven 3.x is always using 4.0.0 version to load the supermodel, so
                    // do the same when loading a dependency.  The model validator will also
                    // check that field later.
                    superModelVersion = MODEL_VERSION_4_0_0;
                }
                parentModel = getSuperModel(superModelVersion);
            }

            return parentModel;
        }

        private Model resolveParent(
                Model childModel,
                Parent parent,
                DefaultProfileActivationContext profileActivationContext,
                Set<String> parentChain)
                throws ModelBuilderException {
            Model parentModel = null;
            if (isBuildRequest()) {
                parentModel = readParentLocally(childModel, parent, profileActivationContext, parentChain);
            }
            if (parentModel == null) {
                parentModel = resolveAndReadParentExternally(childModel, parent, profileActivationContext, parentChain);
            }
            return parentModel;
        }

        private Model readParentLocally(
                Model childModel,
                Parent parent,
                DefaultProfileActivationContext profileActivationContext,
                Set<String> parentChain)
                throws ModelBuilderException {
            ModelSource candidateSource;

            boolean isParentOrSimpleMixin = !(parent instanceof Mixin)
                    || (((Mixin) parent).getClassifier() == null && ((Mixin) parent).getExtension() == null);
            String parentPath = parent.getRelativePath();
            if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT) {
                if (parentPath != null && !parentPath.isEmpty()) {
                    candidateSource = request.getSource().resolve(modelProcessor::locateExistingPom, parentPath);
                    if (candidateSource == null) {
                        wrongParentRelativePath(childModel);
                        return null;
                    }
                } else if (isParentOrSimpleMixin) {
                    candidateSource =
                            resolveReactorModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                    if (candidateSource == null && parentPath == null) {
                        candidateSource = request.getSource().resolve(modelProcessor::locateExistingPom, "..");
                    }
                } else {
                    candidateSource = null;
                }
            } else if (isParentOrSimpleMixin) {
                candidateSource = resolveReactorModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                if (candidateSource == null) {
                    if (parentPath == null) {
                        parentPath = "..";
                    }
                    if (!parentPath.isEmpty()) {
                        candidateSource = request.getSource().resolve(modelProcessor::locateExistingPom, parentPath);
                    }
                }
            } else {
                candidateSource = null;
            }

            if (candidateSource == null) {
                return null;
            }

            // Check for circular parent resolution using source locations (file paths)
            // This must be done BEFORE calling derive() to prevent StackOverflowError
            String sourceLocation = candidateSource.getLocation();

            if (!parentChain.add(sourceLocation)) {
                StringBuilder message = new StringBuilder("The parents form a cycle: ");
                for (String location : parentChain) {
                    message.append(location).append(" -> ");
                }
                message.append(sourceLocation);

                add(Severity.FATAL, Version.BASE, message.toString());
                throw newModelBuilderException();
            }

            try {
                ModelBuilderSessionState derived = derive(candidateSource);
                Model candidateModel = derived.readAsParentModel(profileActivationContext, parentChain);
                addActivePomProfiles(derived.result.getActivePomProfiles());

                String groupId = getGroupId(candidateModel);
                String artifactId = candidateModel.getArtifactId();
                String version = getVersion(candidateModel);

                // Ensure that relative path and GA match, if both are provided
                if (parent.getGroupId() != null && (groupId == null || !groupId.equals(parent.getGroupId()))
                        || parent.getArtifactId() != null
                                && (artifactId == null || !artifactId.equals(parent.getArtifactId()))) {
                    mismatchRelativePathAndGA(childModel, parent, groupId, artifactId);
                    return null;
                }

                if (version != null && parent.getVersion() != null && !version.equals(parent.getVersion())) {
                    try {
                        VersionRange parentRange = versionParser.parseVersionRange(parent.getVersion());
                        if (!parentRange.contains(versionParser.parseVersion(version))) {
                            // version skew drop back to resolution from the repository
                            return null;
                        }

                        // Validate versions aren't inherited when using parent ranges the same way as when read
                        // externally.
                        String rawChildModelVersion = childModel.getVersion();

                        if (rawChildModelVersion == null) {
                            // Message below is checked for in the MNG-2199 core IT.
                            add(Severity.FATAL, Version.V31, "Version must be a constant", childModel.getLocation(""));

                        } else {
                            if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                                // Message below is checked for in the MNG-2199 core IT.
                                add(
                                        Severity.FATAL,
                                        Version.V31,
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
                return candidateModel;
            } finally {
                // Remove the source location from the chain when we're done processing this parent
                parentChain.remove(sourceLocation);
            }
        }

        private void mismatchRelativePathAndGA(Model childModel, Parent parent, String groupId, String artifactId) {
            StringBuilder buffer = new StringBuilder(256);
            buffer.append("'parent.relativePath'");
            if (childModel != getRootModel()) {
                buffer.append(" of POM ").append(ModelProblemUtils.toSourceHint(childModel));
            }
            buffer.append(" points at ").append(groupId).append(':').append(artifactId);
            buffer.append(" instead of ").append(parent.getGroupId()).append(':');
            buffer.append(parent.getArtifactId()).append(", please verify your project structure");

            setSource(childModel);
            boolean warn = MODEL_VERSION_4_0_0.equals(childModel.getModelVersion())
                    || childModel.getParent().getRelativePath() == null;
            add(warn ? Severity.WARNING : Severity.FATAL, Version.BASE, buffer.toString(), parent.getLocation(""));
        }

        private void wrongParentRelativePath(Model childModel) {
            Parent parent = childModel.getParent();
            String parentPath = parent.getRelativePath();
            StringBuilder buffer = new StringBuilder(256);
            buffer.append("'parent.relativePath'");
            if (childModel != getRootModel()) {
                buffer.append(" of POM ").append(ModelProblemUtils.toSourceHint(childModel));
            }
            buffer.append(" points at '").append(parentPath);
            buffer.append("' but no POM could be found, please verify your project structure");

            setSource(childModel);
            add(Severity.FATAL, Version.BASE, buffer.toString(), parent.getLocation(""));
        }

        Model resolveAndReadParentExternally(
                Model childModel,
                Parent parent,
                DefaultProfileActivationContext profileActivationContext,
                Set<String> parentChain)
                throws ModelBuilderException {
            ModelBuilderRequest request = this.request;
            setSource(childModel);

            String groupId = parent.getGroupId();
            String artifactId = parent.getArtifactId();
            String version = parent.getVersion();
            String classifier = parent instanceof Mixin ? ((Mixin) parent).getClassifier() : null;
            String extension = parent instanceof Mixin ? ((Mixin) parent).getExtension() : null;

            // add repositories specified by the current model so that we can resolve the parent
            if (!childModel.getRepositories().isEmpty()) {
                var previousRepositories = repositories;
                mergeRepositories(childModel, false);
                if (!Objects.equals(previousRepositories, repositories)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Merging repositories from " + childModel.getId() + "\n"
                                + repositories.stream()
                                        .map(Object::toString)
                                        .collect(Collectors.joining("\n", "    ", "")));
                    }
                }
            }

            ModelSource modelSource;
            try {
                modelSource = classifier == null && extension == null
                        ? resolveReactorModel(groupId, artifactId, version)
                        : null;
                if (modelSource == null) {
                    ModelResolver.ModelResolverRequest req = new ModelResolver.ModelResolverRequest(
                            request.getSession(),
                            null,
                            repositories,
                            groupId,
                            artifactId,
                            version,
                            classifier,
                            extension != null ? extension : "pom");
                    ModelResolver.ModelResolverResult result = modelResolver.resolveModel(req);
                    modelSource = result.source();
                    if (result.version() != null) {
                        parent = parent.withVersion(result.version());
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
                if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT) {
                    buffer.append(" and parent could not be found in reactor");
                }

                add(Severity.FATAL, Version.BASE, buffer.toString(), parent.getLocation(""), e);
                throw newModelBuilderException();
            }

            ModelBuilderRequest lenientRequest = ModelBuilderRequest.builder(request)
                    .requestType(ModelBuilderRequest.RequestType.CONSUMER_PARENT)
                    .source(modelSource)
                    .build();

            Model parentModel = derive(lenientRequest).readAsParentModel(profileActivationContext, parentChain);

            if (!parent.getVersion().equals(version)) {
                String rawChildModelVersion = childModel.getVersion();

                if (rawChildModelVersion == null) {
                    // Message below is checked for in the MNG-2199 core IT.
                    add(Severity.FATAL, Version.V31, "Version must be a constant", childModel.getLocation(""));
                } else {
                    if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                        // Message below is checked for in the MNG-2199 core IT.
                        add(
                                Severity.FATAL,
                                Version.V31,
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
            List<Profile> activeExternalProfiles = getActiveProfiles(request.getProfiles(), profileActivationContext);

            result.setActiveExternalProfiles(activeExternalProfiles);

            if (!activeExternalProfiles.isEmpty()) {
                Map<String, String> profileProps = new HashMap<>();
                for (Profile profile : activeExternalProfiles) {
                    profileProps.putAll(profile.getProperties());
                }
                profileProps.putAll(request.getUserProperties());
                profileActivationContext.setUserProperties(profileProps);
            }

            profileActivationContext.setModel(inputModel);
            setSource(inputModel);
            List<Profile> activePomProfiles = getActiveProfiles(inputModel.getProfiles(), profileActivationContext);

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

            setRootModel(inputModel);

            Model activatedFileModel = activateFileModel(inputModel);

            // profile activation
            DefaultProfileActivationContext profileActivationContext =
                    getProfileActivationContext(request, activatedFileModel);

            List<Profile> activeExternalProfiles = result.getActiveExternalProfiles();

            if (!activeExternalProfiles.isEmpty()) {
                Map<String, String> profileProps = new HashMap<>();
                for (Profile profile : activeExternalProfiles) {
                    profileProps.putAll(profile.getProperties());
                }
                profileProps.putAll(request.getUserProperties());
                profileActivationContext.setUserProperties(profileProps);
            }

            Model parentModel = readParent(
                    activatedFileModel, activatedFileModel.getParent(), profileActivationContext, parentChain);

            // Now that we have read the parent, we can set the relative
            // path correctly if it was not set in the input model
            if (inputModel.getParent() != null && inputModel.getParent().getRelativePath() == null) {
                String relPath;
                if (parentModel.getPomFile() != null && isBuildRequest()) {
                    relPath = inputModel
                            .getPomFile()
                            .getParent()
                            .toAbsolutePath()
                            .relativize(
                                    parentModel.getPomFile().toAbsolutePath().getParent())
                            .toString();
                } else {
                    relPath = "..";
                }
                inputModel = inputModel.withParent(inputModel.getParent().withRelativePath(relPath));
            }

            Model model = inheritanceAssembler.assembleModelInheritance(inputModel, parentModel, request, this);

            // Mixins
            for (Mixin mixin : model.getMixins()) {
                Model parent = resolveParent(model, mixin, profileActivationContext, parentChain);
                model = inheritanceAssembler.assembleModelInheritance(model, parent, request, this);
            }

            // model normalization
            model = modelNormalizer.mergeDuplicates(model, request, this);

            // profile activation
            profileActivationContext.setModel(model);

            // profile injection
            List<Profile> activePomProfiles = getActiveProfiles(model.getProfiles(), profileActivationContext);
            model = profileInjector.injectProfiles(model, activePomProfiles, request, this);
            model = profileInjector.injectProfiles(model, activeExternalProfiles, request, this);

            addActivePomProfiles(activePomProfiles);

            // model interpolation
            Model resultModel = model;
            resultModel = interpolateModel(resultModel, request, this);

            // model normalization
            resultModel = modelNormalizer.mergeDuplicates(resultModel, request, this);

            // url normalization
            resultModel = modelUrlNormalizer.normalize(resultModel, request);

            // Now the fully interpolated model is available: reconfigure the resolver
            if (!resultModel.getRepositories().isEmpty()) {
                List<String> oldRepos =
                        repositories.stream().map(Object::toString).toList();
                mergeRepositories(resultModel, true);
                List<String> newRepos =
                        repositories.stream().map(Object::toString).toList();
                if (!Objects.equals(oldRepos, newRepos)) {
                    logger.debug("Replacing repositories from " + resultModel.getId() + "\n"
                            + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
                }
            }

            return resultModel;
        }

        private void addActivePomProfiles(List<Profile> activePomProfiles) {
            if (activePomProfiles != null) {
                if (result.getActivePomProfiles() == null) {
                    result.setActivePomProfiles(new ArrayList<>());
                }
                result.getActivePomProfiles().addAll(activePomProfiles);
            }
        }

        private List<Profile> getActiveProfiles(
                Collection<Profile> interpolatedProfiles, DefaultProfileActivationContext profileActivationContext) {
            if (isBuildRequestWithActivation()) {
                return profileSelector.getActiveProfiles(interpolatedProfiles, profileActivationContext, this);
            } else {
                return List.of();
            }
        }

        Model readFileModel() throws ModelBuilderException {
            Model model = cache(request.getSource(), FILE, this::doReadFileModel);
            // set the file model in the result outside the cache
            result.setFileModel(model);
            return model;
        }

        @SuppressWarnings("checkstyle:methodlength")
        Model doReadFileModel() throws ModelBuilderException {
            ModelSource modelSource = request.getSource();
            Model model;
            Path rootDirectory;
            setSource(modelSource.getLocation());
            logger.debug("Reading file model from " + modelSource.getLocation());
            try {
                boolean strict = isBuildRequest();
                try {
                    rootDirectory = request.getSession().getRootDirectory();
                } catch (IllegalStateException ignore) {
                    rootDirectory = modelSource.getPath();
                    while (rootDirectory != null && !Files.isDirectory(rootDirectory)) {
                        rootDirectory = rootDirectory.getParent();
                    }
                }
                try (InputStream is = modelSource.openStream()) {
                    model = modelProcessor.read(XmlReaderRequest.builder()
                            .strict(strict)
                            .location(modelSource.getLocation())
                            .modelId(modelSource.getModelId())
                            .path(modelSource.getPath())
                            .rootDirectory(rootDirectory)
                            .inputStream(is)
                            .transformer(new InterningTransformer(session))
                            .build());
                } catch (XmlReaderException e) {
                    if (!strict) {
                        throw e;
                    }
                    try (InputStream is = modelSource.openStream()) {
                        model = modelProcessor.read(XmlReaderRequest.builder()
                                .strict(false)
                                .location(modelSource.getLocation())
                                .modelId(modelSource.getModelId())
                                .path(modelSource.getPath())
                                .rootDirectory(rootDirectory)
                                .inputStream(is)
                                .transformer(new InterningTransformer(session))
                                .build());
                    } catch (XmlReaderException ne) {
                        // still unreadable even in non-strict mode, rethrow original error
                        throw e;
                    }

                    add(
                            Severity.ERROR,
                            Version.V20,
                            "Malformed POM " + modelSource.getLocation() + ": " + e.getMessage(),
                            e);
                }
            } catch (XmlReaderException e) {
                add(
                        Severity.FATAL,
                        Version.BASE,
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
                add(Severity.FATAL, Version.BASE, "Non-readable POM " + modelSource.getLocation() + ": " + msg, e);
                throw newModelBuilderException();
            }

            if (model.getModelVersion() == null) {
                String namespace = model.getNamespaceUri();
                if (namespace != null && namespace.startsWith(NAMESPACE_PREFIX)) {
                    model = model.withModelVersion(namespace.substring(NAMESPACE_PREFIX.length()));
                }
            }

            if (isBuildRequest()) {
                model = model.withPomFile(modelSource.getPath());

                Parent parent = model.getParent();
                if (parent != null) {
                    String groupId = parent.getGroupId();
                    String artifactId = parent.getArtifactId();
                    String version = parent.getVersion();
                    String path = parent.getRelativePath();
                    if ((groupId == null || artifactId == null || version == null)
                            && (path == null || !path.isEmpty())) {
                        Path pomFile = model.getPomFile();
                        Path relativePath = Paths.get(path != null ? path : "..");
                        Path pomPath = pomFile.resolveSibling(relativePath).normalize();
                        if (Files.isDirectory(pomPath)) {
                            pomPath = modelProcessor.locateExistingPom(pomPath);
                        }
                        if (pomPath != null && Files.isRegularFile(pomPath)) {
                            // Check if parent POM is above the root directory
                            if (!isParentWithinRootDirectory(pomPath, rootDirectory)) {
                                add(
                                        Severity.FATAL,
                                        Version.BASE,
                                        "Parent POM " + pomPath + " is located above the root directory "
                                                + rootDirectory
                                                + ". This setup is invalid when a .mvn directory exists in a subdirectory.",
                                        parent.getLocation("relativePath"));
                                throw newModelBuilderException();
                            }

                            Model parentModel =
                                    derive(Sources.buildSource(pomPath)).readFileModel();
                            String parentGroupId = getGroupId(parentModel);
                            String parentArtifactId = parentModel.getArtifactId();
                            String parentVersion = getVersion(parentModel);
                            if ((groupId == null || groupId.equals(parentGroupId))
                                    && (artifactId == null || artifactId.equals(parentArtifactId))
                                    && (version == null || version.equals(parentVersion))) {
                                model = model.withParent(parent.with()
                                        .groupId(parentGroupId)
                                        .artifactId(parentArtifactId)
                                        .version(parentVersion)
                                        .build());
                            } else {
                                mismatchRelativePathAndGA(model, parent, parentGroupId, parentArtifactId);
                            }
                        } else {
                            if (!MODEL_VERSION_4_0_0.equals(model.getModelVersion()) && path != null) {
                                wrongParentRelativePath(model);
                            }
                        }
                    }
                }

                // subprojects discovery
                if (!hasSubprojectsDefined(model)
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
                        add(Severity.FATAL, Version.V41, "Error discovering subprojects", e);
                    }
                }

                // Enhanced property resolution with profile activation for CI-friendly versions and repository URLs
                // This includes directory properties, profile properties, and user properties
                Map<String, String> properties = getEnhancedProperties(model, rootDirectory);

                // CI friendly version processing with profile-aware properties
                model = model.with()
                        .version(replaceCiFriendlyVersion(properties, model.getVersion()))
                        .parent(
                                model.getParent() != null
                                        ? model.getParent()
                                                .withVersion(replaceCiFriendlyVersion(
                                                        properties,
                                                        model.getParent().getVersion()))
                                        : null)
                        .build();

                // Repository URL interpolation with the same profile-aware properties
                UnaryOperator<String> callback = properties::get;
                model = model.with()
                        .repositories(interpolateRepository(model.getRepositories(), callback))
                        .pluginRepositories(interpolateRepository(model.getPluginRepositories(), callback))
                        .profiles(map(model.getProfiles(), this::interpolateRepository, callback))
                        .distributionManagement(interpolateRepository(model.getDistributionManagement(), callback))
                        .build();
                // Override model properties with user properties
                Map<String, String> newProps = merge(model.getProperties(), session.getUserProperties());
                if (newProps != null) {
                    model = model.withProperties(newProps);
                }
                model = model.withProfiles(merge(model.getProfiles(), session.getUserProperties()));
            }

            for (var transformer : transformers) {
                model = transformer.transformFileModel(model);
            }

            setSource(model);
            modelValidator.validateFileModel(
                    session,
                    model,
                    isBuildRequest() ? ModelValidator.VALIDATION_LEVEL_STRICT : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    this);
            InternalSession internalSession = InternalSession.from(session);
            if (Features.mavenMaven3Personality(internalSession.getSession().getConfigProperties())
                    && Objects.equals(ModelBuilder.MODEL_VERSION_4_1_0, model.getModelVersion())) {
                add(Severity.FATAL, Version.BASE, "Maven3 mode: no higher model version than 4.0.0 allowed");
            }
            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            return model;
        }

        private DistributionManagement interpolateRepository(
                DistributionManagement distributionManagement, UnaryOperator<String> callback) {
            return distributionManagement == null
                    ? null
                    : distributionManagement
                            .with()
                            .repository((DeploymentRepository)
                                    interpolateRepository(distributionManagement.getRepository(), callback))
                            .snapshotRepository((DeploymentRepository)
                                    interpolateRepository(distributionManagement.getSnapshotRepository(), callback))
                            .build();
        }

        private Profile interpolateRepository(Profile profile, UnaryOperator<String> callback) {
            return profile == null
                    ? null
                    : profile.with()
                            .repositories(interpolateRepository(profile.getRepositories(), callback))
                            .pluginRepositories(interpolateRepository(profile.getPluginRepositories(), callback))
                            .build();
        }

        private List<Repository> interpolateRepository(List<Repository> repositories, UnaryOperator<String> callback) {
            return map(repositories, this::interpolateRepository, callback);
        }

        private Repository interpolateRepository(Repository repository, UnaryOperator<String> callback) {
            return repository == null
                    ? null
                    : repository
                            .with()
                            .id(interpolator.interpolate(repository.getId(), callback))
                            .url(interpolator.interpolate(repository.getUrl(), callback))
                            .build();
        }

        /**
         * Merges a list of model profiles with user-defined properties.
         * For each property defined in both the model and user properties, the user property value
         * takes precedence and overrides the model value.
         *
         * @param profiles list of profiles from the model
         * @param userProperties map of user-defined properties that override model properties
         * @return a new list containing profiles with overridden properties if changes were made,
         *         or the original list if no overrides were needed
         */
        List<Profile> merge(List<Profile> profiles, Map<String, String> userProperties) {
            List<Profile> result = null;
            for (int i = 0; i < profiles.size(); i++) {
                Profile profile = profiles.get(i);
                Map<String, String> props = merge(profile.getProperties(), userProperties);
                if (props != null) {
                    Profile merged = profile.withProperties(props);
                    if (result == null) {
                        result = new ArrayList<>(profiles);
                    }
                    result.set(i, merged);
                }
            }
            return result != null ? result : profiles;
        }

        /**
         * Merges model properties with user properties, giving precedence to user properties.
         * For any property key present in both maps, the user property value will override
         * the model property value when they differ.
         *
         * @param properties properties defined in the model
         * @param userProperties user-defined properties that override model properties
         * @return a new map with model properties overridden by user properties if changes were needed,
         *         or null if no overrides were needed
         */
        Map<String, String> merge(Map<String, String> properties, Map<String, String> userProperties) {
            Map<String, String> result = null;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = userProperties.get(key);
                if (value != null && !Objects.equals(value, entry.getValue())) {
                    if (result == null) {
                        result = new LinkedHashMap<>(properties);
                    }
                    result.put(entry.getKey(), value);
                }
            }
            return result;
        }

        Model readRawModel() throws ModelBuilderException {
            // ensure file model is available
            readFileModel();
            Model model = cache(request.getSource(), RAW, this::doReadRawModel);
            // set the raw model in the result outside the cache
            result.setRawModel(model);
            return model;
        }

        private Model doReadRawModel() throws ModelBuilderException {
            Model rawModel = readFileModel();

            if (!MODEL_VERSION_4_0_0.equals(rawModel.getModelVersion()) && isBuildRequest()) {
                rawModel = transformFileToRaw(rawModel);
            }

            for (var transformer : transformers) {
                rawModel = transformer.transformRawModel(rawModel);
            }

            modelValidator.validateRawModel(
                    session,
                    rawModel,
                    isBuildRequest() ? ModelValidator.VALIDATION_LEVEL_STRICT : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    this);

            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            return rawModel;
        }

        /**
         * Record to store both the parent model and its activated profiles for caching.
         */
        private record ParentModelWithProfiles(Model model, List<Profile> activatedProfiles) {}

        /**
         * Reads the request source's parent with cycle detection.
         */
        Model readAsParentModel(DefaultProfileActivationContext profileActivationContext, Set<String> parentChain)
                throws ModelBuilderException {
            Map<DefaultProfileActivationContext.Record, ParentModelWithProfiles> parentsPerContext =
                    cache(request.getSource(), PARENT, ConcurrentHashMap::new);

            for (Map.Entry<DefaultProfileActivationContext.Record, ParentModelWithProfiles> e :
                    parentsPerContext.entrySet()) {
                if (e.getKey().matches(profileActivationContext)) {
                    ParentModelWithProfiles cached = e.getValue();
                    // CRITICAL: On cache hit, we need to replay the cached record's keys into the
                    // current recording context. The matches() method already re-evaluated the
                    // conditions and recorded some keys in ctx, but we also need to ensure all
                    // the keys from the cached record are recorded in the current context.
                    if (profileActivationContext.record != null) {
                        replayRecordIntoContext(e.getKey(), profileActivationContext);
                    }
                    // Add the activated profiles from cache to the result
                    addActivePomProfiles(cached.activatedProfiles());
                    return cached.model();
                }
            }

            // Cache miss: process the parent model
            // CRITICAL: Use a separate recording context to avoid recording intermediate keys
            // that aren't essential to the final result. Only replay the final essential keys
            // into the parent recording context to maintain clean cache keys and avoid
            // over-recording during parent model processing.
            DefaultProfileActivationContext ctx = profileActivationContext.start();
            ParentModelWithProfiles modelWithProfiles = doReadAsParentModel(ctx, parentChain);
            DefaultProfileActivationContext.Record record = ctx.stop();
            replayRecordIntoContext(record, profileActivationContext);

            parentsPerContext.put(record, modelWithProfiles);
            addActivePomProfiles(modelWithProfiles.activatedProfiles());
            return modelWithProfiles.model();
        }

        private ParentModelWithProfiles doReadAsParentModel(
                DefaultProfileActivationContext childProfileActivationContext, Set<String> parentChain)
                throws ModelBuilderException {
            Model raw = readRawModel();
            Model parentData = readParent(raw, raw.getParent(), childProfileActivationContext, parentChain);
            DefaultInheritanceAssembler defaultInheritanceAssembler =
                    new DefaultInheritanceAssembler(new DefaultInheritanceAssembler.InheritanceModelMerger() {
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
                    });
            Model parent = defaultInheritanceAssembler.assembleModelInheritance(raw, parentData, request, this);
            for (Mixin mixin : parent.getMixins()) {
                Model parentModel = resolveParent(parent, mixin, childProfileActivationContext, parentChain);
                parent = defaultInheritanceAssembler.assembleModelInheritance(parent, parentModel, request, this);
            }

            // Profile injection SHOULD be performed on parent models to ensure
            // that profile content becomes part of the parent model before inheritance.
            // This ensures proper precedence: child elements override parent elements,
            // including elements that came from parent profiles.
            //
            // Use the child's activation context (passed as parameter) to determine
            // which parent profiles should be active, ensuring consistency.
            List<Profile> parentActivePomProfiles =
                    getActiveProfiles(parent.getProfiles(), childProfileActivationContext);

            // Inject profiles into parent model
            Model injectedParentModel = profileInjector
                    .injectProfiles(parent, parentActivePomProfiles, request, this)
                    .withProfiles(List.of()); // Remove profiles after injection to avoid double-processing

            // Note: addActivePomProfiles() will be called by the caller for cache miss case
            return new ParentModelWithProfiles(injectedParentModel.withParent(null), parentActivePomProfiles);
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
                        Version.BASE,
                        "'dependencyManagement.dependencies.dependency.groupId' for " + dependency.getManagementKey()
                                + " is missing.",
                        dependency.getLocation(""));
                return null;
            }
            if (artifactId == null || artifactId.isEmpty()) {
                add(
                        Severity.ERROR,
                        Version.BASE,
                        "'dependencyManagement.dependencies.dependency.artifactId' for " + dependency.getManagementKey()
                                + " is missing.",
                        dependency.getLocation(""));
                return null;
            }
            if (version == null || version.isEmpty()) {
                add(
                        Severity.ERROR,
                        Version.BASE,
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
                add(Severity.ERROR, Version.BASE, message.toString());
                return null;
            }

            Model importModel = cache(
                    repositories,
                    groupId,
                    artifactId,
                    version,
                    null,
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
                            request.getSession(), repositories, dependency, new AtomicReference<>());
                }
            } catch (ModelBuilderException | ModelResolverException e) {
                StringBuilder buffer = new StringBuilder(256);
                buffer.append("Non-resolvable import POM");
                if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                    buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
                }
                buffer.append(": ").append(e.getMessage());

                add(Severity.ERROR, Version.BASE, buffer.toString(), dependency.getLocation(""), e);
                return null;
            }

            Path rootDirectory;
            try {
                rootDirectory = request.getSession().getRootDirectory();
            } catch (IllegalStateException e) {
                rootDirectory = null;
            }
            if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT && rootDirectory != null) {
                Path sourcePath = importSource.getPath();
                if (sourcePath != null && sourcePath.startsWith(rootDirectory)) {
                    add(
                            Severity.WARNING,
                            Version.BASE,
                            "BOM imports from within reactor should be avoided",
                            dependency.getLocation(""));
                }
            }

            final ModelBuilderResult importResult;
            try {
                ModelBuilderRequest importRequest = ModelBuilderRequest.builder()
                        .session(request.getSession())
                        .requestType(ModelBuilderRequest.RequestType.CONSUMER_DEPENDENCY)
                        .systemProperties(request.getSystemProperties())
                        .userProperties(request.getUserProperties())
                        .source(importSource)
                        .repositories(repositories)
                        .build();
                ModelBuilderSessionState modelBuilderSession = derive(importRequest);
                // build the effective model
                modelBuilderSession.buildEffectiveModel(importIds);
                importResult = modelBuilderSession.result;
            } catch (ModelBuilderException e) {
                return null;
            }

            importModel = importResult.getEffectiveModel();

            return importModel;
        }

        ModelSource resolveReactorModel(String groupId, String artifactId, String version)
                throws ModelBuilderException {
            Set<ModelSource> sources = mappedSources.get(new GAKey(groupId, artifactId));
            if (sources != null) {
                for (ModelSource source : sources) {
                    Model model = derive(source).readRawModel();
                    if (Objects.equals(getVersion(model), version)) {
                        return source;
                    }
                }
                // TODO: log a warning ?
            }
            return null;
        }

        private <T> T cache(
                List<RemoteRepository> repositories,
                String groupId,
                String artifactId,
                String version,
                String classifier,
                String tag,
                Supplier<T> supplier) {
            RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
            try {
                RgavCacheKey r = new RgavCacheKey(
                        session, trace.mvnTrace(), repositories, groupId, artifactId, version, classifier, tag);
                SourceResponse<RgavCacheKey, T> response =
                        InternalSession.from(session).request(r, tr -> new SourceResponse<>(tr, supplier.get()));
                return response.response;
            } finally {
                RequestTraceHelper.exit(trace);
            }
        }

        private <T> T cache(Source source, String tag, Supplier<T> supplier) throws ModelBuilderException {
            RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
            try {
                SourceCacheKey r = new SourceCacheKey(session, trace.mvnTrace(), source, tag);
                SourceResponse<SourceCacheKey, T> response =
                        InternalSession.from(session).request(r, tr -> new SourceResponse<>(tr, supplier.get()));
                return response.response;
            } finally {
                RequestTraceHelper.exit(trace);
            }
        }

        boolean isBuildRequest() {
            return request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT
                    || request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_EFFECTIVE
                    || request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_CONSUMER;
        }

        boolean isBuildRequestWithActivation() {
            return request.getRequestType() != ModelBuilderRequest.RequestType.BUILD_CONSUMER;
        }

        /**
         * Replays the keys from a cached record into the current recording context.
         * This ensures that when there's a cache hit, all the keys that were originally
         * accessed during the cached computation are recorded in the current context.
         */
        private void replayRecordIntoContext(
                DefaultProfileActivationContext.Record cachedRecord, DefaultProfileActivationContext targetContext) {
            if (targetContext.record == null) {
                return; // Target context is not recording
            }

            // Replay all the recorded keys from the cached record into the target context's record
            // We need to access the mutable maps in the target context's record
            DefaultProfileActivationContext.Record targetRecord = targetContext.record;

            // Replay active profiles
            cachedRecord.usedActiveProfiles.forEach(targetRecord.usedActiveProfiles::putIfAbsent);

            // Replay inactive profiles
            cachedRecord.usedInactiveProfiles.forEach(targetRecord.usedInactiveProfiles::putIfAbsent);

            // Replay system properties
            cachedRecord.usedSystemProperties.forEach(targetRecord.usedSystemProperties::putIfAbsent);

            // Replay user properties
            cachedRecord.usedUserProperties.forEach(targetRecord.usedUserProperties::putIfAbsent);

            // Replay model properties
            cachedRecord.usedModelProperties.forEach(targetRecord.usedModelProperties::putIfAbsent);

            // Replay model infos
            cachedRecord.usedModelInfos.forEach(targetRecord.usedModelInfos::putIfAbsent);

            // Replay exists checks
            cachedRecord.usedExists.forEach(targetRecord.usedExists::putIfAbsent);
        }
    }

    @SuppressWarnings("deprecation")
    private static List<String> getSubprojects(Model activated) {
        List<String> subprojects = activated.getSubprojects();
        if (subprojects.isEmpty()) {
            subprojects = activated.getModules();
        }
        return subprojects;
    }

    /**
     * Checks if subprojects are explicitly defined in the main model.
     * This method distinguishes between:
     * 1. No subprojects/modules element present - returns false (should auto-discover)
     * 2. Empty subprojects/modules element present - returns true (should NOT auto-discover)
     * 3. Non-empty subprojects/modules - returns true (should NOT auto-discover)
     */
    @SuppressWarnings("deprecation")
    private static boolean hasSubprojectsDefined(Model model) {
        // Only consider the main model: profiles do not influence auto-discovery
        // Inline the check for explicit elements using location tracking
        return model.getLocation("subprojects") != null || model.getLocation("modules") != null;
    }

    @Override
    public Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException {
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(request.getSession(), request);
        try {
            ModelBuilderSessionState build = new ModelBuilderSessionState(request);
            Model model = build.readRawModel();
            if (build.hasErrors()) {
                throw build.newModelBuilderException();
            }
            return model;
        } finally {
            // Clean up REQUEST_SCOPED cache entries for raw model building as well
            try {
                clearRequestScopedCache(request);
            } catch (Exception e) {
                // Log but don't fail the build due to cache cleanup issues
                logger.debug("Failed to clear REQUEST_SCOPED cache for raw model request: {}", request, e);
            }
            RequestTraceHelper.exit(trace);
        }
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
        return new DefaultProfileActivationContext(
                pathTranslator,
                rootLocator,
                interpolator,
                request.getActiveProfileIds(),
                request.getInactiveProfileIds(),
                request.getSystemProperties(),
                request.getUserProperties(),
                model);
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
            Map<String, String> map1 = request.getSession().getUserProperties();
            Map<String, String> map2 = model.getProperties();
            Map<String, String> map3 = request.getSession().getSystemProperties();
            UnaryOperator<String> cb = Interpolator.chain(map1::get, map2::get, map3::get);
            try {
                String interpolated =
                        interpolator.interpolate(interpolatedModel.getParent().getVersion(), cb);
                interpolatedModel = interpolatedModel.withParent(
                        interpolatedModel.getParent().withVersion(interpolated));
            } catch (Exception e) {
                problems.add(
                        Severity.ERROR,
                        Version.BASE,
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

    private boolean containsCoordinates(String message, String groupId, String artifactId, String version) {
        return message != null
                && (groupId == null || message.contains(groupId))
                && (artifactId == null || message.contains(artifactId))
                && (version == null || message.contains(version));
    }

    record GAKey(String groupId, String artifactId) {}

    public record RgavCacheKey(
            Session session,
            RequestTrace trace,
            List<RemoteRepository> repositories,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String tag)
            implements Request<Session> {
        @Nonnull
        @Override
        public Session getSession() {
            return session;
        }

        @Nullable
        @Override
        public RequestTrace getTrace() {
            return trace;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RgavCacheKey that
                    && Objects.equals(tag, that.tag)
                    && Objects.equals(groupId, that.groupId)
                    && Objects.equals(version, that.version)
                    && Objects.equals(artifactId, that.artifactId)
                    && Objects.equals(classifier, that.classifier)
                    && Objects.equals(repositories, that.repositories);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repositories, groupId, artifactId, version, classifier, tag);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append("[").append("gav='");
            if (groupId != null) {
                sb.append(groupId);
            }
            sb.append(":");
            if (artifactId != null) {
                sb.append(artifactId);
            }
            sb.append(":");
            if (version != null) {
                sb.append(version);
            }
            sb.append(":");
            if (classifier != null) {
                sb.append(classifier);
            }
            sb.append("', tag='");
            sb.append(tag);
            sb.append("']");
            return sb.toString();
        }
    }

    public record SourceCacheKey(Session session, RequestTrace trace, Source source, String tag)
            implements Request<Session>, CacheMetadata {
        @Nonnull
        @Override
        public Session getSession() {
            return session;
        }

        @Nullable
        @Override
        public RequestTrace getTrace() {
            return trace;
        }

        @Override
        public CacheRetention getCacheRetention() {
            return source instanceof CacheMetadata cacheMetadata
                    ? cacheMetadata.getCacheRetention()
                    : CacheRetention.REQUEST_SCOPED;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SourceCacheKey that
                    && Objects.equals(tag, that.tag)
                    && Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, tag);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + "location=" + source.getLocation() + ", tag=" + tag + ", path="
                    + source.getPath() + ']';
        }
    }

    public static class SourceResponse<R extends Request<?>, T> implements Result<R> {
        private final R request;
        private final T response;

        SourceResponse(R request, T response) {
            this.request = request;
            this.response = response;
        }

        @Nonnull
        @Override
        public R getRequest() {
            return request;
        }
    }

    static class InterningTransformer implements XmlReaderRequest.Transformer {
        static final Set<String> DEFAULT_CONTEXTS = Set.of(
                // Core Maven coordinates
                "groupId",
                "artifactId",
                "version",
                "namespaceUri",
                "packaging",

                // Dependency-related fields
                "scope",
                "type",
                "classifier",

                // Build and plugin-related fields
                "phase",
                "goal",
                "execution",

                // Repository-related fields
                "layout",
                "policy",
                "checksumPolicy",
                "updatePolicy",

                // Common metadata fields
                "modelVersion",
                "name",
                "url",
                "system",
                "distribution",
                "status",

                // SCM fields
                "connection",
                "developerConnection",
                "tag",

                // Common enum-like values that appear frequently
                "id",
                "inherited",
                "optional");

        private final Set<String> contexts;

        /**
         * Creates an InterningTransformer with default contexts.
         */
        InterningTransformer() {
            this.contexts = DEFAULT_CONTEXTS;
        }

        /**
         * Creates an InterningTransformer with contexts from session properties.
         *
         * @param session the Maven session to read properties from
         */
        InterningTransformer(Session session) {
            this.contexts = parseContextsFromSession(session);
        }

        private Set<String> parseContextsFromSession(Session session) {
            String contextsProperty = session.getUserProperties().get(Constants.MAVEN_MODEL_BUILDER_INTERNS);
            if (contextsProperty == null) {
                contextsProperty = session.getSystemProperties().get(Constants.MAVEN_MODEL_BUILDER_INTERNS);
            }

            if (contextsProperty == null || contextsProperty.trim().isEmpty()) {
                return DEFAULT_CONTEXTS;
            }

            return Arrays.stream(contextsProperty.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }

        @Override
        public String transform(String input, String context) {
            return input != null && contexts.contains(context) ? input.intern() : input;
        }

        /**
         * Get the contexts that will be interned by this transformer.
         * Used for testing purposes.
         */
        Set<String> getContexts() {
            return contexts;
        }
    }

    /**
     * Clears REQUEST_SCOPED cache entries for a specific request.
     * <p>
     * The method identifies the outer request and removes the corresponding cache entry from the session data.
     *
     * @param req the request whose REQUEST_SCOPED cache should be cleared
     * @param <REQ> the request type
     */
    private <REQ extends Request<?>> void clearRequestScopedCache(REQ req) {
        if (req.getSession() instanceof Session session) {
            // Use the same key as DefaultRequestCache
            SessionData.Key<Cache> key = SessionData.key(Cache.class, CacheMetadata.class);

            // Get the outer request key using the same logic as DefaultRequestCache
            Object outerRequestKey = getOuterRequest(req);

            Cache<Object, Object> caches = session.getData().get(key);
            if (caches != null) {
                Object removedCache = caches.get(outerRequestKey);
                if (removedCache instanceof Cache<?, ?> map) {
                    int beforeSize = map.size();
                    map.removeIf((k, v) -> !(k instanceof RgavCacheKey) && !(k instanceof SourceCacheKey));
                    int afterSize = map.size();
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "Cleared REQUEST_SCOPED cache for request: {}, removed {} entries, remaining entries: {}",
                                outerRequestKey.getClass().getSimpleName(),
                                afterSize - beforeSize,
                                afterSize);
                    }
                }
            }
        }
    }

    /**
     * Gets the outer request for cache key purposes.
     * This replicates the logic from DefaultRequestCache.doGetOuterRequest().
     */
    private Object getOuterRequest(Request<?> req) {
        RequestTrace trace = req.getTrace();
        if (trace != null) {
            RequestTrace parent = trace.parent();
            if (parent != null && parent.data() instanceof Request<?> parentRequest) {
                return getOuterRequest(parentRequest);
            }
        }
        return req;
    }

    private static <T, A> List<T> map(List<T> resources, BiFunction<T, A, T> mapper, A argument) {
        List<T> newResources = null;
        if (resources != null) {
            for (int i = 0; i < resources.size(); i++) {
                T resource = resources.get(i);
                T newResource = mapper.apply(resource, argument);
                if (newResource != resource) {
                    if (newResources == null) {
                        newResources = new ArrayList<>(resources);
                    }
                    newResources.set(i, newResource);
                }
            }
        }
        return newResources;
    }

    /**
     * Checks if the parent POM path is within the root directory.
     * This prevents invalid setups where a parent POM is located above the root directory.
     *
     * @param parentPath the path to the parent POM
     * @param rootDirectory the root directory
     * @return true if the parent is within the root directory, false otherwise
     */
    private static boolean isParentWithinRootDirectory(Path parentPath, Path rootDirectory) {
        if (parentPath == null || rootDirectory == null) {
            return true; // Allow if either is null (fallback behavior)
        }

        try {
            Path normalizedParent = parentPath.toAbsolutePath().normalize();
            Path normalizedRoot = rootDirectory.toAbsolutePath().normalize();

            // Check if the parent path starts with the root directory path
            return normalizedParent.startsWith(normalizedRoot);
        } catch (Exception e) {
            // If there's any issue with path resolution, allow it (fallback behavior)
            return true;
        }
    }
}
