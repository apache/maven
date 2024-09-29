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
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.VersionRange;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.services.VersionParserException;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.building.Source;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.composition.DependencyManagementImporter;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.interpolation.ModelVersionProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
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
import org.apache.maven.model.v4.MavenTransformer;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.model.validation.ModelValidator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.eclipse.sisu.Nullable;

import static org.apache.maven.api.services.ModelBuilder.MODEL_VERSION_4_0_0;
import static org.apache.maven.model.building.Result.error;
import static org.apache.maven.model.building.Result.newResult;

/**
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultModelBuilder implements ModelBuilder {

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
    private final ReportConfigurationExpander reportConfigurationExpander;
    private final ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;
    private final ModelVersionProcessor versionProcessor;
    private final ModelSourceTransformer transformer;
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
            ReportConfigurationExpander reportConfigurationExpander,
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator,
            ModelVersionProcessor versionProcessor,
            ModelSourceTransformer transformer,
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
        this.reportConfigurationExpander = reportConfigurationExpander;
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        this.versionProcessor = versionProcessor;
        this.transformer = transformer;
        this.versionParser = versionParser;
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setModelProcessor(ModelProcessor)
     */
    @Deprecated
    public DefaultModelBuilder setModelProcessor(ModelProcessor modelProcessor) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setModelProcessor(ModelProcessor)
     */
    @Deprecated
    public DefaultModelBuilder setModelValidator(ModelValidator modelValidator) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setModelNormalizer(ModelNormalizer)
     */
    @Deprecated
    public DefaultModelBuilder setModelNormalizer(ModelNormalizer modelNormalizer) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setModelInterpolator(ModelInterpolator)
     */
    @Deprecated
    public DefaultModelBuilder setModelInterpolator(ModelInterpolator modelInterpolator) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setModelPathTranslator(ModelPathTranslator)
     */
    @Deprecated
    public DefaultModelBuilder setModelPathTranslator(ModelPathTranslator modelPathTranslator) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setModelUrlNormalizer(ModelUrlNormalizer)
     */
    @Deprecated
    public DefaultModelBuilder setModelUrlNormalizer(ModelUrlNormalizer modelUrlNormalizer) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setSuperPomProvider(SuperPomProvider)
     */
    @Deprecated
    public DefaultModelBuilder setSuperPomProvider(SuperPomProvider superPomProvider) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setInheritanceAssembler(InheritanceAssembler)
     */
    @Deprecated
    public DefaultModelBuilder setInheritanceAssembler(InheritanceAssembler inheritanceAssembler) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setProfileSelector(ProfileSelector)
     */
    @Deprecated
    public DefaultModelBuilder setProfileSelector(ProfileSelector profileSelector) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setProfileInjector(ProfileInjector)
     */
    @Deprecated
    public DefaultModelBuilder setProfileInjector(ProfileInjector profileInjector) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setPluginManagementInjector(PluginManagementInjector)
     */
    @Deprecated
    public DefaultModelBuilder setPluginManagementInjector(PluginManagementInjector pluginManagementInjector) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setDependencyManagementInjector(DependencyManagementInjector)
     */
    @Deprecated
    public DefaultModelBuilder setDependencyManagementInjector(
            DependencyManagementInjector dependencyManagementInjector) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setDependencyManagementImporter(DependencyManagementImporter)
     */
    @Deprecated
    public DefaultModelBuilder setDependencyManagementImporter(
            DependencyManagementImporter dependencyManagementImporter) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setLifecycleBindingsInjector(LifecycleBindingsInjector)
     */
    @Deprecated
    public DefaultModelBuilder setLifecycleBindingsInjector(LifecycleBindingsInjector lifecycleBindingsInjector) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setPluginConfigurationExpander(PluginConfigurationExpander)
     */
    @Deprecated
    public DefaultModelBuilder setPluginConfigurationExpander(PluginConfigurationExpander pluginConfigurationExpander) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setReportConfigurationExpander(ReportConfigurationExpander)
     */
    @Deprecated
    public DefaultModelBuilder setReportConfigurationExpander(ReportConfigurationExpander reportConfigurationExpander) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setProfileActivationFilePathInterpolator(ProfileActivationFilePathInterpolator)
     */
    @Deprecated
    public DefaultModelBuilder setProfileActivationFilePathInterpolator(
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator) {
        return new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                profileSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                reportConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionProcessor,
                transformer,
                versionParser);
    }

    /**
     * @deprecated since Maven 4
     * @see DefaultModelBuilderFactory#setReportingConverter(ReportingConverter)
     */
    @Deprecated
    public DefaultModelBuilder setReportingConverter(ReportingConverter reportingConverter) {
        return this;
    }

    @Override
    public DefaultTransformerContextBuilder newTransformerContextBuilder() {
        return new DefaultTransformerContextBuilder(this);
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        return build(request, new LinkedHashSet<>());
    }

    protected ModelBuildingResult build(ModelBuildingRequest request, Collection<String> importIds)
            throws ModelBuildingException {
        // phase 1
        DefaultModelBuildingResult result = new DefaultModelBuildingResult();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(result);

        // read and validate raw model
        Model fileModel = readFileModel(request, problems);

        request.setFileModel(fileModel);
        result.setFileModel(fileModel.clone());

        activateFileModel(request, result, problems);

        if (!request.isTwoPhaseBuilding()) {
            return build(request, result, importIds);
        } else if (hasModelErrors(problems)) {
            throw problems.newModelBuildingException();
        }

        return result;
    }

    private void activateFileModel(
            final ModelBuildingRequest request,
            final DefaultModelBuildingResult result,
            DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        Model inputModel = request.getFileModel();
        problems.setRootModel(inputModel);

        // profile activation
        DefaultProfileActivationContext profileActivationContext = getProfileActivationContext(request);

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

        profileActivationContext.setProjectProperties(inputModel.getDelegate().getProperties());
        problems.setSource(inputModel);
        List<Profile> activePomProfiles =
                profileSelector.getActiveProfiles(inputModel.getProfiles(), profileActivationContext, problems);

        // model normalization
        problems.setSource(inputModel);
        inputModel.update(modelNormalizer.mergeDuplicates(inputModel.getDelegate(), request, problems));

        Map<String, Activation> interpolatedActivations = getProfileActivations(inputModel, false);
        injectProfileActivations(inputModel, interpolatedActivations);

        // profile injection
        for (Profile activeProfile : activePomProfiles) {
            profileInjector.injectProfile(inputModel, activeProfile, request, problems);
        }

        modelValidator.validateExternalProfiles(activeExternalProfiles, inputModel, request, problems);
        for (Profile activeProfile : activeExternalProfiles) {
            profileInjector.injectProfile(inputModel, activeProfile, request, problems);
        }
    }

    @SuppressWarnings("checkstyle:methodlength")
    private Model readEffectiveModel(
            final ModelBuildingRequest request,
            final DefaultModelBuildingResult result,
            DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        Model inputModel = readRawModel(request, problems);
        if (problems.hasFatalErrors()) {
            throw problems.newModelBuildingException();
        }

        problems.setRootModel(inputModel);

        ModelData resultData = new ModelData(request.getModelSource(), inputModel);
        String superModelVersion =
                inputModel.getModelVersion() != null ? inputModel.getModelVersion() : MODEL_VERSION_4_0_0;
        if (!DefaultModelValidator.VALID_MODEL_VERSIONS.contains(superModelVersion)) {
            // Maven 3.x is always using 4.0.0 version to load the supermodel, so
            // do the same when loading a dependency.  The model validator will also
            // check that field later.
            superModelVersion = MODEL_VERSION_4_0_0;
        }
        ModelData superData = new ModelData(null, getSuperModel(superModelVersion));

        // profile activation
        DefaultProfileActivationContext profileActivationContext = getProfileActivationContext(request);

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
            String modelId = currentData.getId();
            result.addModelId(modelId);

            Model model = currentData.getModel();
            result.setRawModel(modelId, model);
            problems.setSource(model);
            org.apache.maven.api.model.Model modelv4 = model.getDelegate();

            // model normalization
            modelv4 = modelNormalizer.mergeDuplicates(modelv4, request, problems);

            // profile activation
            profileActivationContext.setProjectProperties(modelv4.getProperties());

            List<org.apache.maven.api.model.Profile> interpolatedProfiles =
                    interpolateActivations(modelv4.getProfiles(), profileActivationContext, problems);

            // profile injection
            List<org.apache.maven.api.model.Profile> activePomProfiles =
                    profileSelector.getActiveProfilesV4(interpolatedProfiles, profileActivationContext, problems);
            result.setActivePomProfiles(
                    modelId, activePomProfiles.stream().map(Profile::new).collect(Collectors.toList()));
            modelv4 = profileInjector.injectProfiles(modelv4, activePomProfiles, request, problems);
            if (currentData == resultData) {
                for (Profile activeProfile : activeExternalProfiles) {
                    modelv4 = profileInjector.injectProfile(modelv4, activeProfile.getDelegate(), request, problems);
                }
            }

            lineage.add(new Model(modelv4));

            if (currentData == superData) {
                break;
            }

            // add repositories specified by the current model so that we can resolve the parent
            configureResolver(request.getModelResolver(), modelv4, problems, false);

            // we pass a cloned model, so that resolving the parent version does not affect the returned model
            ModelData parentData = readParent(new Model(modelv4), currentData.getSource(), request, problems);

            if (parentData == null) {
                currentData = superData;
            } else if (!parentIds.add(parentData.getId())) {
                StringBuilder message = new StringBuilder("The parents form a cycle: ");
                for (String parentId : parentIds) {
                    message.append(parentId).append(" -> ");
                }
                message.append(parentData.getId());

                problems.add(new ModelProblemCollectorRequest(ModelProblem.Severity.FATAL, ModelProblem.Version.BASE)
                        .setMessage(message.toString()));

                throw problems.newModelBuildingException();
            } else {
                currentData = parentData;
            }
        }

        Model tmpModel = lineage.get(0);

        // inject interpolated activations
        List<org.apache.maven.api.model.Profile> interpolated =
                interpolateActivations(tmpModel.getDelegate().getProfiles(), profileActivationContext, problems);
        if (interpolated != tmpModel.getDelegate().getProfiles()) {
            tmpModel.update(tmpModel.getDelegate().withProfiles(interpolated));
        }

        // inject external profile into current model
        tmpModel.update(profileInjector.injectProfiles(
                tmpModel.getDelegate(),
                activeExternalProfiles.stream().map(Profile::getDelegate).collect(Collectors.toList()),
                request,
                problems));

        checkPluginVersions(lineage, request, problems);

        // inheritance assembly
        Model resultModel = assembleInheritance(lineage, request, problems);

        // consider caching inherited model

        problems.setSource(resultModel);
        problems.setRootModel(resultModel);

        // model interpolation
        resultModel = interpolateModel(resultModel, request, problems);

        // url normalization
        modelUrlNormalizer.normalize(resultModel, request);

        result.setEffectiveModel(resultModel);

        // Now the fully interpolated model is available: reconfigure the resolver
        configureResolver(request.getModelResolver(), resultModel.getDelegate(), problems, true);

        return resultModel;
    }

    private List<org.apache.maven.api.model.Profile> interpolateActivations(
            List<org.apache.maven.api.model.Profile> profiles,
            DefaultProfileActivationContext context,
            DefaultModelProblemCollector problems) {
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

        class ProfileInterpolator extends MavenTransformer
                implements UnaryOperator<org.apache.maven.api.model.Profile> {
            ProfileInterpolator() {
                super(s -> {
                    if (isNotEmpty(s)) {
                        try {
                            return xform.interpolate(s);
                        } catch (InterpolationException e) {
                            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                                    .setMessage(e.getMessage())
                                    .setException(e));
                        }
                    }
                    return s;
                });
            }

            @Override
            public org.apache.maven.api.model.Profile apply(org.apache.maven.api.model.Profile p) {
                return org.apache.maven.api.model.Profile.newBuilder(p)
                        .activation(transformActivation(p.getActivation()))
                        .build();
            }

            @Override
            protected ActivationFile.Builder transformActivationFile_Missing(
                    Supplier<? extends ActivationFile.Builder> creator,
                    ActivationFile.Builder builder,
                    ActivationFile target) {
                final String path = target.getMissing();
                final String xformed = transformPath(path, target, "missing");
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
            org.apache.maven.api.model.InputLocationTracker target,
            String path,
            InterpolationException e,
            String locationKey) {
        problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                .setMessage("Failed to interpolate file location " + path + ": " + e.getMessage())
                .setLocation(Optional.ofNullable(target.getLocation(locationKey))
                        .map(InputLocation::new)
                        .orElse(null))
                .setException(e));
    }

    private static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    @Override
    public ModelBuildingResult build(final ModelBuildingRequest request, final ModelBuildingResult result)
            throws ModelBuildingException {
        return build(request, result, new LinkedHashSet<>());
    }

    public Model buildRawModel(final ModelBuildingRequest request) throws ModelBuildingException {
        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(new DefaultModelBuildingResult());
        Model model = readRawModel(request, problems);
        if (hasModelErrors(problems)) {
            throw problems.newModelBuildingException();
        }
        return model;
    }

    private ModelBuildingResult build(
            final ModelBuildingRequest request, final ModelBuildingResult phaseOneResult, Collection<String> imports)
            throws ModelBuildingException {
        DefaultModelBuildingResult result = asDefaultModelBuildingResult(phaseOneResult);

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(result);

        // phase 2
        Model resultModel = readEffectiveModel(request, result, problems);
        problems.setSource(resultModel);
        problems.setRootModel(resultModel);

        // model path translation
        modelPathTranslator.alignToBaseDirectory(resultModel, resultModel.getProjectDirectoryPath(), request);

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

        resultModel.update(modelNormalizer.injectDefaultValues(resultModel.getDelegate(), request, problems));

        if (request.isProcessPlugins()) {
            // reports configuration
            reportConfigurationExpander.expandPluginConfiguration(resultModel, request, problems);

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

    private DefaultModelBuildingResult asDefaultModelBuildingResult(ModelBuildingResult phaseOneResult) {
        if (phaseOneResult instanceof DefaultModelBuildingResult) {
            return (DefaultModelBuildingResult) phaseOneResult;
        } else {
            return new DefaultModelBuildingResult(phaseOneResult);
        }
    }

    @Deprecated
    @Override
    public Result<? extends Model> buildRawModel(File pomFile, int validationLevel, boolean locationTracking) {
        return buildRawModel(pomFile.toPath(), validationLevel, locationTracking, null);
    }

    @Override
    public Result<? extends Model> buildRawModel(Path pomFile, int validationLevel, boolean locationTracking) {
        return buildRawModel(pomFile, validationLevel, locationTracking, null);
    }

    @Deprecated
    @Override
    public Result<? extends Model> buildRawModel(
            File pomFile, int validationLevel, boolean locationTracking, TransformerContext context) {
        return buildRawModel(pomFile.toPath(), validationLevel, locationTracking, context);
    }

    @Override
    public Result<? extends Model> buildRawModel(
            Path pomFile, int validationLevel, boolean locationTracking, TransformerContext context) {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest()
                .setValidationLevel(validationLevel)
                .setLocationTracking(locationTracking)
                .setModelSource(new FileModelSource(pomFile));
        DefaultModelProblemCollector problems = new DefaultModelProblemCollector(new DefaultModelBuildingResult());
        try {
            Model model = readFileModel(request, problems);

            try {
                if (transformer != null && context != null) {
                    transformer.transform(pomFile, context, model);
                }
            } catch (TransformerException e) {
                problems.add(
                        new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.V40).setException(e));
            }

            return newResult(model, problems.getProblems());
        } catch (ModelBuildingException e) {
            return error(problems.getProblems());
        }
    }

    Model readFileModel(ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        ModelSource modelSource = request.getModelSource();
        org.apache.maven.api.model.Model model = cache(
                request.getModelCache(),
                modelSource,
                ModelCacheTag.FILE,
                () -> doReadFileModel(modelSource, request, problems));

        if (modelSource instanceof FileModelSource) {
            if (request.getTransformerContextBuilder() instanceof DefaultTransformerContextBuilder) {
                DefaultTransformerContextBuilder contextBuilder =
                        (DefaultTransformerContextBuilder) request.getTransformerContextBuilder();
                contextBuilder.putSource(getGroupId(model), model.getArtifactId(), (FileModelSource) modelSource);
            }
        }

        return new Model(model);
    }

    @SuppressWarnings("checkstyle:methodlength")
    private org.apache.maven.api.model.Model doReadFileModel(
            ModelSource modelSource, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        org.apache.maven.api.model.Model model;
        problems.setSource(modelSource.getLocation());
        try {
            boolean strict = request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0;

            Map<String, Object> options = new HashMap<>(3);
            options.put(ModelProcessor.IS_STRICT, strict);
            options.put(ModelProcessor.SOURCE, modelSource);
            options.put(ModelReader.ROOT_DIRECTORY, request.getRootDirectory());

            InputSource source;
            if (request.isLocationTracking()) {
                source = new InputSource(null, modelSource.getLocation());
                options.put(ModelProcessor.INPUT_SOURCE, new org.apache.maven.model.InputSource(source));
            } else {
                source = null;
            }

            try {
                model = modelProcessor
                        .read(modelSource.getInputStream(), options)
                        .getDelegate();
            } catch (ModelParseException e) {
                if (!strict) {
                    throw e;
                }

                options.put(ModelProcessor.IS_STRICT, Boolean.FALSE);

                try {
                    model = modelProcessor
                            .read(modelSource.getInputStream(), options)
                            .getDelegate();
                } catch (ModelParseException ne) {
                    // still unreadable even in non-strict mode, rethrow original error
                    throw e;
                }

                Severity severity = (modelSource instanceof FileModelSource) ? Severity.ERROR : Severity.WARNING;
                problems.add(new ModelProblemCollectorRequest(severity, ModelProblem.Version.V20)
                        .setMessage("Malformed POM " + modelSource.getLocation() + ": " + e.getMessage())
                        .setException(e));
            }

            if (source != null) {
                try {
                    org.apache.maven.api.model.InputLocation loc = model.getLocation("");
                    org.apache.maven.api.model.InputSource v4src = loc != null ? loc.getSource() : null;
                    if (v4src != null) {
                        Field field = InputSource.class.getDeclaredField("modelId");
                        field.setAccessible(true);
                        field.set(v4src, ModelProblemUtils.toId(model));
                    }
                } catch (Throwable t) {
                    // TODO: use a lazy source ?
                    throw new IllegalStateException("Unable to set modelId on InputSource", t);
                }
            }
        } catch (ModelParseException e) {
            problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.BASE)
                    .setMessage("Non-parseable POM " + modelSource.getLocation() + ": " + e.getMessage())
                    .setException(e));
            throw problems.newModelBuildingException();
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
            problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.BASE)
                    .setMessage("Non-readable POM " + modelSource.getLocation() + ": " + msg)
                    .setException(e));
            throw problems.newModelBuildingException();
        }

        if (modelSource instanceof FileModelSource) {
            model = model.withPomFile(((FileModelSource) modelSource).getPath());
        }

        Model retModel = new Model(model);

        problems.setSource(retModel);

        modelValidator.validateFileModel(retModel, request, problems);

        if (hasFatalErrors(problems)) {
            throw problems.newModelBuildingException();
        }

        return model;
    }

    Model readRawModel(ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        ModelSource modelSource = request.getModelSource();

        ModelData modelData = cache(
                request.getModelCache(),
                modelSource,
                ModelCacheTag.RAW,
                () -> doReadRawModel(modelSource, request, problems));

        return modelData.getModel();
    }

    private ModelData doReadRawModel(
            ModelSource modelSource, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        Model rawModel;
        if (modelSource instanceof FileModelSource) {
            rawModel = readFileModel(request, problems);

            if (!MODEL_VERSION_4_0_0.equals(rawModel.getModelVersion())) {
                File pomFile = ((FileModelSource) modelSource).getFile();

                try {
                    if (request.getTransformerContextBuilder() != null) {
                        TransformerContext context =
                                request.getTransformerContextBuilder().initialize(request, problems);
                        transformer.transform(pomFile.toPath(), context, rawModel);
                    }
                } catch (TransformerException e) {
                    problems.add(
                            new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.V40).setException(e));
                }
            }
        } else if (request.getFileModel() == null) {
            rawModel = readFileModel(request, problems);
        } else {
            rawModel = request.getFileModel().clone();
        }

        modelValidator.validateRawModel(rawModel, request, problems);

        if (hasFatalErrors(problems)) {
            throw problems.newModelBuildingException();
        }

        String groupId = getGroupId(rawModel);
        String artifactId = rawModel.getArtifactId();
        String version = getVersion(rawModel);

        return new ModelData(modelSource, rawModel, groupId, artifactId, version);
    }

    String getGroupId(Model model) {
        return getGroupId(model.getDelegate());
    }

    private String getGroupId(org.apache.maven.api.model.Model model) {
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

    private DefaultProfileActivationContext getProfileActivationContext(ModelBuildingRequest request) {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();

        context.setActiveProfileIds(request.getActiveProfileIds());
        context.setInactiveProfileIds(request.getInactiveProfileIds());
        context.setSystemProperties(request.getSystemProperties());
        // enrich user properties with project packaging
        Properties userProperties = request.getUserProperties();
        if (!userProperties.containsKey(ProfileActivationContext.PROPERTY_NAME_PACKAGING)) {
            userProperties.put(
                    ProfileActivationContext.PROPERTY_NAME_PACKAGING,
                    request.getFileModel().getPackaging());
        }
        context.setUserProperties(userProperties);
        context.setProjectDirectory(
                (request.getPomFile() != null) ? request.getPomFile().getParentFile() : null);

        return context;
    }

    private void configureResolver(
            ModelResolver modelResolver,
            org.apache.maven.api.model.Model model,
            DefaultModelProblemCollector problems,
            boolean replaceRepositories) {
        if (modelResolver != null) {
            for (org.apache.maven.api.model.Repository repository : model.getRepositories()) {
                try {
                    modelResolver.addRepository(repository, replaceRepositories);
                } catch (InvalidRepositoryException e) {
                    problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                            .setMessage("Invalid repository " + repository.getId() + ": " + e.getMessage())
                            .setLocation(new InputLocation(repository.getLocation("")))
                            .setException(e));
                }
            }
        }
    }

    private void checkPluginVersions(
            List<Model> lineage, ModelBuildingRequest request, ModelProblemCollector problems) {
        if (request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
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
                problems.add(new ModelProblemCollectorRequest(Severity.WARNING, ModelProblem.Version.V20)
                        .setMessage("'build.plugins.plugin.version' for " + key + " is missing.")
                        .setLocation(location));
            }
        }
    }

    private Model assembleInheritance(
            List<Model> lineage, ModelBuildingRequest request, ModelProblemCollector problems) {
        org.apache.maven.api.model.Model parent =
                lineage.get(lineage.size() - 1).getDelegate();
        for (int i = lineage.size() - 2; i >= 0; i--) {
            Model child = lineage.get(i);
            parent = inheritanceAssembler.assembleModelInheritance(child.getDelegate(), parent, request, problems);
        }
        return new Model(parent);
    }

    private Map<String, Activation> getProfileActivations(Model model, boolean clone) {
        Map<String, Activation> activations = new HashMap<>();
        for (Profile profile : model.getProfiles()) {
            Activation activation = profile.getActivation();

            if (activation == null) {
                continue;
            }

            if (clone) {
                activation = activation.clone();
            }

            activations.put(profile.getId(), activation);
        }

        return activations;
    }

    private void injectProfileActivations(Model model, Map<String, Activation> activations) {
        for (Profile profile : model.getProfiles()) {
            Activation activation = profile.getActivation();

            if (activation == null) {
                continue;
            }

            // restore activation
            profile.setActivation(activations.get(profile.getId()));
        }
    }

    private Model interpolateModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        // save profile activations before interpolation, since they are evaluated with limited scope
        Map<String, Activation> originalActivations = getProfileActivations(model, true);

        Model interpolatedModel = new Model(modelInterpolator.interpolateModel(
                model.getDelegate(), model.getProjectDirectoryPath(), request, problems));
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
                ModelProblemCollectorRequest mpcr = new ModelProblemCollectorRequest(
                                Severity.ERROR, ModelProblem.Version.BASE)
                        .setMessage("Failed to interpolate field: "
                                + interpolatedModel.getParent().getVersion()
                                + " on class: ")
                        .setException(e);
                problems.add(mpcr);
            }
        }
        interpolatedModel.setPomPath(model.getPomPath());

        // restore profiles with file activation to their value before full interpolation
        injectProfileActivations(model, originalActivations);

        return interpolatedModel;
    }

    private ModelData readParent(
            Model childModel, Source childSource, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        ModelData parentData = null;

        Parent parent = childModel.getParent();
        if (parent != null) {
            parentData = readParentLocally(childModel, childSource, request, problems);
            if (parentData == null) {
                parentData = readParentExternally(childModel, request, problems);
            }

            Model parentModel = parentData.getModel();
            if (!"pom".equals(parentModel.getPackaging())) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                        .setMessage("Invalid packaging for parent POM " + ModelProblemUtils.toSourceHint(parentModel)
                                + ", must be \"pom\" but is \"" + parentModel.getPackaging() + "\"")
                        .setLocation(parentModel.getLocation("packaging")));
            }
        }

        return parentData;
    }

    private ModelData readParentLocally(
            Model childModel, Source childSource, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        final Parent parent = childModel.getParent();
        final ModelSource2 candidateSource;
        final Model candidateModel;
        final WorkspaceModelResolver resolver = request.getWorkspaceModelResolver();
        if (resolver == null) {
            candidateSource = getParentPomFile(childModel, childSource);

            if (candidateSource == null) {
                return null;
            }

            ModelBuildingRequest candidateBuildRequest =
                    new DefaultModelBuildingRequest(request).setModelSource(candidateSource);

            candidateModel = readRawModel(candidateBuildRequest, problems);
        } else {
            try {
                candidateModel =
                        resolver.resolveRawModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            } catch (UnresolvableModelException e) {
                problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.BASE) //
                        .setMessage(e.getMessage())
                        .setLocation(parent.getLocation(""))
                        .setException(e));
                throw problems.newModelBuildingException();
            }
            if (candidateModel == null) {
                return null;
            }
            candidateSource = new FileModelSource(candidateModel.getPomPath());
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
            problems.add(new ModelProblemCollectorRequest(Severity.WARNING, ModelProblem.Version.BASE)
                    .setMessage(buffer.toString())
                    .setLocation(parent.getLocation("")));
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
                    problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.V31)
                            .setMessage("Version must be a constant")
                            .setLocation(childModel.getLocation("")));

                } else {
                    if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                        // Message below is checked for in the MNG-2199 core IT.
                        problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.V31)
                                .setMessage("Version must be a constant")
                                .setLocation(childModel.getLocation("version")));
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

        return new ModelData(candidateSource, candidateModel, groupId, artifactId, version);
    }

    private boolean rawChildVersionReferencesParent(String rawChildModelVersion) {
        return rawChildModelVersion.equals("${pom.version}")
                || rawChildModelVersion.equals("${project.version}")
                || rawChildModelVersion.equals("${pom.parent.version}")
                || rawChildModelVersion.equals("${project.parent.version}");
    }

    private ModelSource2 getParentPomFile(Model childModel, Source source) {
        if (!(source instanceof ModelSource2)) {
            return null;
        }

        String parentPath = childModel.getParent().getRelativePath();

        if (parentPath == null || parentPath.isEmpty()) {
            return null;
        }

        if (source instanceof ModelSource3) {
            return ((ModelSource3) source).getRelatedSource(modelProcessor, parentPath);
        } else {
            return ((ModelSource2) source).getRelatedSource(parentPath);
        }
    }

    private ModelData readParentExternally(
            Model childModel, ModelBuildingRequest request, DefaultModelProblemCollector problems)
            throws ModelBuildingException {
        problems.setSource(childModel);

        Parent parent = childModel.getParent();

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
            if (childModel.getProjectDirectoryPath() != null) {
                if (parent.getRelativePath() == null || parent.getRelativePath().isEmpty()) {
                    buffer.append(" and 'parent.relativePath' points at no local POM");
                } else {
                    buffer.append(" and 'parent.relativePath' points at wrong local POM");
                }
            }

            problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.BASE)
                    .setMessage(buffer.toString())
                    .setLocation(parent.getLocation(""))
                    .setException(e));
            throw problems.newModelBuildingException();
        }

        int validationLevel = Math.min(request.getValidationLevel(), ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0);
        ModelBuildingRequest lenientRequest = new DefaultModelBuildingRequest(request)
                .setValidationLevel(validationLevel)
                .setFileModel(null)
                .setModelSource(modelSource);

        Model parentModel = readRawModel(lenientRequest, problems);

        if (!parent.getVersion().equals(version)) {
            String rawChildModelVersion = childModel.getVersion();

            if (rawChildModelVersion == null) {
                // Message below is checked for in the MNG-2199 core IT.
                problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.V31)
                        .setMessage("Version must be a constant")
                        .setLocation(childModel.getLocation("")));

            } else {
                if (rawChildVersionReferencesParent(rawChildModelVersion)) {
                    // Message below is checked for in the MNG-2199 core IT.
                    problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.V31)
                            .setMessage("Version must be a constant")
                            .setLocation(childModel.getLocation("version")));
                }
            }

            // MNG-2199: What else to check here ?
        }

        return new ModelData(
                modelSource, parentModel, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    private Model getSuperModel(String modelVersion) {
        return superPomProvider.getSuperModel(modelVersion);
    }

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

        // Model v4
        List<org.apache.maven.api.model.DependencyManagement> importMgmts = new ArrayList<>();

        for (Iterator<Dependency> it = depMgmt.getDependencies().iterator(); it.hasNext(); ) {
            Dependency dependency = it.next();

            if (!("pom".equals(dependency.getType()) && "import".equals(dependency.getScope()))
                    || "bom".equals(dependency.getType())) {
                continue;
            }

            it.remove();

            // Model v3
            DependencyManagement importMgmt = loadDependencyManagement(model, request, problems, dependency, importIds);
            if (importMgmt == null) {
                continue;
            }

            if (request.isLocationTracking()) {
                // Keep track of why this DependencyManagement was imported.
                // And map model v3 to model v4 -> importMgmt(v3).getDelegate() returns a v4 object
                importMgmts.add(
                        org.apache.maven.api.model.DependencyManagement.newBuilder(importMgmt.getDelegate(), true)
                                .build());
            } else {
                importMgmts.add(importMgmt.getDelegate());
            }
        }

        importIds.remove(importing);

        model.update(
                dependencyManagementImporter.importManagement(model.getDelegate(), importMgmts, request, problems));
    }

    private DependencyManagement loadDependencyManagement(
            Model model,
            ModelBuildingRequest request,
            DefaultModelProblemCollector problems,
            Dependency dependency,
            Collection<String> importIds) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();

        if (groupId == null || groupId.isEmpty()) {
            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                    .setMessage("'dependencyManagement.dependencies.dependency.groupId' for "
                            + dependency.getManagementKey() + " is missing.")
                    .setLocation(dependency.getLocation("")));
            return null;
        }
        if (artifactId == null || artifactId.isEmpty()) {
            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                    .setMessage("'dependencyManagement.dependencies.dependency.artifactId' for "
                            + dependency.getManagementKey() + " is missing.")
                    .setLocation(dependency.getLocation("")));
            return null;
        }
        if (version == null || version.isEmpty()) {
            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                    .setMessage("'dependencyManagement.dependencies.dependency.version' for "
                            + dependency.getManagementKey() + " is missing.")
                    .setLocation(dependency.getLocation("")));
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
            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                    .setMessage(message.toString()));

            return null;
        }

        org.apache.maven.api.model.DependencyManagement importMgmt = cache(
                request.getModelCache(),
                groupId,
                artifactId,
                version,
                ModelCacheTag.IMPORT,
                () -> doLoadDependencyManagement(
                        model, request, problems, dependency, groupId, artifactId, version, importIds));

        // [MNG-5600] Dependency management import should support exclusions.
        List<Exclusion> exclusions = dependency.getDelegate().getExclusions();
        if (importMgmt != null && !exclusions.isEmpty()) {
            // Dependency excluded from import.
            List<org.apache.maven.api.model.Dependency> dependencies = importMgmt.getDependencies().stream()
                    .filter(candidate -> exclusions.stream().noneMatch(exclusion -> match(exclusion, candidate)))
                    .map(candidate -> addExclusions(candidate, exclusions))
                    .collect(Collectors.toList());
            importMgmt = importMgmt.withDependencies(dependencies);
        }

        return importMgmt != null ? new DependencyManagement(importMgmt) : null;
    }

    private static org.apache.maven.api.model.Dependency addExclusions(
            org.apache.maven.api.model.Dependency candidate, List<Exclusion> exclusions) {
        return candidate.withExclusions(Stream.concat(candidate.getExclusions().stream(), exclusions.stream())
                .toList());
    }

    private boolean match(Exclusion exclusion, org.apache.maven.api.model.Dependency candidate) {
        return match(exclusion.getGroupId(), candidate.getGroupId())
                && match(exclusion.getArtifactId(), candidate.getArtifactId());
    }

    private boolean match(String match, String text) {
        return match.equals("*") || match.equals(text);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private org.apache.maven.api.model.DependencyManagement doLoadDependencyManagement(
            Model model,
            ModelBuildingRequest request,
            DefaultModelProblemCollector problems,
            Dependency dependency,
            String groupId,
            String artifactId,
            String version,
            Collection<String> importIds) {
        DependencyManagement importMgmt;
        final WorkspaceModelResolver workspaceResolver = request.getWorkspaceModelResolver();
        final ModelResolver modelResolver = request.getModelResolver();
        if (workspaceResolver == null && modelResolver == null) {
            throw new NullPointerException(String.format(
                    "request.workspaceModelResolver and request.modelResolver cannot be null (parent POM %s and POM %s)",
                    ModelProblemUtils.toId(groupId, artifactId, version), ModelProblemUtils.toSourceHint(model)));
        }

        Model importModel = null;
        if (workspaceResolver != null) {
            try {
                importModel = workspaceResolver.resolveEffectiveModel(groupId, artifactId, version);
            } catch (UnresolvableModelException e) {
                problems.add(new ModelProblemCollectorRequest(Severity.FATAL, ModelProblem.Version.BASE)
                        .setMessage(e.getMessage())
                        .setException(e));
                return null;
            }
        }

        // no workspace resolver or workspace resolver returned null (i.e. model not in workspace)
        if (importModel == null) {
            final ModelSource importSource;
            try {
                importSource = modelResolver.resolveModel(dependency);
            } catch (UnresolvableModelException e) {
                StringBuilder buffer = new StringBuilder(256);
                buffer.append("Non-resolvable import POM");
                if (!containsCoordinates(e.getMessage(), groupId, artifactId, version)) {
                    buffer.append(' ').append(ModelProblemUtils.toId(groupId, artifactId, version));
                }
                buffer.append(": ").append(e.getMessage());

                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, ModelProblem.Version.BASE)
                        .setMessage(buffer.toString())
                        .setLocation(dependency.getLocation(""))
                        .setException(e));
                return null;
            }

            if (importSource instanceof FileModelSource && request.getRootDirectory() != null) {
                Path sourcePath = ((FileModelSource) importSource).getPath();
                if (sourcePath.startsWith(request.getRootDirectory())) {
                    problems.add(new ModelProblemCollectorRequest(Severity.WARNING, ModelProblem.Version.BASE)
                            .setMessage("BOM imports from within reactor should be avoided")
                            .setLocation(dependency.getLocation("")));
                }
            }

            final ModelBuildingResult importResult;
            try {
                ModelBuildingRequest importRequest = new DefaultModelBuildingRequest();
                importRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                importRequest.setModelCache(request.getModelCache());
                importRequest.setSystemProperties(request.getSystemProperties());
                importRequest.setUserProperties(request.getUserProperties());
                importRequest.setLocationTracking(request.isLocationTracking());

                importRequest.setModelSource(importSource);
                importRequest.setModelResolver(modelResolver.newCopy());

                importResult = build(importRequest, importIds);
            } catch (ModelBuildingException e) {
                problems.addAll(e.getProblems());
                return null;
            }

            problems.addAll(importResult.getProblems());

            importModel = importResult.getEffectiveModel();
        }

        importMgmt = importModel.getDependencyManagement();

        if (importMgmt == null) {
            importMgmt = new DependencyManagement();
        }
        return importMgmt.getDelegate();
    }

    private static <T> T cache(
            ModelCache cache,
            String groupId,
            String artifactId,
            String version,
            ModelCacheTag<T> tag,
            Callable<T> supplier) {
        Supplier<T> s = asSupplier(supplier);
        if (cache == null) {
            return s.get();
        } else {
            return cache.computeIfAbsent(groupId, artifactId, version, tag.getName(), s);
        }
    }

    private static <T> T cache(ModelCache cache, Source source, ModelCacheTag<T> tag, Callable<T> supplier) {
        Supplier<T> s = asSupplier(supplier);
        if (cache == null) {
            return s.get();
        } else {
            return cache.computeIfAbsent(source, tag.getName(), s);
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

    private void fireEvent(
            Model model,
            ModelBuildingRequest request,
            ModelProblemCollector problems,
            ModelBuildingEventCatapult catapult) {
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

    ModelProcessor getModelProcessor() {
        return modelProcessor;
    }
}
