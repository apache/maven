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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Session;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.feature.Features;
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
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelCache;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.ModelResolver;
import org.apache.maven.api.services.ModelResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ModelTransformer;
import org.apache.maven.api.services.ModelTransformerContext;
import org.apache.maven.api.services.ModelTransformerContextBuilder;
import org.apache.maven.api.services.ModelTransformerException;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.services.model.DependencyManagementImporter;
import org.apache.maven.api.services.model.DependencyManagementInjector;
import org.apache.maven.api.services.model.InheritanceAssembler;
import org.apache.maven.api.services.model.LifecycleBindingsInjector;
import org.apache.maven.api.services.model.ModelBuildingEvent;
import org.apache.maven.api.services.model.ModelBuildingListener;
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
import org.apache.maven.api.services.model.WorkspaceModelResolver;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.internal.impl.resolver.DefaultModelCache;
import org.apache.maven.internal.impl.resolver.DefaultModelRepositoryHolder;
import org.apache.maven.internal.impl.resolver.DefaultModelResolver;
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
    private final ModelTransformer transformer;
    private final ModelVersionParser versionParser;

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
            ModelTransformer transformer,
            ModelVersionParser versionParser) {
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
        this.transformer = transformer;
        this.versionParser = versionParser;
    }

    @Override
    public ModelTransformerContextBuilder newTransformerContextBuilder() {
        return new DefaultModelTransformerContextBuilder(this);
    }

    @Override
    public ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException {
        request = fillRequestDefaults(request);
        if (request.getInterimResult() != null) {
            return build(request, request.getInterimResult(), new LinkedHashSet<>());
        } else {
            return build(request, new LinkedHashSet<>());
        }
    }

    private static ModelBuilderRequest fillRequestDefaults(ModelBuilderRequest request) {
        ModelBuilderRequest.ModelBuilderRequestBuilder builder = ModelBuilderRequest.builder(request);
        if (request.getModelCache() == null) {
            builder.modelCache(new DefaultModelCache());
        }
        if (request.getModelRepositoryHolder() == null) {
            builder.modelRepositoryHolder(new DefaultModelRepositoryHolder(
                    request.getSession(),
                    DefaultModelRepositoryHolder.RepositoryMerging.POM_DOMINANT,
                    request.getSession().getRemoteRepositories()));
        }
        if (request.getModelResolver() == null) {
            builder.modelResolver(new DefaultModelResolver());
        }
        return builder.build();
    }

    protected ModelBuilderResult build(ModelBuilderRequest request, Collection<String> importIds)
            throws ModelBuilderException {
        // phase 1
        DefaultModelBuilderResult result = new DefaultModelBuilderResult();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(result);

        // read and validate raw model
        Model fileModel = readFileModel(request, problems);
        result.setFileModel(fileModel);

        Model activatedFileModel = activateFileModel(fileModel, request, result, problems);
        result.setActivatedFileModel(activatedFileModel);

        if (!request.isTwoPhaseBuilding()) {
            return build(request, result, importIds);
        } else if (hasModelErrors(problems)) {
            throw problems.newModelBuilderException();
        }

        return result;
    }

    private Model activateFileModel(
            Model inputModel,
            ModelBuilderRequest request,
            DefaultModelBuilderResult result,
            DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        problems.setRootModel(inputModel);

        // profile activation
        DefaultProfileActivationContext profileActivationContext = getProfileActivationContext(request, inputModel);

        problems.setSource("(external profiles)");
        List<Profile> activeExternalProfiles =
                profileSelector.getActiveProfiles(request.getProfiles(), profileActivationContext, problems);

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
        problems.setSource(inputModel);
        List<Profile> activePomProfiles =
                profileSelector.getActiveProfiles(inputModel.getProfiles(), profileActivationContext, problems);

        // model normalization
        problems.setSource(inputModel);
        inputModel = modelNormalizer.mergeDuplicates(inputModel, request, problems);

        Map<String, Activation> interpolatedActivations = getProfileActivations(inputModel);
        inputModel = injectProfileActivations(inputModel, interpolatedActivations);

        // profile injection
        inputModel = profileInjector.injectProfiles(inputModel, activePomProfiles, request, problems);
        inputModel = profileInjector.injectProfiles(inputModel, activeExternalProfiles, request, problems);

        return inputModel;
    }

    @SuppressWarnings("checkstyle:methodlength")
    private Model readEffectiveModel(
            final ModelBuilderRequest request,
            final DefaultModelBuilderResult result,
            DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        Model inputModel = readRawModel(request, problems);
        if (problems.hasFatalErrors()) {
            throw problems.newModelBuilderException();
        }

        inputModel = activateFileModel(inputModel, request, result, problems);

        problems.setRootModel(inputModel);

        ModelData resultData = new ModelData(request.getSource(), inputModel);
        String superModelVersion = inputModel.getModelVersion() != null ? inputModel.getModelVersion() : "4.0.0";
        if (!VALID_MODEL_VERSIONS.contains(superModelVersion)) {
            // Maven 3.x is always using 4.0.0 version to load the supermodel, so
            // do the same when loading a dependency.  The model validator will also
            // check that field later.
            superModelVersion = "4.0.0";
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
            problems.setSource(model);

            // model normalization
            model = modelNormalizer.mergeDuplicates(model, request, problems);

            // profile activation
            profileActivationContext.setProjectProperties(model.getProperties());

            List<Profile> interpolatedProfiles =
                    interpolateActivations(model.getProfiles(), profileActivationContext, problems);

            // profile injection
            List<Profile> activePomProfiles =
                    profileSelector.getActiveProfiles(interpolatedProfiles, profileActivationContext, problems);
            result.setActivePomProfiles(modelId, activePomProfiles);
            model = profileInjector.injectProfiles(model, activePomProfiles, request, problems);
            if (currentData == resultData) {
                model = profileInjector.injectProfiles(model, activeExternalProfiles, request, problems);
            }

            lineage.add(model);

            if (currentData == superData) {
                break;
            }

            // add repositories specified by the current model so that we can resolve the parent
            if (!model.getRepositories().isEmpty()) {
                List<String> oldRepos = request.getModelRepositoryHolder().getRepositories().stream()
                        .map(Object::toString)
                        .toList();
                request.getModelRepositoryHolder().merge(model.getRepositories(), false);
                List<String> newRepos = request.getModelRepositoryHolder().getRepositories().stream()
                        .map(Object::toString)
                        .toList();
                if (!Objects.equals(oldRepos, newRepos)) {
                    logger.debug("Merging repositories from " + model.getId() + "\n"
                            + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
                }
            }

            // we pass a cloned model, so that resolving the parent version does not affect the returned model
            ModelData parentData = readParent(model, currentData.source(), request, problems);

            if (parentData == null) {
                currentData = superData;
            } else if (!parentIds.add(parentData.id())) {
                StringBuilder message = new StringBuilder("The parents form a cycle: ");
                for (String parentId : parentIds) {
                    message.append(parentId).append(" -> ");
                }
                message.append(parentData.id());

                problems.add(Severity.FATAL, ModelProblem.Version.BASE, message.toString());

                throw problems.newModelBuilderException();
            } else {
                currentData = parentData;
            }
        }

        Model tmpModel = lineage.get(0);

        // inject interpolated activations
        List<Profile> interpolated = interpolateActivations(tmpModel.getProfiles(), profileActivationContext, problems);
        if (interpolated != tmpModel.getProfiles()) {
            tmpModel = tmpModel.withProfiles(interpolated);
        }

        // inject external profile into current model
        tmpModel = profileInjector.injectProfiles(tmpModel, activeExternalProfiles, request, problems);

        lineage.set(0, tmpModel);

        checkPluginVersions(lineage, request, problems);

        // inheritance assembly
        Model resultModel = assembleInheritance(lineage, request, problems);

        // consider caching inherited model

        problems.setSource(resultModel);
        problems.setRootModel(resultModel);

        // model interpolation
        resultModel = interpolateModel(resultModel, request, problems);

        // url normalization
        resultModel = modelUrlNormalizer.normalize(resultModel, request);

        result.setEffectiveModel(resultModel);

        // Now the fully interpolated model is available: reconfigure the resolver
        if (!resultModel.getRepositories().isEmpty()) {
            List<String> oldRepos = request.getModelRepositoryHolder().getRepositories().stream()
                    .map(Object::toString)
                    .toList();
            request.getModelRepositoryHolder().merge(resultModel.getRepositories(), true);
            List<String> newRepos = request.getModelRepositoryHolder().getRepositories().stream()
                    .map(Object::toString)
                    .toList();
            if (!Objects.equals(oldRepos, newRepos)) {
                logger.debug("Replacing repositories from " + resultModel.getId() + "\n"
                        + newRepos.stream().map(s -> "    " + s).collect(Collectors.joining("\n")));
            }
        }

        return resultModel;
    }

    private List<Profile> interpolateActivations(
            List<Profile> profiles, DefaultProfileActivationContext context, DefaultModelProblemCollector problems) {
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
            DefaultModelProblemCollector problems,
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
        return build(request, result, new LinkedHashSet<>());
    }

    public Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException {
        request = fillRequestDefaults(request);
        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(new DefaultModelBuilderResult());
        Model model = readRawModel(request, problems);
        if (hasModelErrors(problems)) {
            throw problems.newModelBuilderException();
        }
        return model;
    }

    private ModelBuilderResult build(
            ModelBuilderRequest request, final ModelBuilderResult phaseOneResult, Collection<String> importIds)
            throws ModelBuilderException {
        DefaultModelBuilderResult result = asDefaultModelBuilderResult(phaseOneResult);

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(result);

        // phase 2
        Model resultModel = readEffectiveModel(request, result, problems);
        problems.setSource(resultModel);
        problems.setRootModel(resultModel);

        // model path translation
        resultModel = modelPathTranslator.alignToBaseDirectory(resultModel, resultModel.getProjectDirectory(), request);

        // plugin management injection
        resultModel = pluginManagementInjector.injectManagement(resultModel, request, problems);

        resultModel = fireEvent(resultModel, request, problems, ModelBuildingListener::buildExtensionsAssembled);

        if (request.isProcessPlugins()) {
            if (lifecycleBindingsInjector == null) {
                throw new IllegalStateException("lifecycle bindings injector is missing");
            }

            // lifecycle bindings injection
            resultModel = lifecycleBindingsInjector.injectLifecycleBindings(resultModel, request, problems);
        }

        // dependency management import
        resultModel = importDependencyManagement(resultModel, request, problems, importIds);

        // dependency management injection
        resultModel = dependencyManagementInjector.injectManagement(resultModel, request, problems);

        resultModel = modelNormalizer.injectDefaultValues(resultModel, request, problems);

        if (request.isProcessPlugins()) {
            // plugins configuration
            resultModel = pluginConfigurationExpander.expandPluginConfiguration(resultModel, request, problems);
        }

        result.setEffectiveModel(resultModel);

        // effective model validation
        modelValidator.validateEffectiveModel(resultModel, request, problems);

        if (hasModelErrors(problems)) {
            throw problems.newModelBuilderException();
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

    public Result<? extends Model> buildRawModel(Path pomFile, int validationLevel, boolean locationTracking) {
        return buildRawModel(pomFile, validationLevel, locationTracking, null);
    }

    public Result<? extends Model> buildRawModel(
            Path pomFile, int validationLevel, boolean locationTracking, ModelTransformerContext context) {
        final ModelBuilderRequest request = ModelBuilderRequest.builder()
                .validationLevel(validationLevel)
                .locationTracking(locationTracking)
                .source(ModelSource.fromPath(pomFile))
                .build();
        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(new DefaultModelBuilderResult());
        try {
            Model model = readFileModel(request, problems);

            try {
                if (transformer != null && context != null) {
                    transformer.transform(context, model, pomFile);
                }
            } catch (ModelBuilderException e) {
                problems.add(Severity.FATAL, ModelProblem.Version.V40, null, e);
            }

            return Result.newResult(model, problems.getProblems());
        } catch (ModelBuilderException e) {
            return Result.error(problems.getProblems());
        }
    }

    Model readFileModel(ModelBuilderRequest request, DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        ModelSource modelSource = request.getSource();
        Model model =
                cache(getModelCache(request), modelSource, FILE, () -> doReadFileModel(modelSource, request, problems));

        if (modelSource.getPath() != null) {
            if (getTransformerContextBuilder(request) instanceof DefaultModelTransformerContextBuilder contextBuilder) {
                contextBuilder.putSource(getGroupId(model), model.getArtifactId(), modelSource);
            }
        }

        return model;
    }

    @SuppressWarnings("checkstyle:methodlength")
    private Model doReadFileModel(
            ModelSource modelSource, ModelBuilderRequest request, DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        Model model;
        problems.setSource(modelSource.getLocation());
        try {
            boolean strict = request.getValidationLevel() >= ModelBuilderRequest.VALIDATION_LEVEL_MAVEN_2_0;

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

                Severity severity = request.isProjectBuild() ? Severity.ERROR : Severity.WARNING;
                problems.add(
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
            problems.add(
                    Severity.FATAL,
                    ModelProblem.Version.BASE,
                    "Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage(),
                    e);
            throw problems.newModelBuilderException();
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
            problems.add(
                    Severity.FATAL,
                    ModelProblem.Version.BASE,
                    "Non-readable POM " + modelSource.getLocation() + ": " + msg,
                    e);
            throw problems.newModelBuilderException();
        }

        if (modelSource.getPath() != null) {
            model = model.withPomFile(modelSource.getPath());
        }

        problems.setSource(model);
        modelValidator.validateFileModel(model, request, problems);
        if (hasFatalErrors(problems)) {
            throw problems.newModelBuilderException();
        }

        return model;
    }

    Model readRawModel(ModelBuilderRequest request, DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        ModelSource modelSource = request.getSource();

        ModelData modelData =
                cache(getModelCache(request), modelSource, RAW, () -> doReadRawModel(modelSource, request, problems));

        return modelData.model();
    }

    private ModelData doReadRawModel(
            ModelSource modelSource, ModelBuilderRequest request, DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        Model rawModel = readFileModel(request, problems);
        if (Features.buildConsumer(request.getUserProperties()) && modelSource.getPath() != null) {
            Path pomFile = modelSource.getPath();

            try {
                ModelTransformerContextBuilder transformerContextBuilder = getTransformerContextBuilder(request);
                if (transformerContextBuilder != null) {
                    ModelTransformerContext context = transformerContextBuilder.initialize(request, problems);
                    rawModel = this.transformer.transform(context, rawModel, pomFile);
                }
            } catch (ModelTransformerException e) {
                problems.add(Severity.FATAL, ModelProblem.Version.V40, null, e);
            }
        }

        String namespace = rawModel.getNamespaceUri();
        if (rawModel.getModelVersion() == null && namespace != null && namespace.startsWith(NAMESPACE_PREFIX)) {
            rawModel = rawModel.withModelVersion(namespace.substring(NAMESPACE_PREFIX.length()));
        }

        modelValidator.validateRawModel(rawModel, request, problems);

        if (hasFatalErrors(problems)) {
            throw problems.newModelBuilderException();
        }

        return new ModelData(modelSource, rawModel);
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
        if (request.getValidationLevel() < ModelBuilderRequest.VALIDATION_LEVEL_MAVEN_2_0) {
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

    private ModelData readParent(
            Model childModel,
            ModelSource childSource,
            ModelBuilderRequest request,
            DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        ModelData parentData = null;

        Parent parent = childModel.getParent();
        if (parent != null) {
            parentData = readParentLocally(childModel, childSource, request, problems);
            if (parentData == null) {
                parentData = readParentExternally(childModel, request, problems);
            }

            Model parentModel = parentData.model();
            if (!"pom".equals(parentModel.getPackaging())) {
                problems.add(
                        Severity.ERROR,
                        ModelProblem.Version.BASE,
                        "Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"",
                        parentModel.getLocation("packaging"));
            }
        }

        return parentData;
    }

    private ModelData readParentLocally(
            Model childModel,
            ModelSource childSource,
            ModelBuilderRequest request,
            DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        final Parent parent = childModel.getParent();
        final ModelSource candidateSource;
        final Model candidateModel;
        final WorkspaceModelResolver resolver = getWorkspaceModelResolver(request);
        if (resolver == null) {
            candidateSource = getParentPomFile(childModel, childSource);

            if (candidateSource == null) {
                return null;
            }

            ModelBuilderRequest candidateBuildRequest = ModelBuilderRequest.build(request, candidateSource);

            candidateModel = readRawModel(candidateBuildRequest, problems);
        } else {
            try {
                candidateModel =
                        resolver.resolveRawModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            } catch (ModelBuilderException e) {
                problems.add(Severity.FATAL, ModelProblem.Version.BASE, e.getMessage(), parent.getLocation(""), e);
                throw problems.newModelBuilderException();
            }
            if (candidateModel == null) {
                return null;
            }
            candidateSource = ModelSource.fromPath(candidateModel.getPomFile());
        }

        //
        // TODO jvz Why isn't all this checking the job of the duty of the workspace resolver, we know that we
        // have a model that is suitable, yet more checks are done here and the one for the version is problematic
        // before because with parents as ranges it will never work in this scenario.
        //

        String groupId = getGroupId(candidateModel);
        String artifactId = candidateModel.getArtifactId();

        if (groupId == null
                || !groupId.equals(parent.getGroupId())
                || artifactId == null
                || !artifactId.equals(parent.getArtifactId())) {
            StringBuilder buffer = new StringBuilder(256);
            buffer.append("'parent.relativePath'");
            if (childModel != problems.getRootModel()) {
                buffer.append(" of POM ").append(ModelProblemUtils.toSourceHint(childModel));
            }
            buffer.append(" points at ").append(groupId).append(':').append(artifactId);
            buffer.append(" instead of ").append(parent.getGroupId()).append(':');
            buffer.append(parent.getArtifactId()).append(", please verify your project structure");

            problems.setSource(childModel);
            problems.add(Severity.WARNING, ModelProblem.Version.BASE, buffer.toString(), parent.getLocation(""));
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
                    problems.add(
                            Severity.FATAL,
                            ModelProblem.Version.V31,
                            "Version must be a constant",
                            childModel.getLocation(""));

                } else {
                    if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                        // Message below is checked for in the MNG-2199 core IT.
                        problems.add(
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

    private ModelData readParentExternally(
            Model childModel, ModelBuilderRequest request, DefaultModelProblemCollector problems)
            throws ModelBuilderException {
        problems.setSource(childModel);

        Parent parent = childModel.getParent();

        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();

        ModelResolver modelResolver = getModelResolver(request);
        Objects.requireNonNull(
                modelResolver,
                String.format(
                        "request.modelResolver cannot be null (parent POM %s and POM %s)",
                        ModelProblemUtils.toId(groupId, artifactId, version),
                        ModelProblemUtils.toSourceHint(childModel)));

        ModelSource modelSource;
        try {
            AtomicReference<Parent> modified = new AtomicReference<>();
            Session session = request.getSession()
                    .withRemoteRepositories(request.getModelRepositoryHolder().getRepositories());
            modelSource = modelResolver.resolveModel(session, parent, modified);
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
            if (childModel != problems.getRootModel()) {
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

            problems.add(Severity.FATAL, ModelProblem.Version.BASE, buffer.toString(), parent.getLocation(""), e);
            throw problems.newModelBuilderException();
        }

        int validationLevel = Math.min(request.getValidationLevel(), ModelBuilderRequest.VALIDATION_LEVEL_MAVEN_2_0);
        ModelBuilderRequest lenientRequest = ModelBuilderRequest.builder(request)
                .validationLevel(validationLevel)
                .projectBuild(false)
                .source(modelSource)
                .build();

        Model parentModel = readRawModel(lenientRequest, problems);

        if (!parent.getVersion().equals(version)) {
            String rawChildModelVersion = childModel.getVersion();

            if (rawChildModelVersion == null) {
                // Message below is checked for in the MNG-2199 core IT.
                problems.add(
                        Severity.FATAL,
                        ModelProblem.Version.V31,
                        "Version must be a constant",
                        childModel.getLocation(""));

            } else {
                if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                    // Message below is checked for in the MNG-2199 core IT.
                    problems.add(
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

    private Model getSuperModel(String modelVersion) {
        return superPomProvider.getSuperPom(modelVersion);
    }

    private Model importDependencyManagement(
            Model model,
            ModelBuilderRequest request,
            DefaultModelProblemCollector problems,
            Collection<String> importIds) {
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

            DependencyManagement importMgmt = loadDependencyManagement(model, request, problems, dependency, importIds);

            if (importMgmt != null) {
                if (importMgmts == null) {
                    importMgmts = new ArrayList<>();
                }

                importMgmts.add(importMgmt);
            }
        }

        importIds.remove(importing);

        model = model.withDependencyManagement(model.getDependencyManagement().withDependencies(deps));

        return dependencyManagementImporter.importManagement(model, importMgmts, request, problems);
    }

    private DependencyManagement loadDependencyManagement(
            Model model,
            ModelBuilderRequest request,
            DefaultModelProblemCollector problems,
            Dependency dependency,
            Collection<String> importIds) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();

        if (groupId == null || groupId.isEmpty()) {
            problems.add(
                    Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "'dependencyManagement.dependencies.dependency.groupId' for " + dependency.getManagementKey()
                            + " is missing.",
                    dependency.getLocation(""));
            return null;
        }
        if (artifactId == null || artifactId.isEmpty()) {
            problems.add(
                    Severity.ERROR,
                    ModelProblem.Version.BASE,
                    "'dependencyManagement.dependencies.dependency.artifactId' for " + dependency.getManagementKey()
                            + " is missing.",
                    dependency.getLocation(""));
            return null;
        }
        if (version == null || version.isEmpty()) {
            problems.add(
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
            problems.add(Severity.ERROR, ModelProblem.Version.BASE, message.toString());

            return null;
        }

        Model importModel = cache(
                getModelCache(request),
                groupId,
                artifactId,
                version,
                IMPORT,
                () -> doLoadDependencyManagement(
                        model, request, problems, dependency, groupId, artifactId, version, importIds));
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
            Model model,
            ModelBuilderRequest request,
            DefaultModelProblemCollector problems,
            Dependency dependency,
            String groupId,
            String artifactId,
            String version,
            Collection<String> importIds) {
        final WorkspaceModelResolver workspaceResolver = getWorkspaceModelResolver(request);
        final ModelResolver modelResolver = getModelResolver(request);
        if (workspaceResolver == null && modelResolver == null) {
            throw new NullPointerException(String.format(
                    "request.workspaceModelResolver and request.modelResolver cannot be null (parent POM %s and POM %s)",
                    ModelProblemUtils.toId(groupId, artifactId, version), ModelProblemUtils.toSourceHint(model)));
        }

        Model importModel = null;
        if (workspaceResolver != null) {
            try {
                importModel = workspaceResolver.resolveEffectiveModel(groupId, artifactId, version);
            } catch (ModelBuilderException e) {
                problems.add(Severity.FATAL, ModelProblem.Version.BASE, null, e);
                return null;
            }
        }

        // no workspace resolver or workspace resolver returned null (i.e. model not in workspace)
        if (importModel == null) {
            final ModelSource importSource;
            try {
                Session session = request.getSession()
                        .withRemoteRepositories(
                                request.getModelRepositoryHolder().getRepositories());
                importSource = modelResolver.resolveModel(session, dependency, new AtomicReference<>());
            } catch (ModelBuilderException e) {
                StringBuilder buffer = new StringBuilder(256);
                buffer.append("Non-resolvable import POM");
                if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                    buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
                }
                buffer.append(": ").append(e.getMessage());

                problems.add(
                        Severity.ERROR, ModelProblem.Version.BASE, buffer.toString(), dependency.getLocation(""), e);
                return null;
            }

            Path rootDirectory;
            try {
                rootDirectory = request.getSession().getRootDirectory();
            } catch (IllegalStateException e) {
                rootDirectory = null;
            }
            if (importSource.getPath() != null && rootDirectory != null) {
                Path sourcePath = importSource.getPath();
                if (sourcePath.startsWith(rootDirectory)) {
                    problems.add(
                            Severity.WARNING,
                            ModelProblem.Version.BASE,
                            "BOM imports from within reactor should be avoided",
                            dependency.getLocation(""));
                }
            }

            final ModelBuilderResult importResult;
            try {
                ModelBuilderRequest importRequest = ModelBuilderRequest.builder()
                        .session(request.getSession()
                                .withRemoteRepositories(
                                        request.getModelRepositoryHolder().getRepositories()))
                        .validationLevel(ModelBuilderRequest.VALIDATION_LEVEL_MINIMAL)
                        .systemProperties(request.getSystemProperties())
                        .userProperties(request.getUserProperties())
                        .source(importSource)
                        .modelResolver(modelResolver)
                        .modelCache(request.getModelCache())
                        .modelRepositoryHolder(
                                request.getModelRepositoryHolder().copy())
                        .twoPhaseBuilding(false)
                        .build();
                importResult = build(importRequest, importIds);
            } catch (ModelBuilderException e) {
                e.getResult().getProblems().forEach(problems::add);
                return null;
            }

            importResult.getProblems().forEach(problems::add);

            importModel = importResult.getEffectiveModel();
        }

        return importModel;
    }

    private static <T> T cache(
            ModelCache cache, String groupId, String artifactId, String version, String tag, Callable<T> supplier) {
        Supplier<T> s = asSupplier(supplier);
        if (cache == null) {
            return s.get();
        } else {
            return cache.computeIfAbsent(groupId, artifactId, version, tag, s);
        }
    }

    private static <T> T cache(ModelCache cache, Source source, String tag, Callable<T> supplier) {
        Supplier<T> s = asSupplier(supplier);
        if (cache == null) {
            return s.get();
        } else {
            return cache.computeIfAbsent(source, tag, s);
        }
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

    private static ModelCache getModelCache(ModelBuilderRequest request) {
        return request.getModelCache();
    }

    private static ModelBuildingListener getModelBuildingListener(ModelBuilderRequest request) {
        return (ModelBuildingListener) request.getListener();
    }

    private static WorkspaceModelResolver getWorkspaceModelResolver(ModelBuilderRequest request) {
        return null; // request.getWorkspaceModelResolver();
    }

    private static ModelResolver getModelResolver(ModelBuilderRequest request) {
        return request.getModelResolver();
    }

    private static ModelTransformerContextBuilder getTransformerContextBuilder(ModelBuilderRequest request) {
        return request.getTransformerContextBuilder();
    }
}
