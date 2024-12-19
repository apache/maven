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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
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
import org.apache.maven.internal.impl.util.PhasingExecutor;
import org.apache.maven.model.v4.MavenTransformer;
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
    private final ModelCacheFactory modelCacheFactory;
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
            List<ModelTransformer> transformers,
            ModelCacheFactory modelCacheFactory,
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
        this.modelCacheFactory = modelCacheFactory;
        this.modelResolver = modelResolver;
        this.interpolator = interpolator;
        this.pathTranslator = pathTranslator;
        this.rootLocator = rootLocator;
    }

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
            // Create or derive a session based on the request
            ModelBuilderSessionState session;
            if (mainSession == null) {
                mainSession = new ModelBuilderSessionState(request);
                session = mainSession;
            } else {
                session = mainSession.derive(
                        request, new DefaultModelBuilderResult(ProblemCollector.create(mainSession.session)));
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
        }
    }

    protected class ModelBuilderSessionState implements ModelProblemCollector {
        private static final Pattern REGEX = Pattern.compile("\\$\\{([^}]+)}");

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

        ModelBuilderSessionState(ModelBuilderRequest request) {
            this(
                    request.getSession(),
                    request,
                    new DefaultModelBuilderResult(ProblemCollector.create(request.getSession())),
                    request.getSession()
                            .getData()
                            .computeIfAbsent(SessionData.key(ModelCache.class), modelCacheFactory::newInstance),
                    new Graph(),
                    new ConcurrentHashMap<>(64),
                    List.of(),
                    repos(request),
                    repos(request));
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
            this.pomRepositories = pomRepositories;
            this.externalRepositories = externalRepositories;
            this.repositories = repositories;
            this.result.setSource(this.request.getSource());
        }

        ModelBuilderSessionState derive(ModelSource source) {
            return derive(source, new DefaultModelBuilderResult(ProblemCollector.create(session)));
        }

        ModelBuilderSessionState derive(ModelSource source, DefaultModelBuilderResult result) {
            return derive(ModelBuilderRequest.build(request, source), result);
        }

        /**
         * Creates a new session, sharing cached datas and propagating errors.
         */
        ModelBuilderSessionState derive(ModelBuilderRequest request) {
            return derive(request, new DefaultModelBuilderResult(ProblemCollector.create(session)));
        }

        ModelBuilderSessionState derive(ModelBuilderRequest request, DefaultModelBuilderResult result) {
            if (session != request.getSession()) {
                throw new IllegalArgumentException("Session mismatch");
            }
            return new ModelBuilderSessionState(
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
                return derive(ModelSource.fromPath(path)).readRawModel();
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
            return new ModelBuilderException(result);
        }

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

        String replaceCiFriendlyVersion(Map<String, String> properties, String version) {
            // TODO: we're using a simple regex here, but we should probably use
            //       a proper interpolation service to do the replacements
            //       once one is available in maven-api-impl
            //       https://issues.apache.org/jira/browse/MNG-8262
            if (version != null) {
                Matcher matcher = REGEX.matcher(version);
                if (matcher.find()) {
                    StringBuilder result = new StringBuilder();
                    do {
                        // extract the key inside ${}
                        String key = matcher.group(1);
                        // get replacement from the map, or use the original ${xy} if not found
                        String replacement = properties.getOrDefault(key, "\\" + matcher.group(0));
                        matcher.appendReplacement(result, replacement);
                    } while (matcher.find());
                    matcher.appendTail(result); // Append the remaining part of the string
                    return result.toString();
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
            try (PhasingExecutor executor = createExecutor()) {
                for (DefaultModelBuilderResult r : allResults) {
                    executor.execute(() -> {
                        ModelBuilderSessionState mbs = derive(r.getSource(), r);
                        try {
                            mbs.buildEffectiveModel(new LinkedHashSet<>());
                        } catch (ModelBuilderException e) {
                            // gathered with problem collector
                        } catch (RuntimeException t) {
                            exceptions.add(t);
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
                        : new DefaultModelBuilderResult(ProblemCollector.create(session));
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
                cache.clear();
                mappedSources.clear();
                loadFromRoot(top, top);
            }
        }

        private void loadFilePom(
                Executor executor, Path top, Path pom, Set<Path> parents, DefaultModelBuilderResult r) {
            try {
                Path pomDirectory = Files.isDirectory(pom) ? pom : pom.getParent();
                ModelSource src = ModelSource.fromPath(pom);
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
                                Version.BASE,
                                model,
                                -1,
                                -1,
                                null);
                        r.addProblem(problem);
                        continue;
                    }

                    DefaultModelBuilderResult cr = Objects.equals(top, subprojectFile)
                            ? result
                            : new DefaultModelBuilderResult(ProblemCollector.create(session));
                    if (request.isRecursive()) {
                        r.getChildren().add(cr);
                    }

                    executor.execute(() -> loadFilePom(executor, top, subprojectFile, concat(parents, pom), cr));
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
                    resultModel,
                    isBuildRequest() ? ModelValidator.VALIDATION_LEVEL_STRICT : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    this);

            if (hasErrors()) {
                throw newModelBuilderException();
            }
        }

        Model readParent(Model childModel, DefaultProfileActivationContext profileActivationContext) {
            Model parentModel;

            Parent parent = childModel.getParent();
            if (parent != null) {
                parentModel = resolveParent(childModel, profileActivationContext);

                if (!"pom".equals(parentModel.getPackaging())) {
                    add(
                            Severity.ERROR,
                            Version.BASE,
                            "Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                    + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"",
                            parentModel.getLocation("packaging"));
                }
                result.setParentModel(parentModel);
            } else {
                String superModelVersion = childModel.getModelVersion();
                if (superModelVersion == null || !VALID_MODEL_VERSIONS.contains(superModelVersion)) {
                    // Maven 3.x is always using 4.0.0 version to load the supermodel, so
                    // do the same when loading a dependency.  The model validator will also
                    // check that field later.
                    superModelVersion = MODEL_VERSION_4_0_0;
                }
                parentModel = getSuperModel(superModelVersion);
            }

            return parentModel;
        }

        private Model resolveParent(Model childModel, DefaultProfileActivationContext profileActivationContext)
                throws ModelBuilderException {
            Model parentModel = null;
            if (isBuildRequest()) {
                parentModel = readParentLocally(childModel, profileActivationContext);
            }
            if (parentModel == null) {
                parentModel = resolveAndReadParentExternally(childModel, profileActivationContext);
            }
            return parentModel;
        }

        private Model readParentLocally(Model childModel, DefaultProfileActivationContext profileActivationContext)
                throws ModelBuilderException {
            ModelSource candidateSource;

            Parent parent = childModel.getParent();
            String parentPath = parent.getRelativePath();
            if (request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT) {
                if (parentPath != null && !parentPath.isEmpty()) {
                    candidateSource = request.getSource().resolve(modelProcessor::locateExistingPom, parentPath);
                    if (candidateSource == null) {
                        wrongParentRelativePath(childModel);
                        return null;
                    }
                } else {
                    candidateSource =
                            resolveReactorModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                    if (candidateSource == null && parentPath == null) {
                        candidateSource = request.getSource().resolve(modelProcessor::locateExistingPom, "..");
                    }
                }
            } else {
                candidateSource = resolveReactorModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                if (candidateSource == null) {
                    if (parentPath == null) {
                        parentPath = "..";
                    }
                    if (!parentPath.isEmpty()) {
                        candidateSource = request.getSource().resolve(modelProcessor::locateExistingPom, parentPath);
                    }
                }
            }

            if (candidateSource == null) {
                return null;
            }

            ModelBuilderSessionState derived = derive(candidateSource);
            Model candidateModel = derived.readAsParentModel(profileActivationContext);
            addActivePomProfiles(derived.result.getActivePomProfiles());

            String groupId = getGroupId(candidateModel);
            String artifactId = candidateModel.getArtifactId();
            String version = getVersion(candidateModel);

            // Ensure that relative path and GA match, if both are provided
            if (groupId == null
                    || !groupId.equals(parent.getGroupId())
                    || artifactId == null
                    || !artifactId.equals(parent.getArtifactId())) {
                mismatchRelativePathAndGA(childModel, groupId, artifactId);
                return null;
            }

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
        }

        private void mismatchRelativePathAndGA(Model childModel, String groupId, String artifactId) {
            Parent parent = childModel.getParent();
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

        Model resolveAndReadParentExternally(Model childModel, DefaultProfileActivationContext profileActivationContext)
                throws ModelBuilderException {
            ModelBuilderRequest request = this.request;
            setSource(childModel);

            Parent parent = childModel.getParent();

            String groupId = parent.getGroupId();
            String artifactId = parent.getArtifactId();
            String version = parent.getVersion();

            // add repositories specified by the current model so that we can resolve the parent
            if (!childModel.getRepositories().isEmpty()) {
                var previousRepositories = repositories;
                mergeRepositories(childModel.getRepositories(), false);
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
                modelSource = resolveReactorModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                if (modelSource == null) {
                    AtomicReference<Parent> modified = new AtomicReference<>();
                    modelSource = modelResolver.resolveModel(request.getSession(), repositories, parent, modified);
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

            Model parentModel = derive(lenientRequest).readAsParentModel(profileActivationContext);

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

            Model parentModel = readParent(activatedFileModel, profileActivationContext);

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

            // model normalization
            model = modelNormalizer.mergeDuplicates(model, request, this);

            // profile activation
            profileActivationContext.setModel(model);

            List<Profile> interpolatedProfiles =
                    interpolateActivations(model.getProfiles(), profileActivationContext, this);

            // profile injection
            List<Profile> activePomProfiles = getActiveProfiles(interpolatedProfiles, profileActivationContext);
            model = profileInjector.injectProfiles(model, activePomProfiles, request, this);
            model = profileInjector.injectProfiles(model, activeExternalProfiles, request, this);

            addActivePomProfiles(activePomProfiles);

            // model interpolation
            Model resultModel = model;
            resultModel = interpolateModel(resultModel, request, this);

            // url normalization
            resultModel = modelUrlNormalizer.normalize(resultModel, request);

            // Now the fully interpolated model is available: reconfigure the resolver
            if (!resultModel.getRepositories().isEmpty()) {
                List<String> oldRepos =
                        repositories.stream().map(Object::toString).toList();
                mergeRepositories(resultModel.getRepositories(), true);
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

                    add(
                            Severity.ERROR,
                            Version.V20,
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
                            Model parentModel =
                                    derive(ModelSource.fromPath(pomPath)).readFileModel();
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
                                mismatchRelativePathAndGA(model, parentGroupId, parentArtifactId);
                            }
                        } else {
                            if (!MODEL_VERSION_4_0_0.equals(model.getModelVersion()) && path != null) {
                                wrongParentRelativePath(model);
                            }
                        }
                    }
                }

                // subprojects discovery
                if (getSubprojects(model).isEmpty()
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

                // CI friendly version
                // All expressions are interpolated using user properties and properties
                // defined on the root project.
                Map<String, String> properties = new HashMap<>();
                if (!Objects.equals(rootDirectory, model.getProjectDirectory())) {
                    Path rootModelPath = modelProcessor.locateExistingPom(rootDirectory);
                    if (rootModelPath != null) {
                        Model rootModel =
                                derive(ModelSource.fromPath(rootModelPath)).readFileModel();
                        properties.putAll(rootModel.getProperties());
                    }
                } else {
                    properties.putAll(model.getProperties());
                }
                properties.putAll(session.getUserProperties());
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
            }

            for (var transformer : transformers) {
                model = transformer.transformFileModel(model);
            }

            setSource(model);
            modelValidator.validateFileModel(
                    model,
                    isBuildRequest() ? ModelValidator.VALIDATION_LEVEL_STRICT : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    this);
            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            return model;
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
                    rawModel,
                    isBuildRequest() ? ModelValidator.VALIDATION_LEVEL_STRICT : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                    this);

            if (hasFatalErrors()) {
                throw newModelBuilderException();
            }

            return rawModel;
        }

        /**
         * Reads the request source's parent.
         */
        Model readAsParentModel(DefaultProfileActivationContext profileActivationContext) throws ModelBuilderException {
            Map<DefaultProfileActivationContext.Record, Model> parentsPerContext =
                    cache(request.getSource(), PARENT, ConcurrentHashMap::new);
            for (Map.Entry<DefaultProfileActivationContext.Record, Model> e : parentsPerContext.entrySet()) {
                if (e.getKey().matches(profileActivationContext)) {
                    return e.getValue();
                }
            }
            DefaultProfileActivationContext.Record prev = profileActivationContext.start();
            Model model = doReadAsParentModel(profileActivationContext);
            DefaultProfileActivationContext.Record record = profileActivationContext.stop(prev);
            parentsPerContext.put(record, model);
            return model;
        }

        private Model doReadAsParentModel(DefaultProfileActivationContext profileActivationContext)
                throws ModelBuilderException {
            Model raw = readRawModel();
            Model parentData = readParent(raw, profileActivationContext);
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
                    })
                    .assembleModelInheritance(raw, parentData, request, this);

            // activate profiles
            List<Profile> parentInterpolatedProfiles =
                    interpolateActivations(parent.getProfiles(), profileActivationContext, this);
            // profile injection
            List<Profile> parentActivePomProfiles =
                    getActiveProfiles(parentInterpolatedProfiles, profileActivationContext);
            Model injectedParentModel = profileInjector
                    .injectProfiles(parent, parentActivePomProfiles, request, this)
                    .withProfiles(List.of());
            addActivePomProfiles(parentActivePomProfiles);

            return injectedParentModel.withParent(null);
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

        private <T> T cache(String groupId, String artifactId, String version, String tag, Supplier<T> supplier) {
            return cache.computeIfAbsent(groupId, artifactId, version, tag, supplier);
        }

        private <T> T cache(Source source, String tag, Supplier<T> supplier) throws ModelBuilderException {
            return cache.computeIfAbsent(source, tag, supplier);
        }

        boolean isBuildRequest() {
            return request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_PROJECT
                    || request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_EFFECTIVE
                    || request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_CONSUMER;
        }

        boolean isBuildRequestWithActivation() {
            return request.getRequestType() != ModelBuilderRequest.RequestType.BUILD_CONSUMER;
        }

        private List<Profile> interpolateActivations(
                List<Profile> profiles, DefaultProfileActivationContext context, ModelProblemCollector problems) {
            if (profiles.stream()
                    .map(org.apache.maven.api.model.Profile::getActivation)
                    .noneMatch(Objects::nonNull)) {
                return profiles;
            }
            Interpolator interpolator = request.getSession().getService(Interpolator.class);

            class ProfileInterpolator extends MavenTransformer implements UnaryOperator<Profile> {
                ProfileInterpolator() {
                    super(s -> {
                        try {
                            return interpolator.interpolate(
                                    s, Interpolator.chain(context::getUserProperty, context::getSystemProperty));
                        } catch (InterpolatorException e) {
                            problems.add(Severity.ERROR, Version.BASE, e.getMessage(), e);
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
                protected Activation.Builder transformActivation_Condition(
                        Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
                    // do not interpolate the condition activation
                    return builder;
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
                    try {
                        return context.interpolatePath(path);
                    } catch (InterpolatorException e) {
                        problems.add(
                                Severity.ERROR,
                                Version.BASE,
                                "Failed to interpolate file location " + path + ": " + e.getMessage(),
                                target.getLocation(locationKey),
                                e);
                    }
                    return path;
                }
            }
            return profiles.stream().map(new ProfileInterpolator()).toList();
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

    @Override
    public Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException {
        ModelBuilderSessionState build = new ModelBuilderSessionState(request);
        Model model = build.readRawModel();
        if (build.hasErrors()) {
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
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(pathTranslator, rootLocator, interpolator);

        context.setActiveProfileIds(request.getActiveProfileIds());
        context.setInactiveProfileIds(request.getInactiveProfileIds());
        context.setSystemProperties(request.getSystemProperties());
        context.setUserProperties(request.getUserProperties());
        context.setModel(model);

        return context;
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
            Function<String, String> cb = Interpolator.chain(map1::get, map2::get, map3::get);
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
}
