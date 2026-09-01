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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.feature.Features;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.ModelBase;
import org.apache.maven.api.model.Parent;
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
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.SourceQueries;
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
            // BOMs always need the effective (interpolated) model because transformBom()
            // strips parent and properties — any ${...} references would become dangling.
            // The flatten flag has no semantic effect on BOMs (transformBom always produces
            // a self-contained POM), so we use the same buildBom() path regardless.
            if (isBom) {
                return buildBom(session, project, src);
            } else {
                Model result = buildPom(session, project, src);
                // Validate POM-packaged projects (parent POMs): if the consumer POM cannot be
                // downgraded to 4.0.0, Maven 3 / Gradle cannot resolve the parent.
                // Non-POM projects are consumed as dependencies where unknown elements are
                // ignored, so a higher model version is acceptable (only a warning is logged
                // by transformNonPom/transformPom).
                if (POM_PACKAGING.equals(packaging)
                        && !model.isPreserveModelVersion()
                        && !ModelBuilder.MODEL_VERSION_4_0_0.equals(result.getModelVersion())) {
                    throw new MavenException("""
                            The consumer POM for %s cannot be downgraded to model version 4.0.0 because it contains\
                             features that require a newer model version.\
                             Since consumer POM flattening is disabled, the parent reference is\
                             preserved, which requires consumers to resolve the parent POM.
                            You have the following options to resolve this:
                              1. Enable flattening by setting the property 'maven.consumer.pom.flatten=true'\
                             to inline parent content and produce a self-contained 4.0.0 consumer POM
                              2. Preserve the model version by setting 'preserve.model.version=true'\
                             on the <project> element (Maven 4 consumers only)
                              3. Remove the features that require a newer model version""".formatted(project.getId()));
                }
                return result;
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
        ModelBuilderResult result = buildModel(session, project, src);
        Model model = result.getRawModel();
        Model effectiveModel = result.getEffectiveModel();
        model = interpolatePomVersions(model, effectiveModel);
        return transformPom(model, project);
    }

    /**
     * Interpolates unresolved version references in the raw model's dependencies and
     * dependency management using the effective model's fully-resolved values.
     * <p>
     * The {@code buildPom()} path uses the raw model to preserve the parent reference
     * in the consumer POM, allowing downstream consumers to resolve inheritance. However,
     * properties contributed by Maven extensions (via {@code PropertyContributor} SPI) are
     * only available as user properties during the build session — they are not part of any
     * POM's {@code <properties>} section or parent chain. Without this interpolation step,
     * such property references (e.g. {@code ${nisse.jgit.dynamicVersion}}) would remain
     * unresolved in the installed/deployed consumer POM, making it unusable.
     *
     * @param rawModel the raw model (no inheritance, no interpolation)
     * @param effectiveModel the effective model (inheritance + full interpolation)
     * @return the raw model with version references resolved from the effective model
     * @see <a href="https://github.com/apache/maven/issues/12981">GH-12981</a>
     */
    static Model interpolatePomVersions(Model rawModel, Model effectiveModel) {
        // Build lookups from the effective model
        Map<String, Dependency> effectiveManagedDeps = new LinkedHashMap<>();
        if (effectiveModel.getDependencyManagement() != null) {
            for (Dependency dep : effectiveModel.getDependencyManagement().getDependencies()) {
                effectiveManagedDeps.put(getDependencyKey(dep), dep);
            }
        }
        Map<String, Dependency> effectiveDeps = new LinkedHashMap<>();
        for (Dependency dep : effectiveModel.getDependencies()) {
            effectiveDeps.put(getDependencyKey(dep), dep);
        }

        // Interpolate dependency management versions
        if (rawModel.getDependencyManagement() != null
                && !rawModel.getDependencyManagement().getDependencies().isEmpty()) {
            List<Dependency> interpolatedDeps = new ArrayList<>();
            boolean dmChanged = false;
            for (Dependency dep : rawModel.getDependencyManagement().getDependencies()) {
                if (dep.getVersion() != null && dep.getVersion().contains("${")) {
                    String key = getDependencyKey(dep);
                    Dependency effectiveDep = effectiveManagedDeps.get(key);
                    if (effectiveDep != null && !effectiveDep.getVersion().contains("${")) {
                        dep = dep.withVersion(effectiveDep.getVersion());
                        dmChanged = true;
                    }
                }
                interpolatedDeps.add(dep);
            }
            if (dmChanged) {
                rawModel = rawModel.withDependencyManagement(
                        rawModel.getDependencyManagement().withDependencies(interpolatedDeps));
            }
        }

        // Interpolate direct dependency versions
        if (!rawModel.getDependencies().isEmpty()) {
            List<Dependency> interpolatedDeps = new ArrayList<>();
            boolean depsChanged = false;
            for (Dependency dep : rawModel.getDependencies()) {
                if (dep.getVersion() != null && dep.getVersion().contains("${")) {
                    String key = getDependencyKey(dep);
                    Dependency effectiveDep = effectiveDeps.get(key);
                    if (effectiveDep != null && !effectiveDep.getVersion().contains("${")) {
                        dep = dep.withVersion(effectiveDep.getVersion());
                        depsChanged = true;
                    }
                }
                interpolatedDeps.add(dep);
            }
            if (depsChanged) {
                rawModel = rawModel.withDependencies(interpolatedDeps);
            }
        }

        return rawModel;
    }

    protected Model buildBom(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        ModelBuilderResult result = buildModel(session, project, src);
        Model rawModel = result.getRawModel();
        Model effectiveModel = result.getEffectiveModel();
        // The raw model has no inheritance — only entries declared in this POM.
        // The effective model has fully interpolated values but includes inherited entries.
        // Filter the effective model's dependency management to only include entries
        // explicitly declared in this BOM, using the effective model for resolved values.
        Model model = filterToOwnDependencyManagement(rawModel, effectiveModel, project);
        return transformBom(model, project);
    }

    /**
     * Filters the effective model's dependency management to include only entries
     * that were explicitly declared in this BOM's raw model, not inherited from the
     * parent chain. For non-import entries, the effective model's fully resolved entry
     * is used. For import-scoped entries (which are consumed/flattened in the effective
     * model), the raw entry is preserved as a BOM reference with its version interpolated
     * from the project's properties.
     *
     * @param rawModel the raw model (no inheritance, no interpolation)
     * @param effectiveModel the effective model (inheritance + interpolation)
     * @param project the Maven project (provides resolved properties)
     * @return the effective model with dependency management filtered to own entries
     */
    private Model filterToOwnDependencyManagement(Model rawModel, Model effectiveModel, MavenProject project) {
        if (rawModel.getDependencyManagement() == null
                || rawModel.getDependencyManagement().getDependencies().isEmpty()) {
            // Nothing declared in this BOM — strip all inherited entries
            return effectiveModel.withDependencyManagement(null);
        }

        List<Dependency> declaredDeps = rawModel.getDependencyManagement().getDependencies();

        // Build lookup from the effective model's resolved dependency management
        Map<String, Dependency> effectiveLookup = new LinkedHashMap<>();
        if (effectiveModel.getDependencyManagement() != null) {
            for (Dependency dep : effectiveModel.getDependencyManagement().getDependencies()) {
                effectiveLookup.put(getDependencyKey(dep), dep);
            }
        }

        // For each declared entry, resolve it against the effective model
        List<Dependency> resolvedDeps = new ArrayList<>();
        for (Dependency declared : declaredDeps) {
            if ("import".equals(declared.getScope())) {
                // BOM import entries are consumed (flattened) in the effective model,
                // so they won't be in effectiveLookup. Preserve the import reference
                // with its version resolved from project properties.
                String resolvedVersion = interpolateVersion(declared.getVersion(), project);
                resolvedDeps.add(declared.withVersion(resolvedVersion));
            } else {
                // Regular entry: use the effective model's fully resolved entry
                String key = getDependencyKey(declared);
                Dependency resolved = effectiveLookup.get(key);
                resolvedDeps.add(resolved != null ? resolved : declared);
            }
        }

        return effectiveModel.withDependencyManagement(
                effectiveModel.getDependencyManagement() != null
                        ? effectiveModel.getDependencyManagement().withDependencies(resolvedDeps)
                        : org.apache.maven.api.model.DependencyManagement.newBuilder()
                                .dependencies(resolvedDeps)
                                .build());
    }

    /**
     * Resolves property references ({@code ${...}}) in a version string using the
     * Maven project's fully-resolved properties. Handles model properties, inherited
     * properties, and CI-friendly properties ({@code ${revision}}, etc.).
     */
    private static String interpolateVersion(String version, MavenProject project) {
        if (version == null || !version.contains("${")) {
            return version;
        }
        String result = version;
        Properties props = project.getProperties();
        for (String name : props.stringPropertyNames()) {
            String placeholder = "${" + name + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, props.getProperty(name));
            }
        }
        // Handle built-in project-coordinate properties
        if (result.contains("${project.version}") && project.getVersion() != null) {
            result = result.replace("${project.version}", project.getVersion());
        }
        if (result.contains("${project.groupId}") && project.getGroupId() != null) {
            result = result.replace("${project.groupId}", project.getGroupId());
        }
        return result;
    }

    protected Model buildNonPom(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        Model model = buildEffectiveModel(session, project, src);
        return transformNonPom(model, project);
    }

    private Model buildEffectiveModel(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        InternalSession iSession = InternalSession.from(session);
        ModelBuilderResult result = buildModel(session, project, src);
        Model model = result.getEffectiveModel();
        boolean removeUnusedManagedDeps =
                Features.consumerPomRemoveUnusedManagedDependencies(session.getConfigProperties());

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
                    .filter(dependency -> !"import".equals(dependency.getScope())
                            && (!removeUnusedManagedDeps || nodes.containsKey(getDependencyKey(dependency))))
                    .collect(Collectors.toMap(
                            DefaultConsumerPomBuilder::getDependencyKey,
                            Function.identity(),
                            this::merge,
                            LinkedHashMap::new));

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

    private ModelBuilderResult buildModel(RepositorySystemSession session, MavenProject project, ModelSource src)
            throws ModelBuilderException {
        InternalSession iSession = InternalSession.from(session);
        ModelBuilderRequest.ModelBuilderRequestBuilder request = ModelBuilderRequest.builder();
        request.requestType(ModelBuilderRequest.RequestType.BUILD_CONSUMER);
        request.session(iSession);
        request.source(src);
        request.locationTracking(false);
        request.systemProperties(iSession.getSystemProperties());
        Map<String, String> userProperties = new LinkedHashMap<>();
        // BUILD_CONSUMER does not reactivate project profiles, so expose properties from profiles
        // that were already active when the project model was built.
        if (project != null && project.getActiveProfiles() != null) {
            for (org.apache.maven.model.Profile profile : project.getActiveProfiles()) {
                userProperties.putAll(profile.getDelegate().getProperties());
            }
        }
        userProperties.putAll(iSession.getUserProperties());
        request.userProperties(userProperties);
        request.lifecycleBindingsInjector(lifecycleBindingsInjector::injectLifecycleBindings);
        // Pass remote repositories so that the model builder can resolve BOM imports
        // from non-central repositories (e.g., repositories defined in settings.xml profiles).
        // Prefer project repositories, but fall back to session repositories if the project's
        // remote repository list is not populated (e.g., during install/deploy phases).
        if (project != null
                && project.getRemoteProjectRepositories() != null
                && !project.getRemoteProjectRepositories().isEmpty()) {
            request.repositories(project.getRemoteProjectRepositories().stream()
                    .map(iSession::getRemoteRepository)
                    .toList());
        } else {
            request.repositories(iSession.getRemoteRepositories());
        }
        // Pass profiles and active/inactive profile IDs from the execution request
        // so that settings.xml profiles are applied during consumer POM model building.
        if (iSession instanceof InternalMavenSession mavenSession) {
            MavenExecutionRequest executionRequest =
                    mavenSession.getMavenSession().getRequest();
            if (executionRequest.getProfiles() != null) {
                request.profiles(executionRequest.getProfiles().stream()
                        .map(org.apache.maven.model.Profile::getDelegate)
                        .toList());
            }
            request.activeProfileIds(executionRequest.getActiveProfiles());
            request.inactiveProfileIds(executionRequest.getInactiveProfiles());
        } else {
            LOGGER.debug(
                    "Session is not an InternalMavenSession ({}); settings.xml profiles will not be "
                            + "passed to the consumer POM model builder. BOM imports from repositories "
                            + "defined only in settings.xml profiles may fail to resolve.",
                    iSession.getClass().getName());
        }
        ModelBuilder.ModelBuilderSession mbSession =
                iSession.getData().get(SessionData.key(ModelBuilder.ModelBuilderSession.class));
        return mbSession.build(request.build());
    }

    static Model transformNonPom(Model model, MavenProject project) {
        boolean preserveModelVersion = model.isPreserveModelVersion();
        String packaging = model.getPackaging();

        // Inline packaging-activated profiles into the model
        model = inlinePackagingActivatedProfiles(model, packaging);

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
        model = model.withRoot(false)
                .withModules(null)
                .withSubprojects(null)
                .withProfiles(stripExecutableConditions(model.getProfiles()));
        Parent parent = model.getParent();
        if (parent != null) {
            model = model.withParent(parent.withRelativePath(null));
        }
        var projectSources = project.getBuild().getDelegate().getSources();
        if (SourceQueries.usesModuleSourceHierarchy(projectSources)) {
            // Dependencies are dispatched by maven-jar-plugin in the POM generated for each module.
            model = model.withDependencies(null).withPackaging(POM_PACKAGING);
        }
        if (!preserveModelVersion) {
            /*
             * If the <build> contains <source> elements, it is not compatible with the Maven 4.0.0 model.
             * Remove the full <build> element instead of removing only the <sources> element, because the
             * build without sources does not mean much. Reminder: this removal can be disabled by setting
             * the `preserveModelVersion` XML attribute or `preserve.model.version` property to true.
             */
            if (SourceQueries.hasEnabledSources(projectSources)) {
                model = model.withBuild(null);
            }
            model = model.withPreserveModelVersion(false);
            String modelVersion = new MavenModelVersion().getModelVersion(model);
            model = model.withModelVersion(modelVersion);
        }
        return model;
    }

    private static void warnNotDowngraded(MavenProject project) {
        LOGGER.warn("The consumer POM for " + project.getId() + " cannot be downgraded to 4.0.0. "
                + "If you intent your build to be consumed with Maven 3 projects, you need to remove "
                + "the features that request a newer model version.  If you're fine with having the "
                + "consumer POM not consumable with Maven 3, add the `preserve.model.version='true'` "
                + "attribute on the <project> element of your POM.");
    }

    /**
     * Inlines packaging-activated profiles into the model.
     * <p>
     * When a profile is activated by packaging and the packaging matches the project's packaging,
     * the profile's content (dependencies, dependency management, repositories) is merged into
     * the main model and the profile is removed. This ensures consistent behavior across all
     * tools consuming the POM, since packaging activation is a 4.1.0+ feature not available
     * in Maven 3 or other tools like Gradle.
     * <p>
     * If the profile has other activation conditions besides packaging, only the packaging
     * part is stripped from the activation; the profile's content is <b>not</b> inlined to
     * preserve AND semantics (the content remains gated by the remaining conditions).
     * <p>
     * Profiles with a non-matching packaging activation are dropped entirely, since they
     * can never activate for this artifact's fixed packaging and their presence would block
     * model version downgrade to 4.0.0.
     * <p>
     * Non-transitive scope dependencies (test, provided, system) from inlined profiles are
     * filtered out to prevent leakage into the consumer POM.
     *
     * @param model the model to process
     * @param packaging the project's packaging type
     * @return the model with packaging-activated profiles inlined
     */
    static Model inlinePackagingActivatedProfiles(Model model, String packaging) {
        List<Profile> remainingProfiles = new ArrayList<>();
        List<Dependency> additionalDeps = new ArrayList<>();
        List<Dependency> additionalManagedDeps = new ArrayList<>();
        List<Repository> additionalRepos = new ArrayList<>();

        for (Profile profile : model.getProfiles()) {
            Activation activation = profile.getActivation();
            if (activation != null && activation.getPackaging() != null) {
                if (Objects.equals(activation.getPackaging(), packaging)) {
                    Activation strippedActivation = stripPackagingActivation(activation);
                    if (strippedActivation != null) {
                        // Keep the profile but remove the packaging activation part
                        // Do not inline its contents since it has other activation conditions
                        remainingProfiles.add(profile.withActivation(strippedActivation));
                    } else {
                        // Packaging is the ONLY condition.
                        // Inline profile content into the model
                        additionalDeps.addAll(profile.getDependencies());
                        if (profile.getDependencyManagement() != null) {
                            additionalManagedDeps.addAll(
                                    profile.getDependencyManagement().getDependencies());
                        }
                        additionalRepos.addAll(profile.getRepositories());
                    }
                } else {
                    // Packaging does not match — drop the profile entirely
                }
            } else {
                // No packaging activation — keep the profile as-is
                remainingProfiles.add(profile);
            }
        }

        // Merge additional dependencies into the model, deduplicating by key
        if (!additionalDeps.isEmpty()) {
            additionalDeps.removeIf(DefaultConsumerPomBuilder::hasDependencyScope);
            Map<String, Dependency> mergedDeps = new LinkedHashMap<>();
            for (Dependency dep : model.getDependencies()) {
                mergedDeps.put(getDependencyKey(dep), dep);
            }
            for (Dependency dep : additionalDeps) {
                mergedDeps.putIfAbsent(getDependencyKey(dep), dep);
            }
            model = model.withDependencies(mergedDeps.values());
        }

        // Merge additional managed dependencies into the model, deduplicating by key
        if (!additionalManagedDeps.isEmpty()) {
            // Filter out import-scoped entries — they are BOM references that get
            // flattened during resolution and must not reappear in the consumer POM
            additionalManagedDeps.removeIf(dep -> "import".equals(dep.getScope()));
            DependencyManagement dm = model.getDependencyManagement();
            Map<String, Dependency> mergedManagedDeps = new LinkedHashMap<>();
            if (dm != null) {
                for (Dependency dep : dm.getDependencies()) {
                    mergedManagedDeps.put(getDependencyKey(dep), dep);
                }
            }
            for (Dependency dep : additionalManagedDeps) {
                mergedManagedDeps.putIfAbsent(getDependencyKey(dep), dep);
            }
            model = model.withDependencyManagement((dm != null ? dm : DependencyManagement.newInstance())
                    .withDependencies(mergedManagedDeps.values()));
        }

        // Merge additional repositories into the model, deduplicating by id
        if (!additionalRepos.isEmpty()) {
            Map<String, Repository> mergedRepos = new LinkedHashMap<>();
            for (Repository repo : model.getRepositories()) {
                mergedRepos.put(repo.getId(), repo);
            }
            for (Repository repo : additionalRepos) {
                mergedRepos.putIfAbsent(repo.getId(), repo);
            }
            model = model.withRepositories(mergedRepos.values());
        }

        return model.withProfiles(remainingProfiles);
    }

    /**
     * Strips the packaging activation from an activation, returning the remaining activation
     * or {@code null} if packaging was the only activation condition.
     */
    private static Activation stripPackagingActivation(Activation activation) {
        Activation stripped =
                Activation.newBuilder(activation, true).packaging(null).build();
        // Check if the remaining activation has any other conditions
        if (isActivationEmpty(stripped)) {
            return null;
        }
        return stripped;
    }

    /**
     * Strips {@code executable()} conditions from profile activations.
     * <p>
     * The {@code executable()} function evaluates against the local system {@code PATH},
     * making it environment-dependent. When such a condition survives into a published
     * consumer POM, downstream consumers silently evaluate it against <em>their own</em>
     * {@code PATH}, producing non-reproducible builds. This method removes the entire
     * {@code condition} string when it contains an {@code executable()} call, and drops
     * the activation entirely when no other activation triggers remain.
     *
     * @param profiles the list of profiles to process
     * @return a new list with {@code executable()} conditions stripped
     */
    static List<Profile> stripExecutableConditions(List<Profile> profiles) {
        return profiles.stream()
                .map(p -> p.withActivation(stripExecutableCondition(p.getActivation())))
                .collect(Collectors.toList());
    }

    /**
     * Strips an {@code executable()} condition from a single activation.
     * Returns {@code null} when the activation has no remaining triggers after stripping.
     */
    private static Activation stripExecutableCondition(Activation activation) {
        if (activation == null) {
            return null;
        }
        String condition = activation.getCondition();
        if (condition == null || !condition.contains("executable(")) {
            return activation;
        }
        // Remove the entire condition — partial expression surgery could change
        // the boolean semantics in unexpected ways (e.g. AND vs OR combinations).
        Activation stripped = activation.withCondition(null);
        if (isActivationEmpty(stripped)) {
            return null;
        }
        return stripped;
    }

    /**
     * Returns {@code true} when the activation carries no triggers at all
     * (default {@code activeByDefault} is {@code false}).
     */
    private static boolean isActivationEmpty(Activation activation) {
        return !activation.isActiveByDefault()
                && activation.getJdk() == null
                && activation.getOs() == null
                && activation.getProperty() == null
                && activation.getFile() == null
                && activation.getPackaging() == null
                && activation.getCondition() == null;
    }

    private static List<Profile> prune(List<Profile> profiles) {
        return profiles.stream()
                .map(p -> {
                    Profile.Builder builder = Profile.newBuilder(p, true);
                    prune((ModelBase.Builder) builder, p);
                    builder.activation(stripExecutableCondition(p.getActivation()));
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
