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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.model.DependencyManagementImporter;
import org.apache.maven.api.services.model.DependencyManagementInjector;
import org.apache.maven.api.services.model.InheritanceAssembler;
import org.apache.maven.api.services.model.LifecycleBindingsInjector;
import org.apache.maven.api.services.model.ModelCacheFactory;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.ModelNormalizer;
import org.apache.maven.api.services.model.ModelPathTranslator;
import org.apache.maven.api.services.model.ModelProcessor;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelUrlNormalizer;
import org.apache.maven.api.services.model.ModelValidator;
import org.apache.maven.api.services.model.ModelVersionParser;
import org.apache.maven.api.services.model.PluginConfigurationExpander;
import org.apache.maven.api.services.model.PluginManagementInjector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileInjector;
import org.apache.maven.api.services.model.ProfileSelector;
import org.apache.maven.api.spi.ModelTransformer;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.internal.impl.model.DefaultModelBuilder;
import org.apache.maven.internal.impl.model.DefaultProfileSelector;
import org.apache.maven.internal.impl.model.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
class DefaultConsumerPomBuilder implements ConsumerPomBuilder {
    private static final String BOM_PACKAGING = "bom";

    public static final String POM_PACKAGING = "pom";

    private final ProfileInjector profileInjector;
    private final InheritanceAssembler inheritanceAssembler;
    private final DependencyManagementImporter dependencyManagementImporter;
    private final DependencyManagementInjector dependencyManagementInjector;
    private final LifecycleBindingsInjector lifecycleBindingsInjector;
    private final ModelInterpolator modelInterpolator;
    private final ModelNormalizer modelNormalizer;
    private final ModelPathTranslator modelPathTranslator;
    private final ModelProcessor modelProcessor;
    private final ModelUrlNormalizer modelUrlNormalizer;
    private final ModelValidator modelValidator;
    private final PluginConfigurationExpander pluginConfigurationExpander;
    private final PluginManagementInjector pluginManagementInjector;
    private final SuperPomProvider superPomProvider;
    private final ModelVersionParser versionParser;
    // To break circular dependency    private final Provider<RepositorySystem> repositorySystem;
    private final RemoteRepositoryManager remoteRepositoryManager;
    private final ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;
    private final List<ModelTransformer> transformers;
    private final ModelCacheFactory modelCacheFactory;
    private final ModelResolver modelResolver;

    @Inject
    @SuppressWarnings("checkstyle:ParameterNumber")
    DefaultConsumerPomBuilder(
            ProfileInjector profileInjector,
            InheritanceAssembler inheritanceAssembler,
            DependencyManagementImporter dependencyManagementImporter,
            DependencyManagementInjector dependencyManagementInjector,
            LifecycleBindingsInjector lifecycleBindingsInjector,
            ModelInterpolator modelInterpolator,
            ModelNormalizer modelNormalizer,
            ModelPathTranslator modelPathTranslator,
            ModelProcessor modelProcessor,
            ModelUrlNormalizer modelUrlNormalizer,
            ModelValidator modelValidator,
            PluginConfigurationExpander pluginConfigurationExpander,
            PluginManagementInjector pluginManagementInjector,
            SuperPomProvider superPomProvider,
            ModelVersionParser versionParser,
            RemoteRepositoryManager remoteRepositoryManager,
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator,
            List<ModelTransformer> transformers,
            ModelCacheFactory modelCacheFactory,
            ModelResolver modelResolver) {
        this.profileInjector = profileInjector;
        this.inheritanceAssembler = inheritanceAssembler;
        this.dependencyManagementImporter = dependencyManagementImporter;
        this.dependencyManagementInjector = dependencyManagementInjector;
        this.lifecycleBindingsInjector = lifecycleBindingsInjector;
        this.modelInterpolator = modelInterpolator;
        this.modelNormalizer = modelNormalizer;
        this.modelPathTranslator = modelPathTranslator;
        this.modelProcessor = modelProcessor;
        this.modelUrlNormalizer = modelUrlNormalizer;
        this.modelValidator = modelValidator;
        this.pluginConfigurationExpander = pluginConfigurationExpander;
        this.pluginManagementInjector = pluginManagementInjector;
        this.superPomProvider = superPomProvider;
        this.versionParser = versionParser;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        this.transformers = transformers;
        this.modelCacheFactory = modelCacheFactory;
        this.modelResolver = modelResolver;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Model build(RepositorySystemSession session, MavenProject project, Path src) throws ModelBuilderException {
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
            throws ModelBuilderException {
        ModelBuilderResult result = buildModel(session, project, src);
        Model model = result.getRawModel();
        return transform(model, project);
    }

    protected Model buildNonPom(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuilderException {
        ModelBuilderResult result = buildModel(session, project, src);
        Model model = result.getEffectiveModel();
        return transform(model, project);
    }

    private ModelBuilderResult buildModel(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuilderException {
        ProfileSelector customSelector = new DefaultProfileSelector() {
            @Override
            public List<Profile> getActiveProfiles(
                    Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {
                return new ArrayList<>();
            }
        };
        // TODO: the custom selector should be used as a flag on the request
        DefaultModelBuilder modelBuilder = new DefaultModelBuilder(
                modelProcessor,
                modelValidator,
                modelNormalizer,
                modelInterpolator,
                modelPathTranslator,
                modelUrlNormalizer,
                superPomProvider,
                inheritanceAssembler,
                customSelector,
                profileInjector,
                pluginManagementInjector,
                dependencyManagementInjector,
                dependencyManagementImporter,
                lifecycleBindingsInjector,
                pluginConfigurationExpander,
                profileActivationFilePathInterpolator,
                versionParser,
                transformers,
                modelCacheFactory,
                modelResolver);
        InternalSession iSession = InternalSession.from(session);
        ModelBuilderRequest.ModelBuilderRequestBuilder request = ModelBuilderRequest.builder();
        request.requestType(ModelBuilderRequest.RequestType.BUILD_POM);
        request.session(iSession);
        request.source(ModelSource.fromPath(src));
        request.locationTracking(false);
        request.systemProperties(session.getSystemProperties());
        request.userProperties(session.getUserProperties());
        ModelBuilder.ModelBuilderSession mbSession =
                iSession.getData().get(SessionData.key(ModelBuilder.ModelBuilderSession.class));
        if (mbSession == null) {
            mbSession = modelBuilder.newSession();
        }
        return mbSession.build(request.build());
    }

    static Model transform(Model model, MavenProject project) {
        String packaging = model.getPackaging();
        if (POM_PACKAGING.equals(packaging)) {
            // raw to consumer transform
            model = model.withRoot(false).withModules(null).withSubprojects(null);
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
            builder.profiles(prune(model.getProfiles()));

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
            builder.profiles(prune(model.getProfiles()));

            model = builder.build();
            String modelVersion = new MavenModelVersion().getModelVersion(model);
            model = model.withModelVersion(modelVersion);
        }
        return model;
    }

    private static List<Profile> prune(List<Profile> profiles) {
        return profiles.stream()
                .map(p -> {
                    Profile.Builder builder = Profile.newBuilder(p, true);
                    prune((ModelBase.Builder) builder, p);
                    return builder.build(null).build();
                })
                .filter(p -> !isEmpty(p))
                .collect(Collectors.toList());
    }

    private static boolean isEmpty(Profile profile) {
        return profile.getActivation() == null
                && profile.getBuild() == null
                && profile.getDependencies().isEmpty()
                && (profile.getDependencyManagement() == null
                        || profile.getDependencyManagement().getDependencies().isEmpty())
                && profile.getDistributionManagement() == null
                && profile.getModules().isEmpty()
                && profile.getSubprojects().isEmpty()
                && profile.getProperties().isEmpty()
                && profile.getRepositories().isEmpty()
                && profile.getPluginRepositories().isEmpty()
                && profile.getReporting() == null;
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
