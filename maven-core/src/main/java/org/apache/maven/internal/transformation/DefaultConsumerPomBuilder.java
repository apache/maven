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
package org.apache.maven.internal.transformation;

import javax.inject.Inject;
import javax.inject.Named;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.impl.RemoteRepositoryManager;

@Named
public class DefaultConsumerPomBuilder implements ConsumerPomArtifactTransformer.ConsumerPomBuilder {

    private static final String BOM_PACKAGING = "bom";

    public static final String POM_PACKAGING = "pom";

    @Inject
    PlexusContainer container;

    @Inject
    ModelCacheFactory modelCacheFactory;

    public Model build(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException, ComponentLookupException {
        Model model = project.getModel().getDelegate();
        String packaging = model.getPackaging();
        if (POM_PACKAGING.equals(packaging)) {
            return buildPom(session, project, src);
        } else {
            return buildNonPom(session, project, src);
        }
    }

    protected Model buildPom(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException, ComponentLookupException {
        ModelBuildingResult result = buildModel(session, project, src);
        Model model = result.getRawModel().getDelegate();
        return transform(model);
    }

    protected Model buildNonPom(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException, ComponentLookupException {
        ModelBuildingResult result = buildModel(session, project, src);
        Model model = result.getEffectiveModel().getDelegate();
        return transform(model);
    }

    private ModelBuildingResult buildModel(RepositorySystemSession session, MavenProject project, Path src)
            throws ComponentLookupException, ModelBuildingException {
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
                .setProfileInjector(lookup(ProfileInjector.class))
                .setInheritanceAssembler(lookup(InheritanceAssembler.class))
                .setDependencyManagementImporter(lookup(DependencyManagementImporter.class))
                .setDependencyManagementInjector(lookup(DependencyManagementInjector.class))
                .setLifecycleBindingsInjector(lookup(LifecycleBindingsInjector.class))
                .setModelInterpolator(lookup(ModelInterpolator.class))
                .setModelNormalizer(lookup(ModelNormalizer.class))
                .setModelPathTranslator(lookup(ModelPathTranslator.class))
                .setModelProcessor(lookup(ModelProcessor.class))
                .setModelUrlNormalizer(lookup(ModelUrlNormalizer.class))
                .setModelValidator(lookup(ModelValidator.class))
                .setPluginConfigurationExpander(lookup(PluginConfigurationExpander.class))
                .setPluginManagementInjector(lookup(PluginManagementInjector.class))
                .setReportConfigurationExpander(lookup(ReportConfigurationExpander.class))
                .setSuperPomProvider(lookup(SuperPomProvider.class))
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
                lookup(RepositorySystem.class),
                lookup(RemoteRepositoryManager.class),
                project.getRemoteProjectRepositories(),
                ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT,
                null));
        request.setTransformerContextBuilder(modelBuilder.newTransformerContextBuilder());
        Properties props = new Properties();
        props.putAll(session.getSystemProperties());
        request.setSystemProperties(props);
        props = new Properties();
        props.putAll(session.getUserProperties());
        request.setUserProperties(props);
        request.setModelCache(modelCacheFactory.createCache(session));
        ModelBuildingResult result = modelBuilder.build(request);
        return result;
    }

    private <T> T lookup(Class<T> clazz) throws ComponentLookupException {
        return container.lookup(clazz);
    }

    static Model transform(Model model) {
        String version;
        String packaging = model.getPackaging();
        if (POM_PACKAGING.equals(packaging)) {
            // raw to consumer transform
            model = model.withRoot(false).withModules(null);
            if (model.getParent() != null) {
                model = model.withParent(model.getParent().withRelativePath(null));
            }

            if (!model.isPreserveModelVersion()) {
                model = model.withPreserveModelVersion(false);
                version = new MavenModelVersion().getModelVersion(model);
                model = model.withModelVersion(version);
            } else {
                version = model.getModelVersion();
            }
        } else {
            Model.Builder builder = prune(
                    Model.newBuilder(model, true)
                            .preserveModelVersion(false)
                            .root(false)
                            .parent(null)
                            .build(null),
                    model);
            boolean isBom = BOM_PACKAGING.equals(packaging);
            if (isBom) {
                builder.packaging(POM_PACKAGING);
            }
            builder.profiles(model.getProfiles().stream()
                    .map(p -> prune(Profile.newBuilder(p, true), p).build())
                    .collect(Collectors.toList()));
            model = builder.build();
            version = new MavenModelVersion().getModelVersion(model);
            model = model.withModelVersion(version);
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
        // only keep repositories others than 'central'
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
