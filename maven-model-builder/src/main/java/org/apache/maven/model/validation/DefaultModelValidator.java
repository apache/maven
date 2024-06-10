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
package org.apache.maven.model.validation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.interpolation.ModelVersionProcessor;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
@Named
@Singleton
public class DefaultModelValidator implements ModelValidator {

    private static final Pattern CI_FRIENDLY_EXPRESSION = Pattern.compile("\\$\\{(.+?)}");
    private static final Pattern EXPRESSION_PROJECT_NAME_PATTERN = Pattern.compile("\\$\\{(project.+?)}");

    private static final String ILLEGAL_FS_CHARS = "\\/:\"<>|?*";

    private static final String ILLEGAL_VERSION_CHARS = ILLEGAL_FS_CHARS;

    private static final String ILLEGAL_REPO_ID_CHARS = ILLEGAL_FS_CHARS;

    private static final String EMPTY = "";

    private final Set<String> validIds = new HashSet<>();

    private ModelVersionProcessor versionProcessor;

    @Inject
    public DefaultModelValidator(ModelVersionProcessor versionProcessor) {
        this.versionProcessor = versionProcessor;
    }

    @SuppressWarnings("checkstyle:methodlength")
    @Override
    public void validateRawModel(Model m, ModelBuildingRequest request, ModelProblemCollector problems) {
        Parent parent = m.getParent();
        if (parent != null) {
            validateStringNotEmpty(
                    "parent.groupId", problems, Severity.FATAL, Version.BASE, parent.getGroupId(), parent);

            validateStringNotEmpty(
                    "parent.artifactId", problems, Severity.FATAL, Version.BASE, parent.getArtifactId(), parent);

            validateStringNotEmpty(
                    "parent.version", problems, Severity.FATAL, Version.BASE, parent.getVersion(), parent);

            if (equals(parent.getGroupId(), m.getGroupId()) && equals(parent.getArtifactId(), m.getArtifactId())) {
                addViolation(
                        problems,
                        Severity.FATAL,
                        Version.BASE,
                        "parent.artifactId",
                        null,
                        "must be changed"
                                + ", the parent element cannot have the same groupId:artifactId as the project.",
                        parent);
            }

            if (equals("LATEST", parent.getVersion()) || equals("RELEASE", parent.getVersion())) {
                addViolation(
                        problems,
                        Severity.WARNING,
                        Version.BASE,
                        "parent.version",
                        null,
                        "is either LATEST or RELEASE (both of them are being deprecated)",
                        parent);
            }
        }

        if (request.getValidationLevel() == ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL) {
            // profiles: they are essential for proper model building (may contribute profiles, dependencies...)
            HashSet<String> minProfileIds = new HashSet<>();
            for (Profile profile : m.getProfiles()) {
                if (!minProfileIds.add(profile.getId())) {
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.BASE,
                            "profiles.profile.id",
                            null,
                            "Duplicate activation for profile " + profile.getId(),
                            profile);
                }
            }
        } else if (request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

            // [MNG-6074] Maven should produce an error if no model version has been set in a POM file used to build an
            // effective model.
            //
            // As of 3.4, the model version is mandatory even in raw models. The XML element still is optional in the
            // XML schema and this will not change anytime soon. We do not want to build effective models based on
            // models without a version starting with 3.4.
            validateStringNotEmpty("modelVersion", problems, Severity.ERROR, Version.V20, m.getModelVersion(), m);

            validateModelVersion(problems, m.getModelVersion(), m, "4.0.0");

            validateStringNoExpression("groupId", problems, Severity.WARNING, Version.V20, m.getGroupId(), m);
            if (parent == null) {
                validateStringNotEmpty("groupId", problems, Severity.FATAL, Version.V20, m.getGroupId(), m);
            }

            validateStringNoExpression("artifactId", problems, Severity.WARNING, Version.V20, m.getArtifactId(), m);
            validateStringNotEmpty("artifactId", problems, Severity.FATAL, Version.V20, m.getArtifactId(), m);

            validateVersionNoExpression("version", problems, Severity.WARNING, Version.V20, m.getVersion(), m);
            if (parent == null) {
                validateStringNotEmpty("version", problems, Severity.FATAL, Version.V20, m.getVersion(), m);
            }

            validate20RawDependencies(problems, m.getDependencies(), "dependencies.dependency.", EMPTY, request);

            validate20RawDependenciesSelfReferencing(
                    problems, m, m.getDependencies(), "dependencies.dependency", request);

            if (m.getDependencyManagement() != null) {
                validate20RawDependencies(
                        problems,
                        m.getDependencyManagement().getDependencies(),
                        "dependencyManagement.dependencies.dependency.",
                        EMPTY,
                        request);
            }

            validateRawRepositories(problems, m.getRepositories(), "repositories.repository.", EMPTY, request);

            validateRawRepositories(
                    problems, m.getPluginRepositories(), "pluginRepositories.pluginRepository.", EMPTY, request);

            Build build = m.getBuild();
            if (build != null) {
                validate20RawPlugins(problems, build.getPlugins(), "build.plugins.plugin.", EMPTY, request);

                PluginManagement mgmt = build.getPluginManagement();
                if (mgmt != null) {
                    validate20RawPlugins(
                            problems, mgmt.getPlugins(), "build.pluginManagement.plugins.plugin.", EMPTY, request);
                }
            }

            Set<String> profileIds = new HashSet<>();

            for (Profile profile : m.getProfiles()) {
                String prefix = "profiles.profile[" + profile.getId() + "].";

                if (!profileIds.add(profile.getId())) {
                    addViolation(
                            problems,
                            errOn30,
                            Version.V20,
                            "profiles.profile.id",
                            null,
                            "must be unique but found duplicate profile with id " + profile.getId(),
                            profile);
                }

                validate30RawProfileActivation(problems, profile.getActivation(), prefix);

                validate20RawDependencies(
                        problems, profile.getDependencies(), prefix, "dependencies.dependency.", request);

                if (profile.getDependencyManagement() != null) {
                    validate20RawDependencies(
                            problems,
                            profile.getDependencyManagement().getDependencies(),
                            prefix,
                            "dependencyManagement.dependencies.dependency.",
                            request);
                }

                validateRawRepositories(
                        problems, profile.getRepositories(), prefix, "repositories.repository.", request);

                validateRawRepositories(
                        problems,
                        profile.getPluginRepositories(),
                        prefix,
                        "pluginRepositories.pluginRepository.",
                        request);

                BuildBase buildBase = profile.getBuild();
                if (buildBase != null) {
                    validate20RawPlugins(problems, buildBase.getPlugins(), prefix, "plugins.plugin.", request);

                    PluginManagement mgmt = buildBase.getPluginManagement();
                    if (mgmt != null) {
                        validate20RawPlugins(
                                problems, mgmt.getPlugins(), prefix, "pluginManagement.plugins.plugin.", request);
                    }
                }
            }
        }
    }

    private void validate30RawProfileActivation(ModelProblemCollector problems, Activation activation, String prefix) {
        if (activation == null) {
            return;
        }
        class ActivationFrame {
            String location;
            Optional<? extends InputLocationTracker> parent;

            ActivationFrame(String location, Optional<? extends InputLocationTracker> parent) {
                this.location = location;
                this.parent = parent;
            }
        }
        final Deque<ActivationFrame> stk = new LinkedList<>();

        final Supplier<String> pathSupplier = () -> {
            final boolean parallel = false;
            return StreamSupport.stream(((Iterable<ActivationFrame>) stk::descendingIterator).spliterator(), parallel)
                    .map(f -> f.location)
                    .collect(Collectors.joining("."));
        };
        final Supplier<InputLocation> locationSupplier = () -> {
            if (stk.size() < 2) {
                return null;
            }
            Iterator<ActivationFrame> f = stk.iterator();

            String location = f.next().location;
            ActivationFrame parent = f.next();

            return parent.parent.map(p -> p.getLocation(location)).orElse(null);
        };
        final Consumer<String> validator = s -> {
            if (hasProjectExpression(s)) {
                String path = pathSupplier.get();
                Matcher matcher = EXPRESSION_PROJECT_NAME_PATTERN.matcher(s);
                while (matcher.find()) {
                    String propertyName = matcher.group(0);

                    if (path.startsWith("activation.file.") && "${project.basedir}".equals(propertyName)) {
                        continue;
                    }
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.V30,
                            prefix + path,
                            null,
                            "Failed to interpolate profile activation property " + s + ": " + propertyName
                                    + " expressions are not supported during profile activation.",
                            locationSupplier.get());
                }
            }
        };
        Optional<Activation> root = Optional.of(activation);
        stk.push(new ActivationFrame("activation", root));
        root.map(Activation::getFile).ifPresent(fa -> {
            stk.push(new ActivationFrame("file", Optional.of(fa)));
            stk.push(new ActivationFrame("exists", Optional.empty()));
            validator.accept(fa.getExists());
            stk.peek().location = "missing";
            validator.accept(fa.getMissing());
            stk.pop();
            stk.pop();
        });
        root.map(Activation::getOs).ifPresent(oa -> {
            stk.push(new ActivationFrame("os", Optional.of(oa)));
            stk.push(new ActivationFrame("arch", Optional.empty()));
            validator.accept(oa.getArch());
            stk.peek().location = "family";
            validator.accept(oa.getFamily());
            stk.peek().location = "name";
            validator.accept(oa.getName());
            stk.peek().location = "version";
            validator.accept(oa.getVersion());
            stk.pop();
            stk.pop();
        });
        root.map(Activation::getProperty).ifPresent(pa -> {
            stk.push(new ActivationFrame("property", Optional.of(pa)));
            stk.push(new ActivationFrame("name", Optional.empty()));
            validator.accept(pa.getName());
            stk.peek().location = "value";
            validator.accept(pa.getValue());
            stk.pop();
            stk.pop();
        });
        root.map(Activation::getJdk).ifPresent(jdk -> {
            stk.push(new ActivationFrame("jdk", Optional.empty()));
            validator.accept(jdk);
            stk.pop();
        });
    }

    private void validate20RawPlugins(
            ModelProblemCollector problems,
            List<Plugin> plugins,
            String prefix,
            String prefix2,
            ModelBuildingRequest request) {
        Severity errOn31 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        Map<String, Plugin> index = new HashMap<>();

        for (Plugin plugin : plugins) {
            if (plugin.getGroupId() == null
                    || (plugin.getGroupId() != null
                            && plugin.getGroupId().trim().isEmpty())) {
                addViolation(
                        problems,
                        Severity.FATAL,
                        Version.V20,
                        prefix + prefix2 + "(groupId:artifactId)",
                        null,
                        "groupId of a plugin must be defined. ",
                        plugin);
            }

            if (plugin.getArtifactId() == null
                    || (plugin.getArtifactId() != null
                            && plugin.getArtifactId().trim().isEmpty())) {
                addViolation(
                        problems,
                        Severity.FATAL,
                        Version.V20,
                        prefix + prefix2 + "(groupId:artifactId)",
                        null,
                        "artifactId of a plugin must be defined. ",
                        plugin);
            }

            // This will catch cases like <version></version> or <version/>
            if (plugin.getVersion() != null && plugin.getVersion().trim().isEmpty()) {
                addViolation(
                        problems,
                        Severity.FATAL,
                        Version.V20,
                        prefix + prefix2 + "(groupId:artifactId)",
                        null,
                        "version of a plugin must be defined. ",
                        plugin);
            }

            String key = plugin.getKey();

            Plugin existing = index.get(key);

            if (existing != null) {
                addViolation(
                        problems,
                        errOn31,
                        Version.V20,
                        prefix + prefix2 + "(groupId:artifactId)",
                        null,
                        "must be unique but found duplicate declaration of plugin " + key,
                        plugin);
            } else {
                index.put(key, plugin);
            }

            Set<String> executionIds = new HashSet<>();

            for (PluginExecution exec : plugin.getExecutions()) {
                if (!executionIds.add(exec.getId())) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            prefix + prefix2 + "[" + plugin.getKey() + "].executions.execution.id",
                            null,
                            "must be unique but found duplicate execution with id " + exec.getId(),
                            exec);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void validateEffectiveModel(Model m, ModelBuildingRequest request, ModelProblemCollector problems) {
        validateStringNotEmpty("modelVersion", problems, Severity.ERROR, Version.BASE, m.getModelVersion(), m);

        validateId("groupId", problems, m.getGroupId(), m);

        validateId("artifactId", problems, m.getArtifactId(), m);

        validateStringNotEmpty("packaging", problems, Severity.ERROR, Version.BASE, m.getPackaging(), m);

        if (!m.getModules().isEmpty()) {
            if (!"pom".equals(m.getPackaging())) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.BASE,
                        "packaging",
                        null,
                        "with value '" + m.getPackaging() + "' is invalid. Aggregator projects "
                                + "require 'pom' as packaging.",
                        m);
            }

            for (int i = 0, n = m.getModules().size(); i < n; i++) {
                String module = m.getModules().get(i);
                if (StringUtils.isBlank(module)) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.BASE,
                            "modules.module[" + i + "]",
                            null,
                            "has been specified without a path to the project directory.",
                            m.getLocation("modules"));
                }
            }
        }

        validateStringNotEmpty("version", problems, Severity.ERROR, Version.BASE, m.getVersion(), m);

        Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

        validateEffectiveDependencies(problems, m, m.getDependencies(), false, request);

        DependencyManagement mgmt = m.getDependencyManagement();
        if (mgmt != null) {
            validateEffectiveDependencies(problems, m, mgmt.getDependencies(), true, request);
        }

        if (request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            Set<String> modules = new HashSet<>();
            for (int i = 0, n = m.getModules().size(); i < n; i++) {
                String module = m.getModules().get(i);
                if (!modules.add(module)) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            "modules.module[" + i + "]",
                            null,
                            "specifies duplicate child module " + module,
                            m.getLocation("modules"));
                }
            }

            Severity errOn31 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

            validateBannedCharacters(
                    EMPTY, "version", problems, errOn31, Version.V20, m.getVersion(), null, m, ILLEGAL_VERSION_CHARS);
            validate20ProperSnapshotVersion("version", problems, errOn31, Version.V20, m.getVersion(), null, m);

            Build build = m.getBuild();
            if (build != null) {
                for (Plugin p : build.getPlugins()) {
                    validateStringNotEmpty(
                            "build.plugins.plugin.artifactId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            p.getArtifactId(),
                            p);

                    validateStringNotEmpty(
                            "build.plugins.plugin.groupId", problems, Severity.ERROR, Version.V20, p.getGroupId(), p);

                    validate20PluginVersion(
                            "build.plugins.plugin.version", problems, p.getVersion(), p.getKey(), p, request);

                    validateBoolean(
                            "build.plugins.plugin.inherited",
                            EMPTY,
                            problems,
                            errOn30,
                            Version.V20,
                            p.getInherited(),
                            p.getKey(),
                            p);

                    validateBoolean(
                            "build.plugins.plugin.extensions",
                            EMPTY,
                            problems,
                            errOn30,
                            Version.V20,
                            p.getExtensions(),
                            p.getKey(),
                            p);

                    validate20EffectivePluginDependencies(problems, p, request);
                }

                validate20RawResources(problems, build.getResources(), "build.resources.resource.", request);

                validate20RawResources(
                        problems, build.getTestResources(), "build.testResources.testResource.", request);
            }

            Reporting reporting = m.getReporting();
            if (reporting != null) {
                for (ReportPlugin p : reporting.getPlugins()) {
                    validateStringNotEmpty(
                            "reporting.plugins.plugin.artifactId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            p.getArtifactId(),
                            p);

                    validateStringNotEmpty(
                            "reporting.plugins.plugin.groupId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            p.getGroupId(),
                            p);
                }
            }

            for (Repository repository : m.getRepositories()) {
                validate20EffectiveRepository(problems, repository, "repositories.repository.", request);
            }

            for (Repository repository : m.getPluginRepositories()) {
                validate20EffectiveRepository(problems, repository, "pluginRepositories.pluginRepository.", request);
            }

            DistributionManagement distMgmt = m.getDistributionManagement();
            if (distMgmt != null) {
                if (distMgmt.getStatus() != null) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            "distributionManagement.status",
                            null,
                            "must not be specified.",
                            distMgmt);
                }

                validate20EffectiveRepository(
                        problems, distMgmt.getRepository(), "distributionManagement.repository.", request);
                validate20EffectiveRepository(
                        problems,
                        distMgmt.getSnapshotRepository(),
                        "distributionManagement.snapshotRepository.",
                        request);
            }
        }
    }

    private void validate20RawDependencies(
            ModelProblemCollector problems,
            List<Dependency> dependencies,
            String prefix,
            String prefix2,
            ModelBuildingRequest request) {
        Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);
        Severity errOn31 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        Map<String, Dependency> index = new HashMap<>();

        for (Dependency dependency : dependencies) {
            String key = dependency.getManagementKey();

            if ("import".equals(dependency.getScope())) {
                if (!"pom".equals(dependency.getType())) {
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            prefix + prefix2 + "type",
                            key,
                            "must be 'pom' to import the managed dependencies.",
                            dependency);
                } else if (StringUtils.isNotEmpty(dependency.getClassifier())) {
                    addViolation(
                            problems,
                            errOn30,
                            Version.V20,
                            prefix + prefix2 + "classifier",
                            key,
                            "must be empty, imported POM cannot have a classifier.",
                            dependency);
                }
            } else if ("system".equals(dependency.getScope())) {

                if (request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1) {
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.V31,
                            prefix + prefix2 + "scope",
                            key,
                            "declares usage of deprecated 'system' scope ",
                            dependency);
                }

                String sysPath = dependency.getSystemPath();
                if (StringUtils.isNotEmpty(sysPath)) {
                    if (!hasExpression(sysPath)) {
                        addViolation(
                                problems,
                                Severity.WARNING,
                                Version.V20,
                                prefix + prefix2 + "systemPath",
                                key,
                                "should use a variable instead of a hard-coded path " + sysPath,
                                dependency);
                    } else if (sysPath.contains("${basedir}") || sysPath.contains("${project.basedir}")) {
                        addViolation(
                                problems,
                                Severity.WARNING,
                                Version.V20,
                                prefix + prefix2 + "systemPath",
                                key,
                                "should not point at files within the project directory, " + sysPath
                                        + " will be unresolvable by dependent projects",
                                dependency);
                    }
                }
            }

            if (equals("LATEST", dependency.getVersion()) || equals("RELEASE", dependency.getVersion())) {
                addViolation(
                        problems,
                        Severity.WARNING,
                        Version.BASE,
                        prefix + prefix2 + "version",
                        key,
                        "is either LATEST or RELEASE (both of them are being deprecated)",
                        dependency);
            }

            Dependency existing = index.get(key);

            if (existing != null) {
                String msg;
                if (equals(existing.getVersion(), dependency.getVersion())) {
                    msg = "duplicate declaration of version " + Objects.toString(dependency.getVersion(), "(?)");
                } else {
                    msg = "version " + Objects.toString(existing.getVersion(), "(?)") + " vs "
                            + Objects.toString(dependency.getVersion(), "(?)");
                }

                addViolation(
                        problems,
                        errOn31,
                        Version.V20,
                        prefix + prefix2 + "(groupId:artifactId:type:classifier)",
                        null,
                        "must be unique: " + key + " -> " + msg,
                        dependency);
            } else {
                index.put(key, dependency);
            }
        }
    }

    private void validate20RawDependenciesSelfReferencing(
            ModelProblemCollector problems,
            Model m,
            List<Dependency> dependencies,
            String prefix,
            ModelBuildingRequest request) {
        // We only check for groupId/artifactId/version/classifier cause if there is another
        // module with the same groupId/artifactId/version/classifier this will fail the build
        // earlier like "Project '...' is duplicated in the reactor.
        // So it is sufficient to check only groupId/artifactId/version/classifier and not the
        // packaging type.
        for (Dependency dependency : dependencies) {
            String key = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion()
                    + (dependency.getClassifier() != null ? ":" + dependency.getClassifier() : EMPTY);
            String mKey = m.getGroupId() + ":" + m.getArtifactId() + ":" + m.getVersion();
            if (key.equals(mKey)) {
                // This means a module which is build has a dependency which has the same
                // groupId, artifactId, version and classifier coordinates. This is in consequence
                // a self reference or in other words a circular reference which can not being resolved.
                addViolation(
                        problems,
                        Severity.FATAL,
                        Version.V31,
                        prefix + "[" + key + "]",
                        key,
                        "is referencing itself.",
                        dependency);
            }
        }
    }

    private void validateEffectiveDependencies(
            ModelProblemCollector problems,
            Model m,
            List<Dependency> dependencies,
            boolean management,
            ModelBuildingRequest request) {
        Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

        String prefix = management ? "dependencyManagement.dependencies.dependency." : "dependencies.dependency.";

        for (Dependency d : dependencies) {
            validateEffectiveDependency(problems, d, management, prefix, request);

            if (request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
                validateBoolean(
                        prefix, "optional", problems, errOn30, Version.V20, d.getOptional(), d.getManagementKey(), d);

                if (!management) {
                    validateVersion(
                            prefix, "version", problems, errOn30, Version.V20, d.getVersion(), d.getManagementKey(), d);

                    /*
                     * TODO Extensions like Flex Mojos use custom scopes like "merged", "internal", "external", etc. In
                     * order to don't break backward-compat with those, only warn but don't error out.
                     */
                    validateEnum(
                            prefix,
                            "scope",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            d.getScope(),
                            d.getManagementKey(),
                            d,
                            "provided",
                            "compile",
                            "runtime",
                            "test",
                            "system");

                    validateEffectiveModelAgainstDependency(prefix, problems, m, d, request);
                } else {
                    validateEnum(
                            prefix,
                            "scope",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            d.getScope(),
                            d.getManagementKey(),
                            d,
                            "provided",
                            "compile",
                            "runtime",
                            "test",
                            "system",
                            "import");
                }
            }
        }
    }

    private void validateEffectiveModelAgainstDependency(
            String prefix, ModelProblemCollector problems, Model m, Dependency d, ModelBuildingRequest request) {
        String key = d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion()
                + (d.getClassifier() != null ? ":" + d.getClassifier() : EMPTY);
        String mKey = m.getGroupId() + ":" + m.getArtifactId() + ":" + m.getVersion();
        if (key.equals(mKey)) {
            // This means a module which is build has a dependency which has the same
            // groupId, artifactId, version and classifier coordinates. This is in consequence
            // a self reference or in other words a circular reference which can not being resolved.
            addViolation(
                    problems, Severity.FATAL, Version.V31, prefix + "[" + key + "]", key, "is referencing itself.", d);
        }
    }

    private void validate20EffectivePluginDependencies(
            ModelProblemCollector problems, Plugin plugin, ModelBuildingRequest request) {
        List<Dependency> dependencies = plugin.getDependencies();

        if (!dependencies.isEmpty()) {
            String prefix = "build.plugins.plugin[" + plugin.getKey() + "].dependencies.dependency.";

            Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

            for (Dependency d : dependencies) {
                validateEffectiveDependency(problems, d, false, prefix, request);

                validateVersion(
                        prefix, "version", problems, errOn30, Version.BASE, d.getVersion(), d.getManagementKey(), d);

                validateEnum(
                        prefix,
                        "scope",
                        problems,
                        errOn30,
                        Version.BASE,
                        d.getScope(),
                        d.getManagementKey(),
                        d,
                        "compile",
                        "runtime",
                        "system");
            }
        }
    }

    private void validateEffectiveDependency(
            ModelProblemCollector problems,
            Dependency d,
            boolean management,
            String prefix,
            ModelBuildingRequest request) {
        validateId(
                prefix,
                "artifactId",
                problems,
                Severity.ERROR,
                Version.BASE,
                d.getArtifactId(),
                d.getManagementKey(),
                d);

        validateId(prefix, "groupId", problems, Severity.ERROR, Version.BASE, d.getGroupId(), d.getManagementKey(), d);

        if (!management) {
            validateStringNotEmpty(
                    prefix, "type", problems, Severity.ERROR, Version.BASE, d.getType(), d.getManagementKey(), d);

            validateDependencyVersion(problems, d, prefix);
        }

        if ("system".equals(d.getScope())) {
            String systemPath = d.getSystemPath();

            if (StringUtils.isEmpty(systemPath)) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.BASE,
                        prefix + "systemPath",
                        d.getManagementKey(),
                        "is missing.",
                        d);
            } else {
                File sysFile = new File(systemPath);
                if (!sysFile.isAbsolute()) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.BASE,
                            prefix + "systemPath",
                            d.getManagementKey(),
                            "must specify an absolute path but is " + systemPath,
                            d);
                } else if (!sysFile.isFile()) {
                    String msg = "refers to a non-existing file " + sysFile.getAbsolutePath();
                    systemPath = systemPath.replace('/', File.separatorChar).replace('\\', File.separatorChar);
                    String jdkHome =
                            request.getSystemProperties().getProperty("java.home", EMPTY) + File.separator + "..";
                    if (systemPath.startsWith(jdkHome)) {
                        msg += ". Please verify that you run Maven using a JDK and not just a JRE.";
                    }
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.BASE,
                            prefix + "systemPath",
                            d.getManagementKey(),
                            msg,
                            d);
                }
            }
        } else if (StringUtils.isNotEmpty(d.getSystemPath())) {
            addViolation(
                    problems,
                    Severity.ERROR,
                    Version.BASE,
                    prefix + "systemPath",
                    d.getManagementKey(),
                    "must be omitted." + " This field may only be specified for a dependency with system scope.",
                    d);
        }

        if (request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0) {
            for (Exclusion exclusion : d.getExclusions()) {
                if (request.getValidationLevel() < ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0) {
                    validateId(
                            prefix,
                            "exclusions.exclusion.groupId",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            exclusion.getGroupId(),
                            d.getManagementKey(),
                            exclusion);

                    validateId(
                            prefix,
                            "exclusions.exclusion.artifactId",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            exclusion.getArtifactId(),
                            d.getManagementKey(),
                            exclusion);
                } else {
                    validateIdWithWildcards(
                            prefix,
                            "exclusions.exclusion.groupId",
                            problems,
                            Severity.WARNING,
                            Version.V30,
                            exclusion.getGroupId(),
                            d.getManagementKey(),
                            exclusion);

                    validateIdWithWildcards(
                            prefix,
                            "exclusions.exclusion.artifactId",
                            problems,
                            Severity.WARNING,
                            Version.V30,
                            exclusion.getArtifactId(),
                            d.getManagementKey(),
                            exclusion);
                }
            }
        }
    }

    /**
     * @since 3.2.4
     */
    protected void validateDependencyVersion(ModelProblemCollector problems, Dependency d, String prefix) {
        validateStringNotEmpty(
                prefix, "version", problems, Severity.ERROR, Version.BASE, d.getVersion(), d.getManagementKey(), d);
    }

    private void validateRawRepositories(
            ModelProblemCollector problems,
            List<Repository> repositories,
            String prefix,
            String prefix2,
            ModelBuildingRequest request) {
        Map<String, Repository> index = new HashMap<>();

        for (Repository repository : repositories) {
            validateStringNotEmpty(
                    prefix, prefix2, "id", problems, Severity.ERROR, Version.V20, repository.getId(), null, repository);

            validateStringNotEmpty(
                    prefix,
                    prefix2,
                    "[" + repository.getId() + "].url",
                    problems,
                    Severity.ERROR,
                    Version.V20,
                    repository.getUrl(),
                    null,
                    repository);

            String key = repository.getId();

            Repository existing = index.get(key);

            if (existing != null) {
                Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

                addViolation(
                        problems,
                        errOn30,
                        Version.V20,
                        prefix + prefix2 + "id",
                        null,
                        "must be unique: " + repository.getId() + " -> " + existing.getUrl() + " vs "
                                + repository.getUrl(),
                        repository);
            } else {
                index.put(key, repository);
            }
        }
    }

    private void validate20EffectiveRepository(
            ModelProblemCollector problems, Repository repository, String prefix, ModelBuildingRequest request) {
        if (repository != null) {
            Severity errOn31 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

            validateBannedCharacters(
                    prefix,
                    "id",
                    problems,
                    errOn31,
                    Version.V20,
                    repository.getId(),
                    null,
                    repository,
                    ILLEGAL_REPO_ID_CHARS);

            if ("local".equals(repository.getId())) {
                addViolation(
                        problems,
                        errOn31,
                        Version.V20,
                        prefix + "id",
                        null,
                        "must not be 'local'" + ", this identifier is reserved for the local repository"
                                + ", using it for other repositories will corrupt your repository metadata.",
                        repository);
            }

            if ("legacy".equals(repository.getLayout())) {
                addViolation(
                        problems,
                        Severity.WARNING,
                        Version.V20,
                        prefix + "layout",
                        repository.getId(),
                        "uses the unsupported value 'legacy', artifact resolution might fail.",
                        repository);
            }
        }
    }

    private void validate20RawResources(
            ModelProblemCollector problems, List<Resource> resources, String prefix, ModelBuildingRequest request) {
        Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

        for (Resource resource : resources) {
            validateStringNotEmpty(
                    prefix,
                    "directory",
                    problems,
                    Severity.ERROR,
                    Version.V20,
                    resource.getDirectory(),
                    null,
                    resource);

            validateBoolean(
                    prefix,
                    "filtering",
                    problems,
                    errOn30,
                    Version.V20,
                    resource.getFiltering(),
                    resource.getDirectory(),
                    resource);
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    private boolean validateId(
            String fieldName, ModelProblemCollector problems, String id, InputLocationTracker tracker) {
        return validateId(EMPTY, fieldName, problems, Severity.ERROR, Version.BASE, id, null, tracker);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateId(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String id,
            String sourceHint,
            InputLocationTracker tracker) {
        if (validIds.contains(id)) {
            return true;
        }
        if (!validateStringNotEmpty(prefix, fieldName, problems, severity, version, id, sourceHint, tracker)) {
            return false;
        } else {
            if (!isValidId(id)) {
                addViolation(
                        problems,
                        severity,
                        version,
                        prefix + fieldName,
                        sourceHint,
                        "with value '" + id + "' does not match a valid id pattern.",
                        tracker);
                return false;
            }
            validIds.add(id);
            return true;
        }
    }

    private boolean isValidId(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!isValidIdCharacter(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidIdCharacter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_' || c == '.';
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateIdWithWildcards(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String id,
            String sourceHint,
            InputLocationTracker tracker) {
        if (!validateStringNotEmpty(prefix, fieldName, problems, severity, version, id, sourceHint, tracker)) {
            return false;
        } else {
            if (!isValidIdWithWildCards(id)) {
                addViolation(
                        problems,
                        severity,
                        version,
                        prefix + fieldName,
                        sourceHint,
                        "with value '" + id + "' does not match a valid id pattern.",
                        tracker);
                return false;
            }
            return true;
        }
    }

    private boolean isValidIdWithWildCards(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!isValidIdWithWildCardCharacter(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidIdWithWildCardCharacter(char c) {
        return isValidIdCharacter(c) || c == '?' || c == '*';
    }

    private boolean validateStringNoExpression(
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            InputLocationTracker tracker) {
        if (!hasExpression(string)) {
            return true;
        }

        addViolation(
                problems,
                severity,
                version,
                fieldName,
                null,
                "contains an expression but should be a constant.",
                tracker);

        return false;
    }

    private boolean validateVersionNoExpression(
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            InputLocationTracker tracker) {
        if (!hasExpression(string)) {
            return true;
        }

        Matcher m = CI_FRIENDLY_EXPRESSION.matcher(string.trim());
        while (m.find()) {
            String property = m.group(1);
            if (!versionProcessor.isValidProperty(property)) {
                addViolation(
                        problems,
                        severity,
                        version,
                        fieldName,
                        null,
                        "contains an expression but should be a constant.",
                        tracker);
                return false;
            }
        }

        return true;
    }

    private boolean hasExpression(String value) {
        return value != null && value.contains("${");
    }

    private boolean hasProjectExpression(String value) {
        return value != null && value.contains("${project.");
    }

    private boolean validateStringNotEmpty(
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            InputLocationTracker tracker) {
        return validateStringNotEmpty(EMPTY, fieldName, problems, severity, version, string, null, tracker);
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateStringNotEmpty(
            String prefix,
            String prefix2,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker) {
        if (!validateNotNull(prefix, prefix2, fieldName, problems, severity, version, string, sourceHint, tracker)) {
            return false;
        }

        if (!string.isEmpty()) {
            return true;
        }

        addViolation(problems, severity, version, prefix + prefix2 + fieldName, sourceHint, "is missing.", tracker);

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * <li><code>string.length > 0</code>
     * </ul>
     */
    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateStringNotEmpty(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker) {
        if (!validateNotNull(prefix, fieldName, problems, severity, version, string, sourceHint, tracker)) {
            return false;
        }

        if (string.length() > 0) {
            return true;
        }

        addViolation(problems, severity, version, prefix + fieldName, sourceHint, "is missing.", tracker);

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateNotNull(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            Object object,
            String sourceHint,
            InputLocationTracker tracker) {
        if (object != null) {
            return true;
        }

        addViolation(problems, severity, version, prefix + fieldName, sourceHint, "is missing.", tracker);

        return false;
    }

    /**
     * Asserts:
     * <p/>
     * <ul>
     * <li><code>string != null</code>
     * </ul>
     */
    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateNotNull(
            String prefix,
            String prefix2,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            Object object,
            String sourceHint,
            InputLocationTracker tracker) {
        if (object != null) {
            return true;
        }

        addViolation(problems, severity, version, prefix + prefix2 + fieldName, sourceHint, "is missing.", tracker);

        return false;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateBoolean(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker) {
        if (string == null || string.length() <= 0) {
            return true;
        }

        if ("true".equalsIgnoreCase(string) || "false".equalsIgnoreCase(string)) {
            return true;
        }

        addViolation(
                problems,
                severity,
                version,
                prefix + fieldName,
                sourceHint,
                "must be 'true' or 'false' but is '" + string + "'.",
                tracker);

        return false;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateEnum(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker,
            String... validValues) {
        if (string == null || string.length() <= 0) {
            return true;
        }

        List<String> values = Arrays.asList(validValues);

        if (values.contains(string)) {
            return true;
        }

        addViolation(
                problems,
                severity,
                version,
                prefix + fieldName,
                sourceHint,
                "must be one of " + values + " but is '" + string + "'.",
                tracker);

        return false;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateModelVersion(
            ModelProblemCollector problems, String string, InputLocationTracker tracker, String... validVersions) {
        if (string == null || string.length() <= 0) {
            return true;
        }

        List<String> values = Arrays.asList(validVersions);

        if (values.contains(string)) {
            return true;
        }

        boolean newerThanAll = true;
        boolean olderThanAll = true;
        for (String validValue : validVersions) {
            final int comparison = compareModelVersions(validValue, string);
            newerThanAll = newerThanAll && comparison < 0;
            olderThanAll = olderThanAll && comparison > 0;
        }

        if (newerThanAll) {
            addViolation(
                    problems,
                    Severity.FATAL,
                    Version.V20,
                    "modelVersion",
                    null,
                    "of '" + string + "' is newer than the versions supported by this version of Maven: " + values
                            + ". Building this project requires a newer version of Maven.",
                    tracker);

        } else if (olderThanAll) {
            // note this will not be hit for Maven 1.x project.xml as it is an incompatible schema
            addViolation(
                    problems,
                    Severity.FATAL,
                    Version.V20,
                    "modelVersion",
                    null,
                    "of '" + string + "' is older than the versions supported by this version of Maven: " + values
                            + ". Building this project requires an older version of Maven.",
                    tracker);

        } else {
            addViolation(
                    problems,
                    Severity.ERROR,
                    Version.V20,
                    "modelVersion",
                    null,
                    "must be one of " + values + " but is '" + string + "'.",
                    tracker);
        }

        return false;
    }

    /**
     * Compares two model versions.
     *
     * @param first the first version.
     * @param second the second version.
     * @return negative if the first version is newer than the second version, zero if they are the same or positive if
     * the second version is the newer.
     */
    private static int compareModelVersions(String first, String second) {
        // we use a dedicated comparator because we control our model version scheme.
        String[] firstSegments = StringUtils.split(first, ".");
        String[] secondSegments = StringUtils.split(second, ".");
        for (int i = 0; i < Math.max(firstSegments.length, secondSegments.length); i++) {
            int result = Long.valueOf(i < firstSegments.length ? firstSegments[i] : "0")
                    .compareTo(Long.valueOf(i < secondSegments.length ? secondSegments[i] : "0"));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateBannedCharacters(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker,
            String banned) {
        if (string != null) {
            for (int i = string.length() - 1; i >= 0; i--) {
                if (banned.indexOf(string.charAt(i)) >= 0) {
                    addViolation(
                            problems,
                            severity,
                            version,
                            prefix + fieldName,
                            sourceHint,
                            "must not contain any of these characters " + banned + " but found " + string.charAt(i),
                            tracker);
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateVersion(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker) {
        if (string == null || string.length() <= 0) {
            return true;
        }

        if (hasExpression(string)) {
            addViolation(
                    problems,
                    severity,
                    version,
                    prefix + fieldName,
                    sourceHint,
                    "must be a valid version but is '" + string + "'.",
                    tracker);
            return false;
        }

        return validateBannedCharacters(
                prefix, fieldName, problems, severity, version, string, sourceHint, tracker, ILLEGAL_VERSION_CHARS);
    }

    private boolean validate20ProperSnapshotVersion(
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            String sourceHint,
            InputLocationTracker tracker) {
        if (string == null || string.length() <= 0) {
            return true;
        }

        if (string.endsWith("SNAPSHOT") && !string.endsWith("-SNAPSHOT")) {
            addViolation(
                    problems,
                    severity,
                    version,
                    fieldName,
                    sourceHint,
                    "uses an unsupported snapshot version format, should be '*-SNAPSHOT' instead.",
                    tracker);
            return false;
        }

        return true;
    }

    private boolean validate20PluginVersion(
            String fieldName,
            ModelProblemCollector problems,
            String string,
            String sourceHint,
            InputLocationTracker tracker,
            ModelBuildingRequest request) {
        if (string == null) {
            // NOTE: The check for missing plugin versions is handled directly by the model builder
            return true;
        }

        Severity errOn30 = getSeverity(request, ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);

        if (!validateVersion(EMPTY, fieldName, problems, errOn30, Version.V20, string, sourceHint, tracker)) {
            return false;
        }

        if (string.length() <= 0 || "RELEASE".equals(string) || "LATEST".equals(string)) {
            addViolation(
                    problems,
                    errOn30,
                    Version.V20,
                    fieldName,
                    sourceHint,
                    "must be a valid version but is '" + string + "'.",
                    tracker);
            return false;
        }

        return true;
    }

    private static void addViolation(
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String fieldName,
            String sourceHint,
            String message,
            InputLocationTracker tracker) {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append('\'').append(fieldName).append('\'');

        if (sourceHint != null) {
            buffer.append(" for ").append(sourceHint);
        }

        buffer.append(' ').append(message);

        // CHECKSTYLE_OFF: LineLength
        problems.add(new ModelProblemCollectorRequest(severity, version)
                .setMessage(buffer.toString())
                .setLocation(getLocation(fieldName, tracker)));
        // CHECKSTYLE_ON: LineLength
    }

    private static InputLocation getLocation(String fieldName, InputLocationTracker tracker) {
        InputLocation location = null;

        if (tracker != null) {
            if (fieldName != null) {
                Object key = fieldName;

                int idx = fieldName.lastIndexOf('.');
                if (idx >= 0) {
                    fieldName = fieldName.substring(idx + 1);
                    key = fieldName;
                }

                if (fieldName.endsWith("]")) {
                    key = fieldName.substring(fieldName.lastIndexOf('[') + 1, fieldName.length() - 1);
                    try {
                        key = Integer.valueOf(key.toString());
                    } catch (NumberFormatException e) {
                        // use key as is
                    }
                }

                location = tracker.getLocation(key);
            }

            if (location == null) {
                location = tracker.getLocation(EMPTY);
            }
        }

        return location;
    }

    private static boolean equals(String s1, String s2) {
        return StringUtils.clean(s1).equals(StringUtils.clean(s2));
    }

    private static Severity getSeverity(ModelBuildingRequest request, int errorThreshold) {
        return getSeverity(request.getValidationLevel(), errorThreshold);
    }

    private static Severity getSeverity(int validationLevel, int errorThreshold) {
        if (validationLevel < errorThreshold) {
            return Severity.WARNING;
        } else {
            return Severity.ERROR;
        }
    }
}
