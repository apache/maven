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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.services.MavenException;
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

/**
 * Builds consumer POMs from project models, transforming them into a format suitable for downstream consumers.
 * <p>
 * A consumer POM is a simplified version of a project's POM that is published for consumption by other projects.
 * It removes build-specific information and internal details while preserving essential information like
 * dependencies, repositories, and distribution management.
 * <p>
 * This builder applies two orthogonal transformations:
 * <ul>
 *   <li><b>Dependency Flattening</b>: When enabled via {@code maven.consumer.pom.flatten=true}, dependency management
 *       is flattened into direct dependencies for non-POM projects, and mixins are removed.</li>
 *   <li><b>Model Version Handling</b>: When {@code preserve.model.version=true} is set, the consumer POM
 *       maintains the original model version (4.2.0) instead of downgrading to 4.0.0 for Maven 3 compatibility.
 *       This allows modern features like mixins to be preserved in the consumer POM.</li>
 * </ul>
 * <p>
 * <b>Mixin Handling</b>: Mixins are only supported in model version 4.2.0 or later. If a POM contains mixins:
 * <ul>
 *   <li>Setting {@code preserve.model.version=true} preserves them in the consumer POM with model version 4.2.0</li>
 *   <li>Setting {@code maven.consumer.pom.flatten=true} removes them during transformation</li>
 *   <li>Otherwise, an exception is thrown requiring one of the above options or manual mixin removal</li>
 * </ul>
 * <p>
 * <b>Dependency Filtering</b>: For non-POM projects with dependency management, the builder:
 * <ul>
 *   <li>Filters dependencies to include only those with transitive scopes (compile/runtime)</li>
 *   <li>Applies managed dependency metadata (version, scope, optional flag, exclusions) to direct dependencies</li>
 *   <li>Removes managed dependencies that are not used by direct dependencies</li>
 *   <li>Retains only managed dependencies that appear in the resolved dependency tree</li>
 * </ul>
 * <p>
 * <b>Repository and Profile Pruning</b>: The consumer POM removal strategy:
 * <ul>
 *   <li>Removes the central repository (only non-central repositories are kept)</li>
 *   <li>Removes build, mailing lists, issue management, and other build-specific information</li>
 *   <li>Removes profiles that have no activation, build, dependencies, or properties</li>
 *   <li>Preserves relocation information in distribution management</li>
 * </ul>
 */
@Named
class DefaultConsumerPomBuilder implements PomBuilder {
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
    public Model build(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        Model model = project.getModel().getDelegate();
        boolean flattenEnabled = Features.consumerPomFlatten(session.getConfigProperties());
        String packaging = model.getPackaging();
        String originalPackaging = project.getOriginalModel().getPackaging();

        // Check if this is a BOM (original packaging is "bom")
        boolean isBom = BOM_PACKAGING.equals(originalPackaging);

        // Check if mixins are present without flattening enabled
        if (!model.getMixins().isEmpty() && !flattenEnabled && !model.isPreserveModelVersion()) {
            throw new MavenException("The consumer POM for "
                    + project.getId()
                    + " cannot be created because the POM contains mixins. "
                    + "Mixins are not supported in the default consumer POM format. "
                    + "You have the following options to resolve this:" + System.lineSeparator()
                    + "  1. Preserve the model version by setting 'preserve.model.version=true' to generate a consumer POM with <modelVersion>4.2.0</modelVersion>, which supports mixins"
                    + System.lineSeparator()
                    + "  2. Enable flattening by setting the property 'maven.consumer.pom.flatten=true' to remove mixins during transformation"
                    + System.lineSeparator()
                    + "  3. Remove the mixins from your POM");
        }

        // Check if consumer POM flattening is disabled
        if (!flattenEnabled) {
            // When flattening is disabled, treat non-POM projects like parent POMs
            // Apply only basic transformations without flattening dependency management
            // However, BOMs still need special handling to transform packaging from "bom" to "pom"
            if (isBom) {
                return buildBomWithoutFlatten(session, project, src);
            } else {
                return buildPom(session, project, src);
            }
        }
        // Default behavior: flatten the consumer POM
        if (POM_PACKAGING.equals(packaging)) {
            if (isBom) {
                return buildBom(session, project, src);
            } else {
                return buildPom(session, project, src);
            }
        } else {
            return buildNonPom(session, project, src);
        }
    }

    protected Model buildPom(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        ModelBuilderResult result = buildModel(session, src);
        Model model = result.getRawModel();
        return transformPom(model, project);
    }

    protected Model buildBomWithoutFlatten(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        ModelBuilderResult result = buildModel(session, src);
        Model model = result.getRawModel();
        // For BOMs without flattening, we just need to transform the packaging from "bom" to "pom"
        // but keep everything else from the raw model (including unresolved versions)
        return transformBom(model, project);
    }

    protected Model buildBom(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        ModelBuilderResult result = buildModel(session, src);
        Model model = result.getEffectiveModel();
        return transformBom(model, project);
    }

    protected Model buildNonPom(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        Model model = buildEffectiveModel(session, src);
        return transformNonPom(model, project);
    }

    private Model buildEffectiveModel(RepositorySystemSession session, ModelSource src) throws ModelBuilderException {
        InternalSession iSession = InternalSession.from(session);
        ModelBuilderResult result = buildModel(session, src);
        Model model = result.getEffectiveModel();

        if (model.getDependencyManagement() != null
                && !model.getDependencyManagement().getDependencies().isEmpty()) {
            ArtifactCoordinates artifact = iSession.createArtifactCoordinates(
                    model.getGroupId(), model.getArtifactId(), model.getVersion(), null);
            Node node = iSession.collectDependencies(
                    iSession.createDependencyCoordinates(artifact), PathScope.MAIN_RUNTIME);

            Map<String, Node> nodes = node.stream()
                    .collect(Collectors.toMap(n -> getDependencyKey(n.getDependency()), Function.identity()));
            Map<String, Dependency> directDependencies = model.getDependencies().stream()
                    .filter(dependency -> !"import".equals(dependency.getScope()))
                    .collect(Collectors.toMap(
                            DefaultConsumerPomBuilder::getDependencyKey,
                            Function.identity(),
                            this::merge,
                            LinkedHashMap::new));
            Map<String, Dependency> managedDependencies = model.getDependencyManagement().getDependencies().stream()
                    .filter(dependency ->
                            nodes.containsKey(getDependencyKey(dependency)) && !"import".equals(dependency.getScope()))
                    .collect(Collectors.toMap(
                            DefaultConsumerPomBuilder::getDependencyKey,
                            Function.identity(),
                            this::merge,
                            LinkedHashMap::new));

            // for each managed dep in the model:
            // * if there is no corresponding node in the tree, discard the managed dep
            // * if there's a direct dependency, apply the managed dependency to it and discard the managed dep
            // * else keep the managed dep
            managedDependencies.keySet().retainAll(nodes.keySet());

            directDependencies.replaceAll((key, dependency) -> {
                var managedDependency = managedDependencies.get(key);
                if (managedDependency != null) {
                    if (dependency.getVersion() == null && managedDependency.getVersion() != null) {
                        dependency = dependency.withVersion(managedDependency.getVersion());
                    }
                    if (dependency.getScope() == null && managedDependency.getScope() != null) {
                        dependency = dependency.withScope(managedDependency.getScope());
                    }
                    if (dependency.getOptional() == null && managedDependency.getOptional() != null) {
                        dependency = dependency.withOptional(managedDependency.getOptional());
                    }
                    if (dependency.getExclusions().isEmpty()
                            && !managedDependency.getExclusions().isEmpty()) {
                        dependency = dependency.withExclusions(managedDependency.getExclusions());
                    }
                }
                return dependency;
            });
            // Only keep transitive scopes (null/empty => COMPILE)
            directDependencies.values().removeIf(DefaultConsumerPomBuilder::hasDependencyScope);
            managedDependencies.keySet().removeAll(directDependencies.keySet());

            model = model.withDependencyManagement(
                            managedDependencies.isEmpty()
                                    ? null
                                    : model.getDependencyManagement().withDependencies(managedDependencies.values()))
                    .withDependencies(directDependencies.isEmpty() ? null : directDependencies.values());
        } else {
            // Even without dependencyManagement, filter direct dependencies to compile/runtime only
            Map<String, Dependency> directDependencies = model.getDependencies().stream()
                    .filter(dependency -> !"import".equals(dependency.getScope()))
                    .collect(Collectors.toMap(
                            DefaultConsumerPomBuilder::getDependencyKey,
                            Function.identity(),
                            this::merge,
                            LinkedHashMap::new));
            // Only keep transitive scopes
            directDependencies.values().removeIf(DefaultConsumerPomBuilder::hasDependencyScope);
            model = model.withDependencies(directDependencies.isEmpty() ? null : directDependencies.values());
        }

        return model;
    }

    private static boolean hasDependencyScope(Dependency dependency) {
        String scopeId = dependency.getScope();
        DependencyScope scope;
        if (scopeId == null || scopeId.isEmpty()) {
            scope = DependencyScope.COMPILE;
        } else {
            scope = DependencyScope.forId(scopeId);
        }
        return scope == null || !scope.isTransitive();
    }

    private Dependency merge(Dependency dep1, Dependency dep2) {
        throw new IllegalArgumentException("Duplicate dependency: " + getDependencyKey(dep1));
    }

    private static String getDependencyKey(org.apache.maven.api.Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                + dependency.getType().id() + ":" + dependency.getClassifier();
    }

    private static String getDependencyKey(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                + (dependency.getType() != null ? dependency.getType() : "jar") + ":"
                + (dependency.getClassifier() != null ? dependency.getClassifier() : "");
    }

    private ModelBuilderResult buildModel(RepositorySystemSession session, ModelSource src)
            throws ModelBuilderException {
        InternalSession iSession = InternalSession.from(session);
        ModelBuilderRequest.ModelBuilderRequestBuilder request = ModelBuilderRequest.builder();
        request.requestType(ModelBuilderRequest.RequestType.BUILD_CONSUMER);
        request.session(iSession);
        request.source(src);
        request.locationTracking(false);
        request.systemProperties(session.getSystemProperties());
        request.userProperties(session.getUserProperties());
        request.lifecycleBindingsInjector(lifecycleBindingsInjector::injectLifecycleBindings);
        ModelBuilder.ModelBuilderSession mbSession =
                iSession.getData().get(SessionData.key(ModelBuilder.ModelBuilderSession.class));
        return mbSession.build(request.build());
    }

    static Model transformNonPom(Model model, MavenProject project) {
        boolean preserveModelVersion = model.isPreserveModelVersion();

        Model.Builder builder = prune(
                        Model.newBuilder(model, true)
                                .preserveModelVersion(false)
                                .root(false)
                                .parent(null)
                                .mixins(null)
                                .build(null),
                        model)
                .mailingLists(null)
                .issueManagement(null)
                .scm(
                        model.getScm() != null
                                ? Scm.newBuilder(model.getScm(), true)
                                        .childScmConnectionInheritAppendPath(null)
                                        .childScmUrlInheritAppendPath(null)
                                        .childScmDeveloperConnectionInheritAppendPath(null)
                                        .build()
                                : null);
        builder.profiles(prune(model.getProfiles()));

        model = builder.build();
        String modelVersion = new MavenModelVersion().getModelVersion(model);
        if (!ModelBuilder.MODEL_VERSION_4_0_0.equals(modelVersion) && !preserveModelVersion) {
            warnNotDowngraded(project);
        }
        model = model.withModelVersion(modelVersion);

        return model;
    }

    static Model transformBom(Model model, MavenProject project) {
        boolean preserveModelVersion = model.isPreserveModelVersion();

        Model.Builder builder = prune(
                Model.newBuilder(model, true)
                        .preserveModelVersion(false)
                        .root(false)
                        .parent(null)
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
        return model;
    }

    static Model transformPom(Model model, MavenProject project) {
        boolean preserveModelVersion = model.isPreserveModelVersion();

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
        builder.repositories(pruneRepositories(model.getRepositories()));
        builder.pluginRepositories(null);
        return builder;
    }

    private static List<Repository> pruneRepositories(List<Repository> repositories) {
        return repositories.stream()
                .filter(r -> !org.apache.maven.api.Repository.CENTRAL_ID.equals(r.getId()))
                .collect(Collectors.toList());
    }
}
