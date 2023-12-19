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
package org.apache.maven.internal.transformation.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.composition.DependencyManagementImporter;
import org.apache.maven.model.inheritance.InheritanceAssembler;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.management.DependencyManagementInjector;
import org.apache.maven.model.management.PluginManagementInjector;
import org.apache.maven.model.normalization.ModelNormalizer;
import org.apache.maven.model.path.ModelPathTranslator;
import org.apache.maven.model.path.ModelUrlNormalizer;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;
import org.apache.maven.model.plugin.PluginConfigurationExpander;
import org.apache.maven.model.plugin.ReportConfigurationExpander;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileInjector;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.model.validation.ModelValidator;
import org.apache.maven.model.version.ModelVersionParser;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.impl.RemoteRepositoryManager;

@Named
class DefaultConsumerPomBuilder implements ConsumerPomBuilder {
    private static final String BOM_PACKAGING = "bom";

    public static final String POM_PACKAGING = "pom";

    @Inject
    private ModelCacheFactory modelCacheFactory;

    @Inject
    private ProfileInjector profileInjector;

    @Inject
    private InheritanceAssembler inheritanceAssembler;

    @Inject
    private DependencyManagementImporter dependencyManagementImporter;

    @Inject
    private DependencyManagementInjector dependencyManagementInjector;

    @Inject
    private LifecycleBindingsInjector lifecycleBindingsInjector;

    @Inject
    private ModelInterpolator modelInterpolator;

    @Inject
    private ModelNormalizer modelNormalizer;

    @Inject
    private ModelPathTranslator modelPathTranslator;

    @Inject
    private ModelProcessor modelProcessor;

    @Inject
    private ModelUrlNormalizer modelUrlNormalizer;

    @Inject
    private ModelValidator modelValidator;

    @Inject
    private PluginConfigurationExpander pluginConfigurationExpander;

    @Inject
    private PluginManagementInjector pluginManagementInjector;

    @Inject
    private ReportConfigurationExpander reportConfigurationExpander;

    @Inject
    private SuperPomProvider superPomProvider;

    @Inject
    private ModelVersionParser versionParser;

    // To break circular dependency
    @Inject
    private Provider<RepositorySystem> repositorySystem;

    @Inject
    private RemoteRepositoryManager remoteRepositoryManager;

    @Override
    public Model build(RepositorySystemSession session, MavenProject project, Path src) throws ModelBuildingException {
        Model model = project.getModel().getDelegate();
        String packaging = model.getPackaging();
        String originalPackaging = project.getOriginalModel().getPackaging();
        if (POM_PACKAGING.equals(packaging) && !BOM_PACKAGING.equals(originalPackaging)) {
            return buildPom(session, project, src);
        } else {
            return buildNonPom(session, project, src);
        }
    }

    protected Model buildPom(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException {
        ModelBuildingResult result = buildModel(session, project, src);
        Model model = result.getRawModel().getDelegate();
        return transform(model, project);
    }

    protected Model buildNonPom(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException {
        ModelBuildingResult result = buildModel(session, project, src);
        Model model = result.getEffectiveModel().getDelegate();
        return transform(model, project);
    }

    private ModelBuildingResult buildModel(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException {
        ProfileSelector customSelector = new DefaultProfileSelector() {
            @Override
            public List<Profile> getActiveProfilesV4(
                    Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {
                return new ArrayList<>();
            }
        };
        DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory()
                .setProfileSelector(customSelector)
                // apply currently active ModelProcessor etc. to support extensions like jgitver
                .setProfileInjector(profileInjector)
                .setInheritanceAssembler(inheritanceAssembler)
                .setDependencyManagementImporter(dependencyManagementImporter)
                .setDependencyManagementInjector(dependencyManagementInjector)
                .setLifecycleBindingsInjector(lifecycleBindingsInjector)
                .setModelInterpolator(modelInterpolator)
                .setModelNormalizer(modelNormalizer)
                .setModelPathTranslator(modelPathTranslator)
                .setModelProcessor(modelProcessor)
                .setModelUrlNormalizer(modelUrlNormalizer)
                .setModelValidator(modelValidator)
                .setPluginConfigurationExpander(pluginConfigurationExpander)
                .setPluginManagementInjector(pluginManagementInjector)
                .setReportConfigurationExpander(reportConfigurationExpander)
                .setSuperPomProvider(superPomProvider)
                .setModelVersionParser(versionParser)
                .newInstance();
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        try {
            request.setRootDirectory(project.getRootDirectory());
        } catch (IllegalStateException e) {
            // ignore if we don't have a root directory
        }
        request.setPomFile(src.toFile());
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setLocationTracking(false);
        request.setModelResolver(new ProjectModelResolver(
                session,
                new RequestTrace(null),
                repositorySystem.get(),
                remoteRepositoryManager,
                project.getRemoteProjectRepositories(),
                ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT,
                null));
        request.setTransformerContextBuilder(modelBuilder.newTransformerContextBuilder());
        request.setSystemProperties(toProperties(session.getSystemProperties()));
        request.setUserProperties(toProperties(session.getUserProperties()));
        request.setModelCache(modelCacheFactory.createCache(session));
        return modelBuilder.build(request);
    }

    private Properties toProperties(Map<String, String> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }

    static Model transform(Model model, MavenProject project) {
        String packaging = model.getPackaging();
        if (POM_PACKAGING.equals(packaging)) {
            // raw to consumer transform
            model = model.withRoot(false).withModules(null);
            if (model.getParent() != null) {
                model = model.withParent(model.getParent().withRelativePath(null));
            }

            if (!model.isPreserveModelVersion()) {
                model = model.withPreserveModelVersion(false);
                String modelVersion = new MavenModelVersion().getModelVersion(model);
                model = model.withModelVersion(modelVersion);
            }
        } else if (BOM_PACKAGING.equals(packaging)) {
            DependencyManagement dependencyManagement =
                    project.getOriginalModel().getDependencyManagement().getDelegate();
            List<Dependency> dependencies = new ArrayList<>();
            String version = model.getVersion();

            dependencyManagement
                    .getDependencies()
                    .forEach((dependency) -> dependencies.add(dependency.withVersion(version)));
            Model.Builder builder = prune(
                    Model.newBuilder(model, true)
                            .preserveModelVersion(false)
                            .root(false)
                            .parent(null)
                            .dependencyManagement(dependencyManagement.withDependencies(dependencies))
                            .build(null),
                    model);
            builder.packaging(POM_PACKAGING);
            builder.profiles(model.getProfiles().stream()
                    .map(p -> prune(Profile.newBuilder(p, true), p).build())
                    .collect(Collectors.toList()));

            model = builder.build();
            String modelVersion = new MavenModelVersion().getModelVersion(model);
            model = model.withModelVersion(modelVersion);
        } else {
            Model.Builder builder = prune(
                    Model.newBuilder(model, true)
                            .preserveModelVersion(false)
                            .root(false)
                            .parent(null)
                            .build(null),
                    model);
            builder.profiles(model.getProfiles().stream()
                    .map(p -> prune(Profile.newBuilder(p, true), p).build())
                    .collect(Collectors.toList()));
            model = builder.build();
            String modelVersion = new MavenModelVersion().getModelVersion(model);
            model = model.withModelVersion(modelVersion);
        }
        return model;
    }

    private static <T extends ModelBase.Builder> T prune(T builder, ModelBase model) {
        builder.properties(null).reporting(null);
        if (model.getDistributionManagement() != null
                && model.getDistributionManagement().getRelocation() != null) {
            // keep relocation only
            builder.distributionManagement(DistributionManagement.newBuilder()
                    .relocation(model.getDistributionManagement().getRelocation())
                    .build());
        }
        // only keep repositories other than 'central'
        builder.pluginRepositories(pruneRepositories(model.getPluginRepositories()));
        builder.repositories(pruneRepositories(model.getRepositories()));
        return builder;
    }

    private static List<Repository> pruneRepositories(List<Repository> repositories) {
        return repositories.stream()
                .filter(r -> !org.apache.maven.api.Repository.CENTRAL_ID.equals(r.getId()))
                .collect(Collectors.toList());
    }
}
