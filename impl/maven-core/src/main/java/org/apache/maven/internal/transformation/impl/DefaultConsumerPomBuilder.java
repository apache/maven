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
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.model.LifecycleBindingsInjector;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
class DefaultConsumerPomBuilder implements ConsumerPomBuilder {
    private static final String BOM_PACKAGING = "bom";

    public static final String POM_PACKAGING = "pom";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumerPomBuilder.class);

    private final LifecycleBindingsInjector lifecycleBindingsInjector;

    @Inject
    @SuppressWarnings("checkstyle:ParameterNumber")
    DefaultConsumerPomBuilder(LifecycleBindingsInjector lifecycleBindingsInjector) {
        this.lifecycleBindingsInjector = lifecycleBindingsInjector;
    }

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
        InternalSession iSession = InternalSession.from(session);
        ModelBuilderRequest.ModelBuilderRequestBuilder request = ModelBuilderRequest.builder();
        request.requestType(ModelBuilderRequest.RequestType.BUILD_CONSUMER);
        request.session(iSession);
        request.source(ModelSource.fromPath(src));
        request.locationTracking(false);
        request.systemProperties(session.getSystemProperties());
        request.userProperties(session.getUserProperties());
        request.lifecycleBindingsInjector(lifecycleBindingsInjector::injectLifecycleBindings);
        ModelBuilder.ModelBuilderSession mbSession =
                iSession.getData().get(SessionData.key(ModelBuilder.ModelBuilderSession.class));
        return mbSession.build(request.build());
    }

    static Model transform(Model model, MavenProject project) {
        String packaging = model.getPackaging();
        boolean preserveModelVersion = model.isPreserveModelVersion();
        if (POM_PACKAGING.equals(packaging)) {
            // raw to consumer transform
            model = model.withRoot(false).withModules(null).withSubprojects(null);
            if (model.getParent() != null) {
                model = model.withParent(model.getParent().withRelativePath(null));
            }

            if (!preserveModelVersion) {
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
            if (!ModelBuilder.MODEL_VERSION_4_0_0.equals(modelVersion) && !preserveModelVersion) {
                warnNotDowngraded(project);
            }
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
            if (!ModelBuilder.MODEL_VERSION_4_0_0.equals(modelVersion) && !preserveModelVersion) {
                warnNotDowngraded(project);
            }
            model = model.withModelVersion(modelVersion);
        }
        return model;
    }

    static void warnNotDowngraded(MavenProject project) {
        LOGGER.warn("The consumer POM for " + project.getId() + " cannot be downgraded to 4.0.0. "
                + "If you intent your build to be consumed with Maven 3 projects, you need to remove "
                + "the features that request a newer model version.  If you're fine with having the "
                + "consumer POM not consumable with Maven 3, add the `preserve.model.version='true'` "
                + "attribute on the <project> element of your POM.");
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
