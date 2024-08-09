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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.composition.DependencyManagementImporter;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.plugin.ReportingConverter;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.model.validation.ModelValidator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.sisu.Nullable;

import static org.apache.maven.model.building.Result.error;
import static org.apache.maven.model.building.Result.newResult;

/**
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultModelBuilder implements ModelBuilder {
    @Inject
    private ModelProcessor modelProcessor;

    @Inject
    private ModelValidator modelValidator;

    @Inject
    private ModelNormalizer modelNormalizer;

    @Inject
    private ModelInterpolator modelInterpolator;

    @Inject
    private ModelPathTranslator modelPathTranslator;

    @Inject
    private ModelUrlNormalizer modelUrlNormalizer;

    @Inject
    private SuperPomProvider superPomProvider;

    @Inject
    private InheritanceAssembler inheritanceAssembler;

    @Inject
    private ProfileSelector profileSelector;

    @Inject
    private ProfileInjector profileInjector;

    @Inject
    private PluginManagementInjector pluginManagementInjector;

    @Inject
    private DependencyManagementInjector dependencyManagementInjector;

    @Inject
    private DependencyManagementImporter dependencyManagementImporter;

    @Inject
    @Nullable
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Inject
    private PluginConfigurationExpander pluginConfigurationExpander;

    @Inject
    private ReportConfigurationExpander reportConfigurationExpander;

    @Inject
    private ReportingConverter reportingConverter;

    @Inject
    private ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;

    public DefaultModelBuilder setModelProcessor(ModelProcessor modelProcessor) {
        this.modelProcessor = modelProcessor;
        return this;
    }

    public DefaultModelBuilder setModelValidator(ModelValidator modelValidator) {
        this.modelValidator = modelValidator;
        return this;
    }

    public DefaultModelBuilder setModelNormalizer(ModelNormalizer modelNormalizer) {
        this.modelNormalizer = modelNormalizer;
        return this;
    }

    public DefaultModelBuilder setModelInterpolator(ModelInterpolator modelInterpolator) {
        this.modelInterpolator = modelInterpolator;
        return this;
    }

    public DefaultModelBuilder setModelPathTranslator(ModelPathTranslator modelPathTranslator) {
        this.modelPathTranslator = modelPathTranslator;
        return this;
    }

    public DefaultModelBuilder setModelUrlNormalizer(ModelUrlNormalizer modelUrlNormalizer) {
        this.modelUrlNormalizer = modelUrlNormalizer;
        return this;
    }

    public DefaultModelBuilder setSuperPomProvider(SuperPomProvider superPomProvider) {
        this.superPomProvider = superPomProvider;
        return this;
    }

    public DefaultModelBuilder setProfileSelector(ProfileSelector profileSelector) {
        this.profileSelector = profileSelector;
        return this;
    }

    public DefaultModelBuilder setProfileInjector(ProfileInjector profileInjector) {
        this.profileInjector = profileInjector;
        return this;
    }

    public DefaultModelBuilder setInheritanceAssembler(InheritanceAssembler inheritanceAssembler) {
        this.inheritanceAssembler = inheritanceAssembler;
        return this;
    }

    public DefaultModelBuilder setDependencyManagementImporter(DependencyManagementImporter depMgmtImporter) {
        this.dependencyManagementImporter = depMgmtImporter;
        return this;
    }

    public DefaultModelBuilder setDependencyManagementInjector(DependencyManagementInjector depMgmtInjector) {
        this.dependencyManagementInjector = depMgmtInjector;
        return this;
    }

    public DefaultModelBuilder setLifecycleBindingsInjector(LifecycleBindingsInjector lifecycleBindingsInjector) {
        this.lifecycleBindingsInjector = lifecycleBindingsInjector;
        return this;
    }

    public DefaultModelBuilder setPluginConfigurationExpander(PluginConfigurationExpander pluginConfigurationExpander) {
        this.pluginConfigurationExpander = pluginConfigurationExpander;
        return this;
    }

    public DefaultModelBuilder setPluginManagementInjector(PluginManagementInjector pluginManagementInjector) {
        this.pluginManagementInjector = pluginManagementInjector;
        return this;
    }

    public DefaultModelBuilder setReportConfigurationExpander(ReportConfigurationExpander reportConfigurationExpander) {
        this.reportConfigurationExpander = reportConfigurationExpander;
        return this;
    }

    public DefaultModelBuilder setReportingConverter(ReportingConverter reportingConverter) {
        this.reportingConverter = reportingConverter;
        return this;
    }

    public DefaultModelBuilder setProfileActivationFilePathInterpolator(
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator) {
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        return this;
    }

    @SuppressWarnings("checkstyle:methodlength")
    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        return build(request, new LinkedHashSet<String>());
    }

    @SuppressWarnings("checkstyle:methodlength")
    protected ModelBuildingResult build(ModelBuildingRequest request, Collection<String> importIds)
            throws ModelBuildingException {
        // phase 1
        DefaultModelBuildingResult result = new DefaultModelBuildingResult();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(result);

        // read and validate raw model
        Model inputModel = request.getRawModel();
        if (inputModel == null) {
            inputModel = readModel(request.getModelSource(), request.getPomFile(), request, problems);
        }

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

        problems.setRootModel(inputModel);

        ModelData resultData = new ModelData(request.getModelSource(), inputModel);
        ModelData superData = new ModelData(null, getSuperModel());

        Collection<String> parentIds = new LinkedHashSet<>();
        List<ModelData> lineage = new ArrayList<>();

        for (ModelData currentData = resultData; currentData != null; ) {
            lineage.add(currentData);

            Model rawModel = currentData.getModel();
            currentData.setRawModel(rawModel);

            Model tmpModel = rawModel.clone();
            currentData.setModel(tmpModel);

            problems.setSource(tmpModel);

            // model normalization
            modelNormalizer.mergeDuplicates(tmpModel, request, problems);

            profileActivationContext.setProjectProperties(tmpModel.getProperties());

            List<Profile> interpolatedProfiles = getInterpolatedProfiles(rawModel, profileActivationContext, problems);
            tmpModel.setProfiles(interpolatedProfiles);

            List<Profile> activePomProfiles =
                    profileSelector.getActiveProfiles(tmpModel.getProfiles(), profileActivationContext, problems);

            List<Profile> rawProfiles = new ArrayList<>();
            for (Profile activePomProfile : activePomProfiles) {
                rawProfiles.add(rawModel.getProfiles().get(interpolatedProfiles.indexOf(activePomProfile)));
            }
            currentData.setActiveProfiles(rawProfiles);

            // profile injection
            for (Profile activeProfile : activePomProfiles) {
                profileInjector.injectProfile(tmpModel, activeProfile, request, problems);
            }

            if (currentData == resultData) {
                for (Profile activeProfile : activeExternalProfiles) {
                    profileInjector.injectProfile(tmpModel, activeProfile, request, problems);
                }
            }

            if (currentData == superData) {
                break;
            }

            configureResolver(request.getModelResolver(), tmpModel, problems);

            ModelData parentData = readParent(tmpModel, currentData.getSource(), request, problems);

            if (parentData == null) {
                currentData = superData;
            } else if (currentData == resultData) { // First iteration - add initial id after version resolution.
                currentData.setGroupId(
                        currentData.getRawModel().getGroupId() == null
                                ? parentData.getGroupId()
                                : currentData.getRawModel().getGroupId());

                currentData.setVersion(
                        currentData.getRawModel().getVersion() == null
                                ? parentData.getVersion()
                                : currentData.getRawModel().getVersion());

                currentData.setArtifactId(currentData.getRawModel().getArtifactId());
                parentIds.add(currentData.getId());
                // Reset - only needed for 'getId'.
                currentData.setGroupId(null);
                currentData.setArtifactId(null);
                currentData.setVersion(null);
                currentData = parentData;
            } else if (!parentIds.add(parentData.getId())) {
                String message = "The parents form a cycle: ";
                for (String modelId : parentIds) {
                    message += modelId + " -> ";
                }
                message += parentData.getId();

                problems.add(new ModelProblemCollectorRequest(ModelProblem.Severity.FATAL, ModelProblem.Version.BASE)
                        .setMessage(message));

                throw problems.newModelBuildingException();
            } else {
                currentData = parentData;
            }
        }

        problems.setSource(inputModel);
        checkPluginVersions(lineage, request, problems);

        // inheritance assembly
        assembleInheritance(lineage, request, problems);

        Model resultModel = resultData.getModel();

        problems.setSource(resultModel);
        problems.setRootModel(resultModel);

        // model interpolation
        resultModel = interpolateModel(resultModel, request, problems);
        resultData.setModel(resultModel);

        if (resultModel.getParent() != null) {
            final ModelData parentData = lineage.get(1);
            if (parentData.getVersion() == null || parentData.getVersion().contains("${")) {
                final Model interpolatedParent = interpolateModel(parentData.getModel(), request, problems);
                // parentData.setModel( interpolatedParent );
                parentData.setVersion(interpolatedParent.getVersion());
            }
        }

        // url normalization
        modelUrlNormalizer.normalize(resultModel, request);

        // Now the fully interpolated model is available: reconfigure the resolver
        configureResolver(request.getModelResolver(), resultModel, problems, true);

        resultData.setGroupId(resultModel.getGroupId());
        resultData.setArtifactId(resultModel.getArtifactId());
        resultData.setVersion(resultModel.getVersion());

        result.setEffectiveModel(resultModel);

        for (ModelData currentData : lineage) {
            String modelId = (currentData != superData) ? currentData.getId() : "";

            result.addModelId(modelId);
            result.setActivePomProfiles(modelId, currentData.getActiveProfiles());
            result.setRawModel(modelId, currentData.getRawModel());
        }

        if (!request.isTwoPhaseBuilding()) {
            build(request, result, importIds);
        }

        return result;
    }

    @FunctionalInterface
    private interface InterpolateString {
        String apply(String s) throws InterpolationException;
    }

    private List<Profile> getInterpolatedProfiles(
            Model rawModel, DefaultProfileActivationContext context, DefaultModelProblemCollector problems) {
        List<Profile> interpolatedActivations = getProfiles(rawModel, true);

        if (interpolatedActivations.isEmpty()) {
            return Collections.emptyList();
        }
        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource(new MapBasedValueSource(context.getProjectProperties()));
        interpolator.addValueSource(new MapBasedValueSource(context.getUserProperties()));
        interpolator.addValueSource(new MapBasedValueSource(context.getSystemProperties()));

        class Interpolation {
            final InputLocationTracker target;

            final InterpolateString impl;

            Interpolation(InputLocationTracker target, InterpolateString impl) {
                this.target = target;
                this.impl = impl;
            }

            void performFor(String value, String locationKey, Consumer<String> mutator) {
                if (StringUtils.isEmpty(value)) {
                    return;
                }
                try {
                    mutator.accept(impl.apply(value));
                } catch (InterpolationException e) {
                    problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                            .setMessage("Failed to interpolate value " + value + ": " + e.getMessage())
                            .setLocation(target.getLocation(locationKey))
                            .setException(e));
                }
            }
        }
        for (Profile profile : interpolatedActivations) {
            Activation activation = profile.getActivation();
            Optional<Activation> a = Optional.ofNullable(activation);
            a.map(Activation::getFile).ifPresent(fa -> {
                Interpolation nt =
                        new Interpolation(fa, s -> profileActivationFilePathInterpolator.interpolate(s, context));
                nt.performFor(fa.getExists(), "exists", fa::setExists);
                nt.performFor(fa.getMissing(), "missing", fa::setMissing);
            });
            a.map(Activation::getOs).ifPresent(oa -> {
                Interpolation nt = new Interpolation(oa, interpolator::interpolate);
                nt.performFor(oa.getArch(), "arch", oa::setArch);
                nt.performFor(oa.getFamily(), "family", oa::setFamily);
                nt.performFor(oa.getName(), "name", oa::setName);
                nt.performFor(oa.getVersion(), "version", oa::setVersion);
            });
            a.map(Activation::getProperty).ifPresent(pa -> {
                Interpolation nt = new Interpolation(pa, interpolator::interpolate);
                nt.performFor(pa.getName(), "name", pa::setName);
                nt.performFor(pa.getValue(), "value", pa::setValue);
            });
            a.map(Activation::getJdk).ifPresent(ja -> new Interpolation(activation, interpolator::interpolate)
                    .performFor(ja, "jdk", activation::setJdk));
        }
        return interpolatedActivations;
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request, ModelBuildingResult result)
            throws ModelBuildingException {
        return build(request, result, new LinkedHashSet<String>());
    }

    private ModelBuildingResult build(
            ModelBuildingRequest request, ModelBuildingResult result, Collection<String> imports)
            throws ModelBuildingException {
        // phase 2
        Model resultModel = result.getEffectiveModel();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(result);
        problems.setSource(resultModel);
        problems.setRootModel(resultModel);

        // model path translation
        modelPathTranslator.alignToBaseDirectory(resultModel, resultModel.getProjectDirectory(), request);

        // plugin management injection
        pluginManagementInjector.injectManagement(resultModel, request, problems);

        fireEvent(resultModel, request, problems, ModelBuildingEventCatapult.BUILD_EXTENSIONS_ASSEMBLED);

        if (request.isProcessPlugins()) {
            if (lifecycleBindingsInjector == null) {
                throw new IllegalStateException("lifecycle bindings injector is missing");
            }

            // lifecycle bindings injection
            lifecycleBindingsInjector.injectLifecycleBindings(resultModel, request, problems);
        }

        // dependency management import
        importDependencyManagement(resultModel, request, problems, imports);

        // dependency management injection
        dependencyManagementInjector.injectManagement(resultModel, request, problems);

        modelNormalizer.injectDefaultValues(resultModel, request, problems);

        if (request.isProcessPlugins()) {
            // reports configuration
            reportConfigurationExpander.expandPluginConfiguration(resultModel, request, problems);

            // reports conversion to decoupled site plugin
            reportingConverter.convertReporting(resultModel, request, problems);

            // plugins configuration
            pluginConfigurationExpander.expandPluginConfiguration(resultModel, request, problems);
        }

        // effective model validation
        modelValidator.validateEffectiveModel(resultModel, request, problems);

        if (hasModelErrors(problems)) {
            throw problems.newModelBuildingException();
        }

        return result;
    }

    @Override
    public Result<? extends Model> buildRawModel(File pomFile, int validationLevel, boolean locationTracking) {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest()
                .setValidationLevel(validationLevel)
                .setLocationTracking(locationTracking);
        final DefaultModelProblemCollector collector =
                new DefaultModelProblemCollector(new DefaultModelBuildingResult());
        try {
            return newResult(readModel(null, pomFile, request, collector), collector.getProblems());
        } catch (ModelBuildingException e) {
            return error(collector.getProblems());
        }
    }

    private Model readModel(
            ModelSource modelSource, File pomFile, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        Model model;

        if (modelSource == null) {
            if (pomFile != null) {
                modelSource = new FileModelSource(pomFile);
            } else {
                throw new NullPointerException("neither pomFile nor modelSource can be null");
            }
        }

        problems.setSource(modelSource.getLocation());
        try {
            boolean strict = request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;
            InputSource source = request.isLocationTracking() ? new InputSource() : null;

            Map<String, Object> options = new HashMap<>();
            options.put(ModelProcessor.IS_STRICT, strict);
            options.put(ModelProcessor.INPUT_SOURCE, source);
            options.put(ModelProcessor.SOURCE, modelSource);

            try {
                model = modelProcessor.read(modelSource.getInputStream(), options);
            } catch (ModelParseException e) {
                if (!strict) {
                    throw e;
                }

                options.put(ModelProcessor.IS_STRICT, Boolean.FALSE);

                try {
                    model = modelProcessor.read(modelSource.getInputStream(), options);
                } catch (ModelParseException ne) {
                    // still unreadable even in non-strict mode, rethrow original error
                    throw e;
                }

                if (pomFile != null) {
                    problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.V20)
                            .setMessage("Malformed POM " + modelSource.getLocation() + ": " + e.getMessage())
                            .setException(e));
                } else {
                    problems.add(new ModelProblemCollectorRequest(Severity.WARNING, Version.V20)
                            .setMessage("Malformed POM " + modelSource.getLocation() + ": " + e.getMessage())
                            .setException(e));
                }
            }

            if (source != null) {
                source.setModelId(ModelProblemUtils.toId(model));
                source.setLocation(modelSource.getLocation());
            }
        } catch (ModelParseException e) {
            problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.BASE)
                    .setMessage("Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage())
                    .setException(e));
            throw problems.newModelBuildingException();
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null || msg.length() <= 0) {
                // NOTE: There's java.nio.charset.MalformedInputException and sun.io.MalformedInputException
                if (e.getClass().getName().endsWith("MalformedInputException")) {
                    msg = "Some input bytes do not match the file encoding.";
                } else {
                    msg = e.getClass().getSimpleName();
                }
            }
            problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.BASE)
                    .setMessage("Non-readable POM " + modelSource.getLocation() + ": " + msg)
                    .setException(e));
            throw problems.newModelBuildingException();
        }

        model.setPomFile(pomFile);

        problems.setSource(model);
        modelValidator.validateRawModel(model, request, problems);

        if (hasFatalErrors(problems)) {
            throw problems.newModelBuildingException();
        }

        return model;
    }

    private DefaultProfileActivationContext getProfileActivationContext(ModelBuildingRequest request, Model rawModel) {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();

        context.setActiveProfileIds(request.getActiveProfileIds());
        context.setInactiveProfileIds(request.getInactiveProfileIds());
        context.setSystemProperties(request.getSystemProperties());
        // enrich user properties with project packaging
        Properties userProperties = request.getUserProperties();
        userProperties.computeIfAbsent(
                (Object) ProfileActivationContext.PROPERTY_NAME_PACKAGING, (p) -> (Object) rawModel.getPackaging());
        context.setUserProperties(userProperties);
        context.setProjectDirectory(
                (request.getPomFile() != null) ? request.getPomFile().getParentFile() : null);

        return context;
    }

    private void configureResolver(ModelResolver modelResolver, Model model, DefaultModelProblemCollector problems) {
        configureResolver(modelResolver, model, problems, false);
    }

    private void configureResolver(
            ModelResolver modelResolver,
            Model model,
            DefaultModelProblemCollector problems,
            boolean replaceRepositories) {
        if (modelResolver == null) {
            return;
        }

        problems.setSource(model);

        List<Repository> repositories = model.getRepositories();

        for (Repository repository : repositories) {
            try {
                modelResolver.addRepository(repository, replaceRepositories);
            } catch (InvalidRepositoryException e) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage("Invalid repository " + repository.getId() + ": " + e.getMessage())
                        .setLocation(repository.getLocation(""))
                        .setException(e));
            }
        }
    }

    private void checkPluginVersions(
            List<ModelData> lineage, ModelBuildingRequest request, ModelProblemCollector problems) {
        if (request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            return;
        }

        Map<String, Plugin> plugins = new HashMap<>();
        Map<String, String> versions = new HashMap<>();
        Map<String, String> managedVersions = new HashMap<>();

        for (int i = lineage.size() - 1; i >= 0; i--) {
            Model model = lineage.get(i).getModel();
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
                        if (managedVersions.get(key) == null) {
                            managedVersions.put(key, plugin.getVersion());
                        }
                    }
                }
            }
        }

        for (String key : versions.keySet()) {
            if (versions.get(key) == null && managedVersions.get(key) == null) {
                InputLocation location = plugins.get(key).getLocation("");
                problems.add(new ModelProblemCollectorRequest(Severity.WARNING, Version.V20)
                        .setMessage("'build.plugins.plugin.version' for " + key + " is missing.")
                        .setLocation(location));
            }
        }
    }

    private void assembleInheritance(
            List<ModelData> lineage, ModelBuildingRequest request, ModelProblemCollector problems) {
        for (int i = lineage.size() - 2; i >= 0; i--) {
            Model parent = lineage.get(i + 1).getModel();
            Model child = lineage.get(i).getModel();
            inheritanceAssembler.assembleModelInheritance(child, parent, request, problems);
        }
    }

    private List<Profile> getProfiles(Model model, boolean clone) {
        ArrayList<Profile> profiles = new ArrayList<>();
        for (Profile profile : model.getProfiles()) {
            if (clone) {
                profile = profile.clone();
            }
            profiles.add(profile);
        }
        return profiles;
    }

    private Model interpolateModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        // save profile activations before interpolation, since they are evaluated with limited scope
        List<Profile> originalProfiles = getProfiles(model, true);

        Model interpolatedModel =
                modelInterpolator.interpolateModel(model, model.getProjectDirectory(), request, problems);
        if (interpolatedModel.getParent() != null) {
            StringSearchInterpolator ssi = new StringSearchInterpolator();
            ssi.addValueSource(new MapBasedValueSource(request.getUserProperties()));

            ssi.addValueSource(new MapBasedValueSource(model.getProperties()));

            ssi.addValueSource(new MapBasedValueSource(request.getSystemProperties()));

            try {
                String interpolated =
                        ssi.interpolate(interpolatedModel.getParent().getVersion());
                interpolatedModel.getParent().setVersion(interpolated);
            } catch (Exception e) {
                ModelProblemCollectorRequest mpcr = new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage("Failed to interpolate field: "
                                + interpolatedModel.getParent().getVersion()
                                + " on class: ")
                        .setException(e);
                problems.add(mpcr);
            }
        }
        interpolatedModel.setPomFile(model.getPomFile());

        // restore profiles with any activation to their value before full interpolation
        List<Profile> interpolatedProfiles = model.getProfiles();
        IntStream.range(0, interpolatedProfiles.size()).forEach(i -> interpolatedProfiles
                .get(i)
                .setActivation(originalProfiles.get(i).getActivation()));

        return interpolatedModel;
    }

    private ModelData readParent(
            Model childModel,
            ModelSource childSource,
            ModelBuildingRequest request,
            DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        ModelData parentData;

        Parent parent = childModel.getParent();

        if (parent != null) {
            String groupId = parent.getGroupId();
            String artifactId = parent.getArtifactId();
            String version = parent.getVersion();

            parentData = getCache(request.getModelCache(), groupId, artifactId, version, ModelCacheTag.RAW);

            if (parentData == null) {
                parentData = readParentLocally(childModel, childSource, request, problems);

                if (parentData == null) {
                    parentData = readParentExternally(childModel, request, problems);
                }

                putCache(request.getModelCache(), groupId, artifactId, version, ModelCacheTag.RAW, parentData);
            } else {
                /*
                 * NOTE: This is a sanity check of the cache hit. If the cached parent POM was locally resolved, the
                 * child's <relativePath> should point at that parent, too. If it doesn't, we ignore the cache and
                 * resolve externally, to mimic the behavior if the cache didn't exist in the first place. Otherwise,
                 * the cache would obscure a bad POM.
                 */

                File pomFile = parentData.getModel().getPomFile();
                if (pomFile != null) {
                    FileModelSource pomSource = new FileModelSource(pomFile);
                    ModelSource expectedParentSource = getParentPomFile(childModel, childSource);

                    if (expectedParentSource == null
                            || (expectedParentSource instanceof ModelSource2
                                    && !pomSource.equals(expectedParentSource))) {
                        parentData = readParentExternally(childModel, request, problems);
                    }
                }
            }

            Model parentModel = parentData.getModel();

            if (!"pom".equals(parentModel.getPackaging())) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage("Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"")
                        .setLocation(parentModel.getLocation("packaging")));
            }
        } else {
            parentData = null;
        }

        return parentData;
    }

    private ModelData readParentLocally(
            Model childModel,
            ModelSource childSource,
            ModelBuildingRequest request,
            DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        final Parent parent = childModel.getParent();
        final ModelSource candidateSource;
        final Model candidateModel;
        final WorkspaceModelResolver resolver = request.getWorkspaceModelResolver();
        if (resolver == null) {
            candidateSource = getParentPomFile(childModel, childSource);

            if (candidateSource == null) {
                return null;
            }

            File pomFile = null;
            if (candidateSource instanceof FileModelSource) {
                pomFile = ((FileModelSource) candidateSource).getPomFile();
            }

            candidateModel = readModel(candidateSource, pomFile, request, problems);
        } else {
            try {
                candidateModel =
                        resolver.resolveRawModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            } catch (UnresolvableModelException e) {
                problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.BASE) //
                        .setMessage(e.getMessage().toString())
                        .setLocation(parent.getLocation(""))
                        .setException(e));
                throw problems.newModelBuildingException();
            }
            if (candidateModel == null) {
                return null;
            }
            candidateSource = new FileModelSource(candidateModel.getPomFile());
        }

        //
        // TODO jvz Why isn't all this checking the job of the duty of the workspace resolver, we know that we
        // have a model that is suitable, yet more checks are done here and the one for the version is problematic
        // before because with parents as ranges it will never work in this scenario.
        //

        String groupId = candidateModel.getGroupId();
        if (groupId == null && candidateModel.getParent() != null) {
            groupId = candidateModel.getParent().getGroupId();
        }
        String artifactId = candidateModel.getArtifactId();
        String version = candidateModel.getVersion();
        if (version == null && candidateModel.getParent() != null) {
            version = candidateModel.getParent().getVersion();
        }

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
            problems.add(new ModelProblemCollectorRequest(Severity.WARNING, Version.BASE)
                    .setMessage(buffer.toString())
                    .setLocation(parent.getLocation("")));
            return null;
        }
        if (version != null && parent.getVersion() != null && !version.equals(parent.getVersion())) {
            try {
                VersionRange parentRange = VersionRange.createFromVersionSpec(parent.getVersion());
                if (!parentRange.hasRestrictions()) {
                    // the parent version is not a range, we have version skew, drop back to resolution from repo
                    return null;
                }
                if (!parentRange.containsVersion(new DefaultArtifactVersion(version))) {
                    // version skew drop back to resolution from the repository
                    return null;
                }

                // Validate versions aren't inherited when using parent ranges the same way as when read externally.
                String rawChildModelVersion = childModel.getVersion();

                if (rawChildModelVersion == null) {
                    // Message below is checked for in the MNG-2199 core IT.
                    problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.V31)
                            .setMessage("Version must be a constant")
                            .setLocation(childModel.getLocation("")));

                } else {
                    if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                        // Message below is checked for in the MNG-2199 core IT.
                        problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.V31)
                                .setMessage("Version must be a constant")
                                .setLocation(childModel.getLocation("version")));
                    }
                }

                // MNG-2199: What else to check here ?
            } catch (InvalidVersionSpecificationException e) {
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

        ModelData parentData = new ModelData(candidateSource, candidateModel, groupId, artifactId, version);

        return parentData;
    }

    private boolean rawChildVersionReferencesParent(String rawChildModelVersion) {
        return rawChildModelVersion.equals("${pom.version}")
                || rawChildModelVersion.equals("${project.version}")
                || rawChildModelVersion.equals("${pom.parent.version}")
                || rawChildModelVersion.equals("${project.parent.version}");
    }

    private ModelSource getParentPomFile(Model childModel, ModelSource source) {
        if (!(source instanceof ModelSource2)) {
            return null;
        }

        String parentPath = childModel.getParent().getRelativePath();

        if (parentPath == null || parentPath.length() <= 0) {
            return null;
        }

        return ((ModelSource2) source).getRelatedSource(parentPath);
    }

    private ModelData readParentExternally(
            Model childModel, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        problems.setSource(childModel);

        Parent parent = childModel.getParent().clone();

        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();

        ModelResolver modelResolver = request.getModelResolver();
        Objects.requireNonNull(
                modelResolver,
                String.format(
                        "request.modelResolver cannot be null (parent POM %s and POM %s)",
                        ModelProblemUtils.toId(groupId, artifactId, version),
                        ModelProblemUtils.toSourceHint(childModel)));

        ModelSource modelSource;
        try {
            modelSource = modelResolver.resolveModel(parent);
        } catch (UnresolvableModelException e) {
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
                if (parent.getRelativePath() == null || parent.getRelativePath().length() <= 0) {
                    buffer.append(" and 'parent.relativePath' points at no local POM");
                } else {
                    buffer.append(" and 'parent.relativePath' points at wrong local POM");
                }
            }

            problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.BASE)
                    .setMessage(buffer.toString())
                    .setLocation(parent.getLocation(""))
                    .setException(e));
            throw problems.newModelBuildingException();
        }

        ModelBuildingRequest lenientRequest = request;
        if (request.getValidationLevel() > ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            lenientRequest = new FilterModelBuildingRequest(request) {
                @Override
                public int getValidationLevel() {
                    return ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;
                }
            };
        }

        Model parentModel = readModel(modelSource, null, lenientRequest, problems);

        if (!parent.getVersion().equals(version)) {
            String rawChildModelVersion = childModel.getVersion();

            if (rawChildModelVersion == null) {
                // Message below is checked for in the MNG-2199 core IT.
                problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.V31)
                        .setMessage("Version must be a constant")
                        .setLocation(childModel.getLocation("")));

            } else {
                if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                    // Message below is checked for in the MNG-2199 core IT.
                    problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.V31)
                            .setMessage("Version must be a constant")
                            .setLocation(childModel.getLocation("version")));
                }
            }

            // MNG-2199: What else to check here ?
        }

        ModelData parentData = new ModelData(
                modelSource, parentModel, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());

        return parentData;
    }

    private Model getSuperModel() {
        return superPomProvider.getSuperModel("4.0.0").clone();
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void importDependencyManagement(
            Model model,
            ModelBuildingRequest request,
            DefaultModelProblemCollector problems,
            Collection<String> importIds) {
        DependencyManagement depMgmt = model.getDependencyManagement();

        if (depMgmt == null) {
            return;
        }

        String importing = model.getGroupId() + ':' + model.getArtifactId() + ':' + model.getVersion();

        importIds.add(importing);

        final WorkspaceModelResolver workspaceResolver = request.getWorkspaceModelResolver();
        final ModelResolver modelResolver = request.getModelResolver();

        ModelBuildingRequest importRequest = null;

        List<DependencyManagement> importMgmts = null;

        for (Iterator<Dependency> it = depMgmt.getDependencies().iterator(); it.hasNext(); ) {
            Dependency dependency = it.next();

            if (!"pom".equals(dependency.getType()) || !"import".equals(dependency.getScope())) {
                continue;
            }

            it.remove();

            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String version = dependency.getVersion();

            if (groupId == null || groupId.length() <= 0) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage("'dependencyManagement.dependencies.dependency.groupId' for "
                                + dependency.getManagementKey() + " is missing.")
                        .setLocation(dependency.getLocation("")));
                continue;
            }
            if (artifactId == null || artifactId.length() <= 0) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage("'dependencyManagement.dependencies.dependency.artifactId' for "
                                + dependency.getManagementKey() + " is missing.")
                        .setLocation(dependency.getLocation("")));
                continue;
            }
            if (version == null || version.length() <= 0) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage("'dependencyManagement.dependencies.dependency.version' for "
                                + dependency.getManagementKey() + " is missing.")
                        .setLocation(dependency.getLocation("")));
                continue;
            }

            String imported = groupId + ':' + artifactId + ':' + version;

            if (importIds.contains(imported)) {
                String message = "The dependencies of type=pom and with scope=import form a cycle: ";
                for (String modelId : importIds) {
                    message += modelId + " -> ";
                }
                message += imported;
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE).setMessage(message));

                continue;
            }

            DependencyManagement importMgmt =
                    getCache(request.getModelCache(), groupId, artifactId, version, ModelCacheTag.IMPORT);

            if (importMgmt == null) {
                if (workspaceResolver == null && modelResolver == null) {
                    throw new NullPointerException(String.format(
                            "request.workspaceModelResolver and request.modelResolver cannot be null"
                                    + " (parent POM %s and POM %s)",
                            ModelProblemUtils.toId(groupId, artifactId, version),
                            ModelProblemUtils.toSourceHint(model)));
                }

                Model importModel = null;
                if (workspaceResolver != null) {
                    try {
                        importModel = workspaceResolver.resolveEffectiveModel(groupId, artifactId, version);
                    } catch (UnresolvableModelException e) {
                        problems.add(new ModelProblemCollectorRequest(Severity.FATAL, Version.BASE)
                                .setMessage(e.getMessage().toString())
                                .setException(e));
                        continue;
                    }
                }

                // no workspace resolver or workspace resolver returned null (i.e. model not in workspace)
                if (importModel == null) {
                    final ModelSource importSource;
                    try {
                        importSource = modelResolver.resolveModel(groupId, artifactId, version);
                    } catch (UnresolvableModelException e) {
                        StringBuilder buffer = new StringBuilder(256);
                        buffer.append("Non-resolvable import POM");
                        if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                            buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
                        }
                        buffer.append(": ").append(e.getMessage());

                        problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                                .setMessage(buffer.toString())
                                .setLocation(dependency.getLocation(""))
                                .setException(e));
                        continue;
                    }

                    if (importRequest == null) {
                        importRequest = new DefaultModelBuildingRequest();
                        importRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                        importRequest.setModelCache(request.getModelCache());
                        importRequest.setSystemProperties(request.getSystemProperties());
                        importRequest.setUserProperties(request.getUserProperties());
                        importRequest.setLocationTracking(request.isLocationTracking());
                    }

                    importRequest.setModelSource(importSource);
                    importRequest.setModelResolver(modelResolver.newCopy());

                    final ModelBuildingResult importResult;
                    try {
                        importResult = build(importRequest, importIds);
                    } catch (ModelBuildingException e) {
                        problems.addAll(e.getProblems());
                        continue;
                    }

                    problems.addAll(importResult.getProblems());

                    importModel = importResult.getEffectiveModel();
                }

                importMgmt = importModel.getDependencyManagement();

                if (importMgmt == null) {
                    importMgmt = new DependencyManagement();
                }

                putCache(request.getModelCache(), groupId, artifactId, version, ModelCacheTag.IMPORT, importMgmt);
            }

            if (importMgmts == null) {
                importMgmts = new ArrayList<>();
            }

            importMgmts.add(importMgmt);
        }

        importIds.remove(importing);

        dependencyManagementImporter.importManagement(model, importMgmts, request, problems);
    }

    private <T> void putCache(
            ModelCache modelCache, String groupId, String artifactId, String version, ModelCacheTag<T> tag, T data) {
        if (modelCache != null) {
            modelCache.put(groupId, artifactId, version, tag.getName(), tag.intoCache(data));
        }
    }

    private <T> T getCache(
            ModelCache modelCache, String groupId, String artifactId, String version, ModelCacheTag<T> tag) {
        if (modelCache != null) {
            Object data = modelCache.get(groupId, artifactId, version, tag.getName());
            if (data != null) {
                return tag.fromCache(tag.getType().cast(data));
            }
        }
        return null;
    }

    private void fireEvent(
            Model model,
            ModelBuildingRequest request,
            ModelProblemCollector problems,
            ModelBuildingEventCatapult catapult)
            throws ModelBuildingException {
        ModelBuildingListener listener = request.getModelBuildingListener();

        if (listener != null) {
            ModelBuildingEvent event = new DefaultModelBuildingEvent(model, request, problems);

            catapult.fire(listener, event);
        }
    }

    private boolean containsCoordinates(String message, String groupId, String artifactId, String version) {
        return message != null
                && (groupId == null || message.contains(groupId))
                && (artifactId == null || message.contains(artifactId))
                && (version == null || message.contains(version));
    }

    protected boolean hasModelErrors(ModelProblemCollectorExt problems) {
        if (problems instanceof DefaultModelProblemCollector) {
            return ((DefaultModelProblemCollector) problems).hasErrors();
        } else {
            // the default execution path only knows the DefaultModelProblemCollector,
            // only reason it's not in signature is because it's package private
            throw new IllegalStateException();
        }
    }

    protected boolean hasFatalErrors(ModelProblemCollectorExt problems) {
        if (problems instanceof DefaultModelProblemCollector) {
            return ((DefaultModelProblemCollector) problems).hasFatalErrors();
        } else {
            // the default execution path only knows the DefaultModelProblemCollector,
            // only reason it's not in signature is because it's package private
            throw new IllegalStateException();
        }
    }
}
