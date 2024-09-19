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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.annotations.Nullable;
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
import org.apache.maven.api.model.InputLocationTracker;
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
import org.apache.maven.api.services.ModelResolver;
import org.apache.maven.api.services.ModelResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.services.model.DependencyManagementImporter;
import org.apache.maven.api.services.model.DependencyManagementInjector;
import org.apache.maven.api.services.model.InheritanceAssembler;
import org.apache.maven.api.services.model.LifecycleBindingsInjector;
import org.apache.maven.api.services.model.ModelBuildingEvent;
import org.apache.maven.api.services.model.ModelBuildingListener;
import org.apache.maven.api.services.model.ModelCache;
import org.apache.maven.api.services.model.ModelCacheFactory;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.ModelNormalizer;
import org.apache.maven.api.services.model.ModelPathTranslator;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.model.ModelUrlNormalizer;
import org.apache.maven.api.services.model.ModelValidator;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.api.services.model.PluginConfigurationExpander;
import org.apache.maven.api.services.model.PluginManagementInjector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileInjector;
import org.apache.maven.api.services.model.ProfileSelector;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.api.spi.ModelTransformer;
import org.apache.maven.api.spi.ModelTransformerException;
import org.apache.maven.internal.impl.resolver.DefaultModelResolver;
import org.apache.maven.model.v4.MavenTransformer;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

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
    private final LifecycleBindingsInjector lifecycleBindingsInjector;
    private final PluginConfigurationExpander pluginConfigurationExpander;
    private final ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;
    private final ModelVersionParser versionParser;
    private final List<ModelTransformer> transformers;
    private final ModelCacheFactory modelCacheFactory;

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
            @Nullable LifecycleBindingsInjector lifecycleBindingsInjector,
            PluginConfigurationExpander pluginConfigurationExpander,
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator,
            ModelVersionParser versionParser,
            List<ModelTransformer> transformers,
            ModelCacheFactory modelCacheFactory) {
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
        this.lifecycleBindingsInjector = lifecycleBindingsInjector;
        this.pluginConfigurationExpander = pluginConfigurationExpander;
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        this.versionParser = versionParser;
        this.transformers = transformers;
        this.modelCacheFactory = modelCacheFactory;
    }

    public ModelBuilderSession newSession() {
        return new ModelBuilderSession() {
            DefaultModelBuilderSession mainSession;

            @Override
            public ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException {
                request = fillRequestDefaults(request);
                if (request.getInterimResult() != null) {
                    if (mainSession == null) {
                        throw new IllegalStateException("ModelBuilderSession is not initialized");
                    }
                    DefaultModelBuilderResult result = asDefaultModelBuilderResult(request.getInterimResult());
                    return DefaultModelBuilder.this.build2(mainSession.derive(request, result), new LinkedHashSet<>());
                } else {
                    DefaultModelBuilderSession session;
                    if (mainSession == null) {
                        mainSession = new DefaultModelBuilderSession(request);
                        session = mainSession;
                    } else {
                        session = mainSession.derive(request, new DefaultModelBuilderResult());
                    }
                    return DefaultModelBuilder.this.build(session, new LinkedHashSet<>());
                }
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

        private Set<ModelProblem.Severity> severities = EnumSet.noneOf(ModelProblem.Severity.class);

        volatile boolean fullReactorLoaded;

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
            this.result.getProblems().forEach(p -> severities.add(p.getSeverity()));
        }

        public DefaultModelBuilderSession derive(ModelSource source) {
            return derive(ModelBuilderRequest.build(request, source), result);
        }

        public DefaultModelBuilderSession derive(ModelBuilderRequest request, DefaultModelBuilderResult result) {
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

        public DefaultModelBuilderResult result() {
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

        public Model getRawModel(Path from, String groupId, String artifactId) {
            ModelSource source = getSource(groupId, artifactId);
            if (source == null) {
                // we need to check the whole reactor in case it's a dependency
                loadFullReactor();
                source = getSource(groupId, artifactId);
            }
            if (source != null) {
                if (!addEdge(from, source.getPath())) {
                    return null;
                }
                try {
                    return readRawModel(derive(source));
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
                return readRawModel(derive(ModelSource.fromPath(path)));
            } catch (ModelBuilderException e) {
                // gathered with problem collector
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
                rootDirectory = session().getRootDirectory();
            } catch (IllegalStateException e) {
                // if no root directory, bail out
                return;
            }
            List<Path> toLoad = new ArrayList<>();
            Path root = getModelProcessor().locateExistingPom(rootDirectory);
            toLoad.add(root);
            while (!toLoad.isEmpty()) {
                Path pom = toLoad.remove(0);
                try {
                    Model rawModel = readFileModel(derive(ModelSource.fromPath(pom)));
                    List<String> subprojects = rawModel.getSubprojects();
                    if (subprojects.isEmpty()) {
                        subprojects = rawModel.getModules();
                    }
                    for (String subproject : subprojects) {
                        Path subprojectFile = getModelProcessor()
                                .locateExistingPom(pom.getParent().resolve(subproject));
                        if (subprojectFile != null) {
                            toLoad.add(subprojectFile);
                        }
                    }
                } catch (ModelBuilderException e) {
                    // gathered with problem collector
                    add(Severity.ERROR, ModelProblem.Version.V40, "Failed to load project " + pom, e);
                }
            }
        }

        private boolean addEdge(Path from, Path p) {
            try {
                dag.addEdge(from.toString(), p.toString());
                return true;
            } catch (Graph.CycleDetectedException e) {
                add(new DefaultModelProblem(
                        "Cycle detected between models at " + from + " and " + p,
                        Severity.FATAL,
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
            if (groupId != null) {
                mappedSources
                        .computeIfAbsent(new GAKey(null, artifactId), k -> new HashSet<>())
                        .add(source);
            }
        }

        public boolean hasFatalErrors() {
            return severities.contains(ModelProblem.Severity.FATAL);
        }

        public boolean hasErrors() {
            return severities.contains(ModelProblem.Severity.ERROR) || severities.contains(ModelProblem.Severity.FATAL);
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

        public String getRootModelId() {
            return ModelProblemUtils.toId(rootModel);
        }

        @Override
        public void add(ModelProblem problem) {
            result.getProblems().add(problem);

            severities.add(problem.getSeverity());
        }

        public void addAll(Collection<ModelProblem> problems) {
            this.result.getProblems().addAll(problems);

            for (ModelProblem problem : problems) {
                severities.add(problem.getSeverity());
            }
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
            ModelBuilderResult result = this.result;
            if (result.getModelIds().isEmpty()) {
                DefaultModelBuilderResult tmp = new DefaultModelBuilderResult();
                tmp.setEffectiveModel(result.getEffectiveModel());
                tmp.setProblems(getProblems());
                tmp.setActiveExternalProfiles(result.getActiveExternalProfiles());
                String id = getRootModelId();
                tmp.addModelId(id);
                tmp.setRawModel(id, getRootModel());
                result = tmp;
            }
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
        Model transformRawToBuildPom(Model model) {
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
    }

    private static ModelBuilderRequest fillRequestDefaults(ModelBuilderRequest request) {
        ModelBuilderRequest.ModelBuilderRequestBuilder builder = ModelBuilderRequest.builder(request);
        if (request.getModelResolver() == null) {
            builder.modelResolver(new DefaultModelResolver());
        }
        return builder.build();
    }

    protected ModelBuilderResult build(DefaultModelBuilderSession build, Collection<String> importIds)
            throws ModelBuilderException {
        // phase 1
        ModelBuilderRequest request = build.request;
        DefaultModelBuilderResult result = build.result;

        // read and validate raw model
        Model fileModel = readFileModel(build);
        result.setFileModel(fileModel);

        Model activatedFileModel = activateFileModel(build, fileModel);
        result.setActivatedFileModel(activatedFileModel);

        if (!request.isTwoPhaseBuilding()) {
            return build2(build, importIds);
        } else if (hasModelErrors(build)) {
            throw build.newModelBuilderException();
        }

        return result;
    }

    private Model activateFileModel(DefaultModelBuilderSession build, Model inputModel) throws ModelBuilderException {
        build.setRootModel(inputModel);

        // profile activation
        DefaultProfileActivationContext profileActivationContext =
                getProfileActivationContext(build.request, inputModel);

        build.setSource("(external profiles)");
        List<Profile> activeExternalProfiles =
                profileSelector.getActiveProfiles(build.request.getProfiles(), profileActivationContext, build);

        build.result.setActiveExternalProfiles(activeExternalProfiles);

        if (!activeExternalProfiles.isEmpty()) {
            Properties profileProps = new Properties();
            for (Profile profile : activeExternalProfiles) {
                profileProps.putAll(profile.getProperties());
            }
            profileProps.putAll(profileActivationContext.getUserProperties());
            profileActivationContext.setUserProperties(profileProps);
        }

        profileActivationContext.setProjectProperties(inputModel.getProperties());
        build.setSource(inputModel);
        List<Profile> activePomProfiles =
                profileSelector.getActiveProfiles(inputModel.getProfiles(), profileActivationContext, build);

        // model normalization
        build.setSource(inputModel);
        inputModel = modelNormalizer.mergeDuplicates(inputModel, build.request, build);

        Map<String, Activation> interpolatedActivations = getProfileActivations(inputModel);
        inputModel = injectProfileActivations(inputModel, interpolatedActivations);

        // profile injection
        inputModel = profileInjector.injectProfiles(inputModel, activePomProfiles, build.request, build);
        inputModel = profileInjector.injectProfiles(inputModel, activeExternalProfiles, build.request, build);

        return inputModel;
    }

    @SuppressWarnings("checkstyle:methodlength")
    private Model readEffectiveModel(final DefaultModelBuilderSession build) throws ModelBuilderException {

        ModelBuilderRequest request = build.request;
        DefaultModelBuilderResult result = build.result;

        Model inputModel = readRawModel(build);
        if (build.hasFatalErrors()) {
            throw build.newModelBuilderException();
        }

        inputModel = activateFileModel(build, inputModel);

        build.setRootModel(inputModel);

        ModelData resultData = new ModelData(request.getSource(), inputModel);
        String superModelVersion =
                inputModel.getModelVersion() != null ? inputModel.getModelVersion() : MODEL_VERSION_4_0_0;
        if (!VALID_MODEL_VERSIONS.contains(superModelVersion)) {
            // Maven 3.x is always using 4.0.0 version to load the supermodel, so
            // do the same when loading a dependency.  The model validator will also
            // check that field later.
            superModelVersion = MODEL_VERSION_4_0_0;
        }
        ModelData superData = new ModelData(null, getSuperModel(superModelVersion));

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

        Collection<String> parentIds = new LinkedHashSet<>();

        List<Model> lineage = new ArrayList<>();

        for (ModelData currentData = resultData; ; ) {
            String modelId = currentData.id();
            result.addModelId(modelId);

            Model model = currentData.model();
            result.setRawModel(modelId, model);
            build.setSource(model);

            // model normalization
            model = modelNormalizer.mergeDuplicates(model, request, build);

            // profile activation
            profileActivationContext.setProjectProperties(model.getProperties());

            List<Profile> interpolatedProfiles =
                    interpolateActivations(model.getProfiles(), profileActivationContext, build);

            // profile injection
            List<Profile> activePomProfiles =
                    profileSelector.getActiveProfiles(interpolatedProfiles, profileActivationContext, build);
            result.setActivePomProfiles(modelId, activePomProfiles);
            model = profileInjector.injectProfiles(model, activePomProfiles, request, build);
            if (currentData == resultData) {
                model = profileInjector.injectProfiles(model, activeExternalProfiles, request, build);
            }

            lineage.add(model);

            if (currentData == superData) {
                break;
            }

            // add repositories specified by the current model so that we can resolve the parent
            if (!model.getRepositories().isEmpty()) {
                List<String> oldRepos =
                        build.getRepositories().stream().map(Object::toString).toList();
                build.mergeRepositories(model.getRepositories(), false);
                List<String> newRepos =
                        build.getRepositories().stream().map(Object::toString).toList();
                if (!Objects.equals(oldRepos, newRepos)) {
                    logger.debug("Merging repositories from " + model.getId() + "\n"
                            + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
                }
            }

            // we pass a cloned model, so that resolving the parent version does not affect the returned model
            ModelData parentData = readParent(build, model, currentData.source());

            if (parentData == null) {
                currentData = superData;
            } else if (!parentIds.add(parentData.id())) {
                StringBuilder message = new StringBuilder("The parents form a cycle: ");
                for (String parentId : parentIds) {
                    message.append(parentId).append(" -> ");
                }
                message.append(parentData.id());

                build.add(Severity.FATAL, ModelProblem.Version.BASE, message.toString());

                throw build.newModelBuilderException();
            } else {
                currentData = parentData;
            }
        }

        Model tmpModel = lineage.get(0);

        // inject interpolated activations
        List<Profile> interpolated = interpolateActivations(tmpModel.getProfiles(), profileActivationContext, build);
        if (interpolated != tmpModel.getProfiles()) {
            tmpModel = tmpModel.withProfiles(interpolated);
        }

        // inject external profile into current model
        tmpModel = profileInjector.injectProfiles(tmpModel, activeExternalProfiles, request, build);

        lineage.set(0, tmpModel);

        checkPluginVersions(lineage, request, build);

        // inheritance assembly
        Model resultModel = assembleInheritance(lineage, request, build);

        // consider caching inherited model

        build.setSource(resultModel);
        build.setRootModel(resultModel);

        // model interpolation
        resultModel = interpolateModel(resultModel, request, build);

        // url normalization
        resultModel = modelUrlNormalizer.normalize(resultModel, request);

        result.setEffectiveModel(resultModel);

        // Now the fully interpolated model is available: reconfigure the resolver
        if (!resultModel.getRepositories().isEmpty()) {
            List<String> oldRepos =
                    build.getRepositories().stream().map(Object::toString).toList();
            build.mergeRepositories(resultModel.getRepositories(), true);
            List<String> newRepos =
                    build.getRepositories().stream().map(Object::toString).toList();
            if (!Objects.equals(oldRepos, newRepos)) {
                logger.debug("Replacing repositories from " + resultModel.getId() + "\n"
                        + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
            }
        }

        return resultModel;
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
                        addInterpolationProblem(problems, target, path, e, locationKey);
                    }
                }
                return path;
            }
        }
        return profiles.stream().map(new ProfileInterpolator()).toList();
    }

    private static void addInterpolationProblem(
            ModelProblemCollector problems,
            InputLocationTracker target,
            String path,
            InterpolationException e,
            String locationKey) {
        problems.add(
                Severity.ERROR,
                ModelProblem.Version.BASE,
                "Failed to interpolate file location " + path + ": " + e.getMessage(),
                target.getLocation(locationKey),
                e);
    }

    private static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public ModelBuilderResult build(final ModelBuilderRequest request, final ModelBuilderResult result)
            throws ModelBuilderException {
        return build(
                new DefaultModelBuilderSession(request, (DefaultModelBuilderResult) result), new LinkedHashSet<>());
    }

    public Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException {
        request = fillRequestDefaults(request);
        DefaultModelBuilderSession build = new DefaultModelBuilderSession(request);
        Model model = readRawModel(build);
        if (hasModelErrors(build)) {
            throw build.newModelBuilderException();
        }
        return model;
    }

    private ModelBuilderResult build2(DefaultModelBuilderSession build, Collection<String> importIds)
            throws ModelBuilderException {
        ModelBuilderRequest request = build.request;
        DefaultModelBuilderResult result = build.result;

        // phase 2
        Model resultModel = readEffectiveModel(build);
        build.setSource(resultModel);
        build.setRootModel(resultModel);

        // model path translation
        resultModel = modelPathTranslator.alignToBaseDirectory(resultModel, resultModel.getProjectDirectory(), request);

        // plugin management injection
        resultModel = pluginManagementInjector.injectManagement(resultModel, request, build);

        resultModel = fireEvent(resultModel, request, build, ModelBuildingListener::buildExtensionsAssembled);

        if (request.getRequestType() != ModelBuilderRequest.RequestType.DEPENDENCY) {
            if (lifecycleBindingsInjector == null) {
                throw new IllegalStateException("lifecycle bindings injector is missing");
            }

            // lifecycle bindings injection
            resultModel = lifecycleBindingsInjector.injectLifecycleBindings(resultModel, request, build);
        }

        // dependency management import
        resultModel = importDependencyManagement(build, resultModel, importIds);

        // dependency management injection
        resultModel = dependencyManagementInjector.injectManagement(resultModel, request, build);

        resultModel = modelNormalizer.injectDefaultValues(resultModel, request, build);

        if (request.getRequestType() != ModelBuilderRequest.RequestType.DEPENDENCY) {
            // plugins configuration
            resultModel = pluginConfigurationExpander.expandPluginConfiguration(resultModel, request, build);
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
                build);

        if (hasModelErrors(build)) {
            throw build.newModelBuilderException();
        }

        return result;
    }

    private DefaultModelBuilderResult asDefaultModelBuilderResult(ModelBuilderResult phaseOneResult) {
        if (phaseOneResult instanceof DefaultModelBuilderResult) {
            return (DefaultModelBuilderResult) phaseOneResult;
        } else {
            return new DefaultModelBuilderResult(phaseOneResult);
        }
    }

    Model readFileModel(DefaultModelBuilderSession build) throws ModelBuilderException {
        Model model = cache(build.cache, build.request.getSource(), FILE, () -> doReadFileModel(build));
        if (build.request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
            build.putSource(getGroupId(model), model.getArtifactId(), build.request.getSource());
        }
        return model;
    }

    @SuppressWarnings("checkstyle:methodlength")
    private Model doReadFileModel(DefaultModelBuilderSession build) throws ModelBuilderException {
        ModelBuilderRequest request = build.request;
        ModelSource modelSource = request.getSource();
        Model model;
        build.setSource(modelSource.getLocation());
        try {
            boolean strict = request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM;

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
                ((ModelProblemCollector) build)
                        .add(
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
            ((ModelProblemCollector) build)
                    .add(
                            Severity.FATAL,
                            ModelProblem.Version.BASE,
                            "Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage(),
                            e);
            throw ((ModelProblemCollector) build).newModelBuilderException();
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
            ((ModelProblemCollector) build)
                    .add(
                            Severity.FATAL,
                            ModelProblem.Version.BASE,
                            "Non-readable POM " + modelSource.getLocation() + ": " + msg,
                            e);
            throw ((ModelProblemCollector) build).newModelBuilderException();
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
                        Model parentModel = readFileModel(build.derive(ModelSource.fromPath(pomPath)));
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
                    ((ModelProblemCollector) build)
                            .add(Severity.FATAL, ModelProblem.Version.V41, "Error discovering subprojects", e);
                }
            }
        }

        for (var transformer : transformers) {
            model = transformer.transformFileModel(model);
        }

        ((ModelProblemCollector) build).setSource(model);
        modelValidator.validateFileModel(
                model,
                request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM
                        ? ModelValidator.VALIDATION_LEVEL_STRICT
                        : ModelValidator.VALIDATION_LEVEL_MINIMAL,
                request,
                build);
        if (hasFatalErrors(build)) {
            throw build.newModelBuilderException();
        }

        return model;
    }

    Model readRawModel(DefaultModelBuilderSession build) throws ModelBuilderException {
        return cache(build.cache, build.request.getSource(), RAW, () -> doReadRawModel(build));
    }

    private Model doReadRawModel(DefaultModelBuilderSession build) throws ModelBuilderException {
        ModelBuilderRequest request = build.request;
        Model rawModel = readFileModel(build);
        if (!MODEL_VERSION_4_0_0.equals(rawModel.getModelVersion())
                && request.getRequestType() == ModelBuilderRequest.RequestType.BUILD_POM) {
            try {
                rawModel = build.transformRawToBuildPom(rawModel);
            } catch (ModelTransformerException e) {
                build.add(Severity.FATAL, ModelProblem.Version.V40, null, e);
            }
        }

        String namespace = rawModel.getNamespaceUri();
        if (rawModel.getModelVersion() == null && namespace != null && namespace.startsWith(NAMESPACE_PREFIX)) {
            rawModel = rawModel.withModelVersion(namespace.substring(NAMESPACE_PREFIX.length()));
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
                build);

        if (hasFatalErrors(build)) {
            throw build.newModelBuilderException();
        }

        return rawModel;
    }

    static String getGroupId(Model model) {
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    private String getVersion(Model model) {
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

    private ModelData readParent(DefaultModelBuilderSession build, Model childModel, ModelSource childSource)
            throws ModelBuilderException {
        ModelData parentData = null;

        Parent parent = childModel.getParent();
        if (parent != null) {
            parentData = readParentLocally(build, childModel, childSource);
            if (parentData == null) {
                parentData = resolveAndReadParentExternally(build, childModel);
            }

            Model parentModel = parentData.model();
            if (!"pom".equals(parentModel.getPackaging())) {
                build.add(
                        Severity.ERROR,
                        ModelProblem.Version.BASE,
                        "Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"",
                        parentModel.getLocation("packaging"));
            }
        }

        return parentData;
    }

    private ModelData readParentLocally(DefaultModelBuilderSession build, Model childModel, ModelSource childSource)
            throws ModelBuilderException {
        ModelSource candidateSource = getParentPomFile(childModel, childSource);
        if (candidateSource == null) {
            return null;
        }
        final Model candidateModel = readRawModel(build.derive(candidateSource));

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
            if (childModel != build.getRootModel()) {
                buffer.append(" of POM ").append(ModelProblemUtils.toSourceHint(childModel));
            }
            buffer.append(" points at ").append(groupId).append(':').append(artifactId);
            buffer.append(" instead of ").append(parent.getGroupId()).append(':');
            buffer.append(parent.getArtifactId()).append(", please verify your project structure");

            build.setSource(childModel);
            build.add(Severity.WARNING, ModelProblem.Version.BASE, buffer.toString(), parent.getLocation(""));
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
                    build.add(
                            Severity.FATAL,
                            ModelProblem.Version.V31,
                            "Version must be a constant",
                            childModel.getLocation(""));

                } else {
                    if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                        // Message below is checked for in the MNG-2199 core IT.
                        build.add(
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

        /*
         * if ( version == null || !version.equals( parent.getVersion() ) ) { return null; }
         */

        return new ModelData(candidateSource, candidateModel);
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

    private ModelData resolveAndReadParentExternally(DefaultModelBuilderSession build, Model childModel)
            throws ModelBuilderException {
        ModelBuilderRequest request = build.request;
        build.setSource(childModel);

        Parent parent = childModel.getParent();

        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();

        ModelResolver modelResolver = getModelResolver(request);
        requireNonNull(
                modelResolver,
                String.format(
                        "request.modelResolver cannot be null (parent POM %s and POM %s)",
                        ModelProblemUtils.toId(groupId, artifactId, version),
                        ModelProblemUtils.toSourceHint(childModel)));

        ModelSource modelSource;
        try {
            AtomicReference<Parent> modified = new AtomicReference<>();
            modelSource = modelResolver.resolveModel(request.getSession(), build.getRepositories(), parent, modified);
            if (modified.get() != null) {
                parent = modified.get();
            }
        } catch (ModelResolverException e) {
            // Message below is checked for in the MNG-2199 core IT.
            StringBuilder buffer = new StringBuilder(256);
            buffer.append("Non-resolvable parent POM");
            if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
            }
            if (childModel != ((ModelProblemCollector) build).getRootModel()) {
                buffer.append(" for ").append(ModelProblemUtils.toId(childModel));
            }
            buffer.append(": ").append(e.getMessage());
            if (childModel.getProjectDirectory() != null) {
                if (parent.getRelativePath() == null || parent.getRelativePath().isEmpty()) {
                    buffer.append(" and 'parent.relativePath' points at no local POM");
                } else {
                    buffer.append(" and 'parent.relativePath' points at wrong local POM");
                }
            }

            ((ModelProblemCollector) build)
                    .add(Severity.FATAL, ModelProblem.Version.BASE, buffer.toString(), parent.getLocation(""), e);
            throw ((ModelProblemCollector) build).newModelBuilderException();
        }

        ModelBuilderRequest lenientRequest = ModelBuilderRequest.builder(request)
                .requestType(ModelBuilderRequest.RequestType.PARENT_POM)
                .source(modelSource)
                .build();

        Model parentModel = readParentModel(new DefaultModelBuilderSession(lenientRequest, build.result));

        if (!parent.getVersion().equals(version)) {
            String rawChildModelVersion = childModel.getVersion();

            if (rawChildModelVersion == null) {
                // Message below is checked for in the MNG-2199 core IT.
                build.add(
                        Severity.FATAL,
                        ModelProblem.Version.V31,
                        "Version must be a constant",
                        childModel.getLocation(""));
            } else {
                if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                    // Message below is checked for in the MNG-2199 core IT.
                    build.add(
                            Severity.FATAL,
                            ModelProblem.Version.V31,
                            "Version must be a constant",
                            childModel.getLocation("version"));
                }
            }

            // MNG-2199: What else to check here ?
        }

        return new ModelData(modelSource, parentModel);
    }

    Model readParentModel(DefaultModelBuilderSession build) {
        return cache(build.cache, build.request.getSource(), PARENT, () -> doReadParentModel(build));
    }

    private Model doReadParentModel(DefaultModelBuilderSession build) {
        Model raw = readRawModel(build);

        ModelData parentData;
        if (raw.getParent() != null) {
            parentData = resolveAndReadParentExternally(build, raw);
        } else {
            String superModelVersion = raw.getModelVersion() != null ? raw.getModelVersion() : "4.0.0";
            if (!VALID_MODEL_VERSIONS.contains(superModelVersion)) {
                // Maven 3.x is always using 4.0.0 version to load the supermodel, so
                // do the same when loading a dependency.  The model validator will also
                // check that field later.
                superModelVersion = MODEL_VERSION_4_0_0;
            }
            parentData = new ModelData(null, getSuperModel(superModelVersion));
        }

        Model parent = inheritanceAssembler.assembleModelInheritance(raw, parentData.model(), build.request, build);
        return parent.withParent(null);
    }

    private Model getSuperModel(String modelVersion) {
        return superPomProvider.getSuperPom(modelVersion);
    }

    private Model importDependencyManagement(
            DefaultModelBuilderSession build, Model model, Collection<String> importIds) {
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

            DependencyManagement importMgmt = loadDependencyManagement(build, model, dependency, importIds);

            if (importMgmt != null) {
                if (importMgmts == null) {
                    importMgmts = new ArrayList<>();
                }

                importMgmts.add(importMgmt);
            }
        }

        importIds.remove(importing);

        model = model.withDependencyManagement(model.getDependencyManagement().withDependencies(deps));

        return dependencyManagementImporter.importManagement(model, importMgmts, build.request, build);
    }

    private DependencyManagement loadDependencyManagement(
            DefaultModelBuilderSession build, Model model, Dependency dependency, Collection<String> importIds) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();

        if (groupId == null || groupId.isEmpty()) {
            build.add(
                    Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "'dependencyManagement.dependencies.dependency.groupId' for " + dependency.getManagementKey()
                            + " is missing.",
                    dependency.getLocation(""));
            return null;
        }
        if (artifactId == null || artifactId.isEmpty()) {
            build.add(
                    Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "'dependencyManagement.dependencies.dependency.artifactId' for " + dependency.getManagementKey()
                            + " is missing.",
                    dependency.getLocation(""));
            return null;
        }
        if (version == null || version.isEmpty()) {
            build.add(
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
            build.add(Severity.ERROR, ModelProblem.Version.BASE, message.toString());
            return null;
        }

        Model importModel = cache(
                build.cache,
                groupId,
                artifactId,
                version,
                IMPORT,
                () -> doLoadDependencyManagement(build, model, dependency, groupId, artifactId, version, importIds));
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

    @SuppressWarnings("checkstyle:parameternumber")
    private Model doLoadDependencyManagement(
            DefaultModelBuilderSession build,
            Model model,
            Dependency dependency,
            String groupId,
            String artifactId,
            String version,
            Collection<String> importIds) {
        final ModelBuilderRequest request = build.request;
        final ModelResolver modelResolver = getModelResolver(request);
        if (modelResolver == null) {
            throw new NullPointerException(String.format(
                    "request.workspaceModelResolver and request.modelResolver cannot be null (parent POM %s and POM %s)",
                    ModelProblemUtils.toId(groupId, artifactId, version), ModelProblemUtils.toSourceHint(model)));
        }

        Model importModel;
        // no workspace resolver or workspace resolver returned null (i.e. model not in workspace)
        final ModelSource importSource;
        try {
            importSource = modelResolver.resolveModel(
                    request.getSession(), build.getRepositories(), dependency, new AtomicReference<>());
        } catch (ModelBuilderException e) {
            StringBuilder buffer = new StringBuilder(256);
            buffer.append("Non-resolvable import POM");
            if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
            }
            buffer.append(": ").append(e.getMessage());

            build.add(Severity.ERROR, ModelProblem.Version.BASE, buffer.toString(), dependency.getLocation(""), e);
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
                build.add(
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
                    .modelResolver(modelResolver)
                    .twoPhaseBuilding(false)
                    .repositories(build.getRepositories())
                    .build();
            importResult = build(new DefaultModelBuilderSession(importRequest), importIds);
        } catch (ModelBuilderException e) {
            e.getResult().getProblems().forEach(build::add);
            return null;
        }

        importResult.getProblems().forEach(build::add);

        importModel = importResult.getEffectiveModel();

        return importModel;
    }

    private static <T> T cache(
            ModelCache cache, String groupId, String artifactId, String version, String tag, Callable<T> supplier) {
        return cache.computeIfAbsent(groupId, artifactId, version, tag, asSupplier(supplier));
    }

    private static <T> T cache(ModelCache cache, Source source, String tag, Callable<T> supplier) {
        return cache.computeIfAbsent(source, tag, asSupplier(supplier));
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

    private Model fireEvent(
            Model model,
            ModelBuilderRequest request,
            ModelProblemCollector problems,
            BiConsumer<ModelBuildingListener, ModelBuildingEvent> catapult) {
        ModelBuildingListener listener = getModelBuildingListener(request);

        if (listener != null) {
            AtomicReference<Model> m = new AtomicReference<>(model);

            ModelBuildingEvent event = new DefaultModelBuildingEvent(model, m::set, request, problems);

            catapult.accept(listener, event);

            return m.get();
        }

        return model;
    }

    private boolean containsCoordinates(String message, String groupId, String artifactId, String version) {
        return message != null
                && (groupId == null || message.contains(groupId))
                && (artifactId == null || message.contains(artifactId))
                && (version == null || message.contains(version));
    }

    protected boolean hasModelErrors(ModelProblemCollector problems) {
        return problems.hasErrors();
    }

    protected boolean hasFatalErrors(ModelProblemCollector problems) {
        return problems.hasFatalErrors();
    }

    ModelProcessor getModelProcessor() {
        return modelProcessor;
    }

    private static ModelBuildingListener getModelBuildingListener(ModelBuilderRequest request) {
        return (ModelBuildingListener) request.getListener();
    }

    private static ModelResolver getModelResolver(ModelBuilderRequest request) {
        return request.getModelResolver();
    }

    record GAKey(String groupId, String artifactId) {}
}
