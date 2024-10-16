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

import java.util.Arrays;

import org.apache.maven.api.Version;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.model.Model;
import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.composition.DependencyManagementImporter;
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.interpolation.ModelVersionProcessor;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.apache.maven.model.locator.ModelLocator;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;
import org.apache.maven.model.normalization.DefaultModelNormalizer;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.DefaultModelPathTranslator;
import org.apache.maven.model.path.DefaultModelUrlNormalizer;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.path.PathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.path.UrlNormalizer;
import org.apache.maven.model.plugin.DefaultPluginConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportConfigurationExpander;
import org.apache.maven.model.plugin.DefaultReportingConverter;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.plugin.ReportingConverter;
import org.apache.maven.model.profile.DefaultProfileInjector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.apache.maven.model.root.DefaultRootLocator;
import org.apache.maven.model.root.RootLocator;
import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.model.validation.ModelValidator;

import static java.util.Objects.requireNonNull;

/**
 * A factory to create model builder instances when no dependency injection is available. <em>Note:</em> This class is
 * only meant as a utility for developers that want to employ the model builder outside the Maven build system, Maven
 * plugins should always acquire model builder instances via dependency injection. Developers might want to subclass
 * this factory to provide custom implementations for some of the components used by the model builder, or use the
 * builder API to inject custom instances.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
public class DefaultModelBuilderFactory {

    private ModelProcessor modelProcessor;
    private ModelValidator modelValidator;
    private ModelNormalizer modelNormalizer;
    private ModelInterpolator modelInterpolator;
    private ModelPathTranslator modelPathTranslator;
    private ModelUrlNormalizer modelUrlNormalizer;
    private SuperPomProvider superPomProvider;
    private InheritanceAssembler inheritanceAssembler;
    private ProfileSelector profileSelector;
    private ProfileInjector profileInjector;
    private PluginManagementInjector pluginManagementInjector;
    private DependencyManagementInjector dependencyManagementInjector;
    private DependencyManagementImporter dependencyManagementImporter;
    private LifecycleBindingsInjector lifecycleBindingsInjector;
    private PluginConfigurationExpander pluginConfigurationExpander;
    private ReportConfigurationExpander reportConfigurationExpander;
    private ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;
    private ModelVersionProcessor versionProcessor;
    private ModelSourceTransformer transformer;
    private ModelVersionParser versionParser;

    public DefaultModelBuilderFactory setModelProcessor(ModelProcessor modelProcessor) {
        this.modelProcessor = modelProcessor;
        return this;
    }

    public DefaultModelBuilderFactory setModelValidator(ModelValidator modelValidator) {
        this.modelValidator = modelValidator;
        return this;
    }

    public DefaultModelBuilderFactory setModelNormalizer(ModelNormalizer modelNormalizer) {
        this.modelNormalizer = modelNormalizer;
        return this;
    }

    public DefaultModelBuilderFactory setModelInterpolator(ModelInterpolator modelInterpolator) {
        this.modelInterpolator = modelInterpolator;
        return this;
    }

    public DefaultModelBuilderFactory setModelPathTranslator(ModelPathTranslator modelPathTranslator) {
        this.modelPathTranslator = modelPathTranslator;
        return this;
    }

    public DefaultModelBuilderFactory setModelUrlNormalizer(ModelUrlNormalizer modelUrlNormalizer) {
        this.modelUrlNormalizer = modelUrlNormalizer;
        return this;
    }

    public DefaultModelBuilderFactory setSuperPomProvider(SuperPomProvider superPomProvider) {
        this.superPomProvider = superPomProvider;
        return this;
    }

    public DefaultModelBuilderFactory setInheritanceAssembler(InheritanceAssembler inheritanceAssembler) {
        this.inheritanceAssembler = inheritanceAssembler;
        return this;
    }

    public DefaultModelBuilderFactory setProfileSelector(ProfileSelector profileSelector) {
        this.profileSelector = profileSelector;
        return this;
    }

    public DefaultModelBuilderFactory setProfileInjector(ProfileInjector profileInjector) {
        this.profileInjector = profileInjector;
        return this;
    }

    public DefaultModelBuilderFactory setPluginManagementInjector(PluginManagementInjector pluginManagementInjector) {
        this.pluginManagementInjector = pluginManagementInjector;
        return this;
    }

    public DefaultModelBuilderFactory setDependencyManagementInjector(
            DependencyManagementInjector dependencyManagementInjector) {
        this.dependencyManagementInjector = dependencyManagementInjector;
        return this;
    }

    public DefaultModelBuilderFactory setDependencyManagementImporter(
            DependencyManagementImporter dependencyManagementImporter) {
        this.dependencyManagementImporter = dependencyManagementImporter;
        return this;
    }

    public DefaultModelBuilderFactory setLifecycleBindingsInjector(
            LifecycleBindingsInjector lifecycleBindingsInjector) {
        this.lifecycleBindingsInjector = lifecycleBindingsInjector;
        return this;
    }

    public DefaultModelBuilderFactory setPluginConfigurationExpander(
            PluginConfigurationExpander pluginConfigurationExpander) {
        this.pluginConfigurationExpander = pluginConfigurationExpander;
        return this;
    }

    public DefaultModelBuilderFactory setReportConfigurationExpander(
            ReportConfigurationExpander reportConfigurationExpander) {
        this.reportConfigurationExpander = reportConfigurationExpander;
        return this;
    }

    @Deprecated
    public DefaultModelBuilderFactory setReportingConverter(ReportingConverter reportingConverter) {
        return this;
    }

    public DefaultModelBuilderFactory setProfileActivationFilePathInterpolator(
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator) {
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        return this;
    }

    public DefaultModelBuilderFactory setVersionProcessor(ModelVersionProcessor versionProcessor) {
        this.versionProcessor = versionProcessor;
        return this;
    }

    public DefaultModelBuilderFactory setTransformer(ModelSourceTransformer transformer) {
        this.transformer = transformer;
        return this;
    }

    public DefaultModelBuilderFactory setModelVersionParser(ModelVersionParser versionParser) {
        this.versionParser = versionParser;
        return this;
    }

    protected ModelProcessor newModelProcessor() {
        return new DefaultModelProcessor(Arrays.asList(newModelParsers()), newModelLocator(), newModelReader());
    }

    protected ModelParser[] newModelParsers() {
        return new ModelParser[0];
    }

    protected ModelLocator newModelLocator() {
        return new DefaultModelLocator();
    }

    protected ModelReader newModelReader() {
        return new DefaultModelReader(newModelSourceTransformer());
    }

    protected ProfileSelector newProfileSelector() {
        return new DefaultProfileSelector(Arrays.asList(newProfileActivators()));
    }

    protected ProfileActivator[] newProfileActivators() {
        return new ProfileActivator[] {
            new JdkVersionProfileActivator(),
            new OperatingSystemProfileActivator(),
            new PropertyProfileActivator(),
            new FileProfileActivator(newProfileActivationFilePathInterpolator())
        };
    }

    protected ProfileActivationFilePathInterpolator newProfileActivationFilePathInterpolator() {
        return new ProfileActivationFilePathInterpolator(newPathTranslator(), newRootLocator());
    }

    protected UrlNormalizer newUrlNormalizer() {
        return new DefaultUrlNormalizer();
    }

    protected PathTranslator newPathTranslator() {
        return new DefaultPathTranslator();
    }

    protected RootLocator newRootLocator() {
        return new DefaultRootLocator();
    }

    protected ModelInterpolator newModelInterpolator() {
        UrlNormalizer normalizer = newUrlNormalizer();
        PathTranslator pathTranslator = newPathTranslator();
        RootLocator rootLocator = newRootLocator();
        return new StringVisitorModelInterpolator(pathTranslator, normalizer, rootLocator);
    }

    protected ModelVersionProcessor newModelVersionPropertiesProcessor() {
        return new DefaultModelVersionProcessor();
    }

    protected ModelValidator newModelValidator() {
        ModelVersionProcessor processor = newModelVersionPropertiesProcessor();
        return new DefaultModelValidator(processor);
    }

    protected ModelNormalizer newModelNormalizer() {
        return new DefaultModelNormalizer();
    }

    protected ModelPathTranslator newModelPathTranslator() {
        return new DefaultModelPathTranslator(newPathTranslator());
    }

    protected ModelUrlNormalizer newModelUrlNormalizer() {
        return new DefaultModelUrlNormalizer(newUrlNormalizer());
    }

    protected InheritanceAssembler newInheritanceAssembler() {
        return new DefaultInheritanceAssembler();
    }

    protected ProfileInjector newProfileInjector() {
        return new DefaultProfileInjector();
    }

    protected SuperPomProvider newSuperPomProvider() {
        return new DefaultSuperPomProvider(newModelProcessor());
    }

    protected DependencyManagementImporter newDependencyManagementImporter() {
        return new DefaultDependencyManagementImporter();
    }

    protected DependencyManagementInjector newDependencyManagementInjector() {
        return new DefaultDependencyManagementInjector();
    }

    protected LifecycleBindingsInjector newLifecycleBindingsInjector() {
        return new StubLifecycleBindingsInjector();
    }

    protected PluginManagementInjector newPluginManagementInjector() {
        return new DefaultPluginManagementInjector();
    }

    protected PluginConfigurationExpander newPluginConfigurationExpander() {
        return new DefaultPluginConfigurationExpander();
    }

    protected ReportConfigurationExpander newReportConfigurationExpander() {
        return new DefaultReportConfigurationExpander();
    }

    @Deprecated
    protected ReportingConverter newReportingConverter() {
        return new DefaultReportingConverter();
    }

    private ModelSourceTransformer newModelSourceTransformer() {
        return new BuildModelSourceTransformer();
    }

    private ModelVersionParser newModelVersionParser() {
        // This is a limited parser that does not support ranges and compares versions as strings
        // in real-life this parser should not be used, but replaced with a proper one
        return new ModelVersionParser() {
            @Override
            public Version parseVersion(String version) {
                requireNonNull(version, "version");
                return new Version() {
                    @Override
                    public String asString() {
                        return version;
                    }

                    @Override
                    public int compareTo(Version o) {
                        return version.compareTo(o.asString());
                    }
                };
            }

            @Override
            public VersionRange parseVersionRange(String range) {
                throw new IllegalArgumentException("ranges not supported by this parser");
            }

            @Override
            public VersionConstraint parseVersionConstraint(String constraint) {
                throw new IllegalArgumentException("constraint not supported by this parser");
            }

            @Override
            public boolean isSnapshot(String version) {
                requireNonNull(version, "version");
                return version.endsWith("SNAPSHOT");
            }
        };
    }

    /**
     * Creates a new model builder instance.
     *
     * @return The new model builder instance, never {@code null}.
     */
    public DefaultModelBuilder newInstance() {
        return new DefaultModelBuilder(
                modelProcessor != null ? modelProcessor : newModelProcessor(),
                modelValidator != null ? modelValidator : newModelValidator(),
                modelNormalizer != null ? modelNormalizer : newModelNormalizer(),
                modelInterpolator != null ? modelInterpolator : newModelInterpolator(),
                modelPathTranslator != null ? modelPathTranslator : newModelPathTranslator(),
                modelUrlNormalizer != null ? modelUrlNormalizer : newModelUrlNormalizer(),
                superPomProvider != null ? superPomProvider : newSuperPomProvider(),
                inheritanceAssembler != null ? inheritanceAssembler : newInheritanceAssembler(),
                profileSelector != null ? profileSelector : newProfileSelector(),
                profileInjector != null ? profileInjector : newProfileInjector(),
                pluginManagementInjector != null ? pluginManagementInjector : newPluginManagementInjector(),
                dependencyManagementInjector != null ? dependencyManagementInjector : newDependencyManagementInjector(),
                dependencyManagementImporter != null ? dependencyManagementImporter : newDependencyManagementImporter(),
                lifecycleBindingsInjector != null ? lifecycleBindingsInjector : newLifecycleBindingsInjector(),
                pluginConfigurationExpander != null ? pluginConfigurationExpander : newPluginConfigurationExpander(),
                reportConfigurationExpander != null ? reportConfigurationExpander : newReportConfigurationExpander(),
                profileActivationFilePathInterpolator != null
                        ? profileActivationFilePathInterpolator
                        : newProfileActivationFilePathInterpolator(),
                versionProcessor != null ? versionProcessor : newModelVersionPropertiesProcessor(),
                transformer != null ? transformer : newModelSourceTransformer(),
                versionParser != null ? versionParser : newModelVersionParser());
    }

    private static class StubLifecycleBindingsInjector implements LifecycleBindingsInjector {

        @Override
        public void injectLifecycleBindings(
                Model model, ModelBuildingRequest request, ModelProblemCollector problems) {}
    }
}
