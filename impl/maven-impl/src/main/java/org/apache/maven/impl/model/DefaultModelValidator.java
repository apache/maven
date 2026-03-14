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
package org.apache.maven.impl.model;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.ActivationOS;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.BuildBase;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Exclusion;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputLocationTracker;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ModelValidator;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.model.v4.MavenModelVersion;
import org.apache.maven.model.v4.MavenTransformer;
import org.eclipse.aether.scope.ScopeManager;

/**
 */
@Named
@Singleton
public class DefaultModelValidator implements ModelValidator {
    public static final String BUILD_ALLOW_EXPRESSION_IN_EFFECTIVE_PROJECT_VERSION =
            "maven.build.allowExpressionInEffectiveProjectVersion";

    private static final Pattern EXPRESSION_NAME_PATTERN = Pattern.compile("\\$\\{(.+?)}");
    private static final Pattern EXPRESSION_PROJECT_NAME_PATTERN = Pattern.compile("\\$\\{(project.+?)}");

    private static final String ILLEGAL_FS_CHARS = "\\/:\"<>|?*";

    private static final String ILLEGAL_VERSION_CHARS = ILLEGAL_FS_CHARS;

    private static final String ILLEGAL_REPO_ID_CHARS = ILLEGAL_FS_CHARS;

    private static final String EMPTY = "";

    private record ActivationFrame(String location, Optional<? extends InputLocationTracker> parent) {}

    private static class ActivationWalker extends MavenTransformer {

        private final Deque<ActivationFrame> stk;

        ActivationWalker(Deque<ActivationFrame> stk, UnaryOperator<String> transformer) {
            super(transformer);
            this.stk = stk;
        }

        private ActivationFrame nextFrame(String property) {
            return new ActivationFrame(property, Optional.empty());
        }

        private <P> ActivationFrame nextFrame(String property, Function<P, InputLocationTracker> child) {
            @SuppressWarnings("unchecked")
            final Optional<P> parent = (Optional<P>) stk.peek().parent;
            return new ActivationFrame(property, parent.map(child));
        }

        @Override
        public Activation transformActivation(Activation target) {
            stk.push(new ActivationFrame("activation", Optional.of(target)));
            try {
                return super.transformActivation(target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected Activation.Builder transformActivation_File(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            stk.push(nextFrame("file", Activation::getFile));
            try {
                return super.transformActivation_File(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationFile.Builder transformActivationFile_Exists(
                Supplier<? extends ActivationFile.Builder> creator,
                ActivationFile.Builder builder,
                ActivationFile target) {
            stk.push(nextFrame("exists"));
            try {
                return super.transformActivationFile_Exists(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationFile.Builder transformActivationFile_Missing(
                Supplier<? extends ActivationFile.Builder> creator,
                ActivationFile.Builder builder,
                ActivationFile target) {
            stk.push(nextFrame("missing"));
            try {
                return super.transformActivationFile_Missing(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected Activation.Builder transformActivation_Jdk(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            stk.push(nextFrame("jdk"));
            try {
                return super.transformActivation_Jdk(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected Activation.Builder transformActivation_Os(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            stk.push(nextFrame("os", Activation::getOs));
            try {
                return super.transformActivation_Os(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationOS.Builder transformActivationOS_Arch(
                Supplier<? extends ActivationOS.Builder> creator, ActivationOS.Builder builder, ActivationOS target) {
            stk.push(nextFrame("arch"));
            try {
                return super.transformActivationOS_Arch(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationOS.Builder transformActivationOS_Family(
                Supplier<? extends ActivationOS.Builder> creator, ActivationOS.Builder builder, ActivationOS target) {
            stk.push(nextFrame("family"));
            try {
                return super.transformActivationOS_Family(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationOS.Builder transformActivationOS_Name(
                Supplier<? extends ActivationOS.Builder> creator, ActivationOS.Builder builder, ActivationOS target) {
            stk.push(nextFrame("name"));
            try {
                return super.transformActivationOS_Name(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationOS.Builder transformActivationOS_Version(
                Supplier<? extends ActivationOS.Builder> creator, ActivationOS.Builder builder, ActivationOS target) {
            stk.push(nextFrame("version"));
            try {
                return super.transformActivationOS_Version(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected Activation.Builder transformActivation_Packaging(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            stk.push(nextFrame("packaging"));
            try {
                return super.transformActivation_Packaging(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected Activation.Builder transformActivation_Property(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            stk.push(nextFrame("property", Activation::getProperty));
            try {
                return super.transformActivation_Property(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationProperty.Builder transformActivationProperty_Name(
                Supplier<? extends ActivationProperty.Builder> creator,
                ActivationProperty.Builder builder,
                ActivationProperty target) {
            stk.push(nextFrame("name"));
            try {
                return super.transformActivationProperty_Name(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected ActivationProperty.Builder transformActivationProperty_Value(
                Supplier<? extends ActivationProperty.Builder> creator,
                ActivationProperty.Builder builder,
                ActivationProperty target) {
            stk.push(nextFrame("value"));
            try {
                return super.transformActivationProperty_Value(creator, builder, target);
            } finally {
                stk.pop();
            }
        }

        @Override
        protected Activation.Builder transformActivation_Condition(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            stk.push(nextFrame("condition"));
            try {
                return super.transformActivation_Condition(creator, builder, target);
            } finally {
                stk.pop();
            }
        }
    }

    private final Set<String> validCoordinatesIds = ConcurrentHashMap.newKeySet();

    private final Set<String> validProfileIds = ConcurrentHashMap.newKeySet();

    @Inject
    public DefaultModelValidator() {}

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void validateFileModel(Session session, Model model, int validationLevel, ModelProblemCollector problems) {

        Parent parent = model.getParent();
        if (parent != null) {
            validateStringNotEmpty(
                    "parent.groupId", problems, Severity.FATAL, Version.BASE, parent.getGroupId(), parent);

            validateStringNotEmpty(
                    "parent.artifactId", problems, Severity.FATAL, Version.BASE, parent.getArtifactId(), parent);

            if (equals(parent.getGroupId(), model.getGroupId())
                    && equals(parent.getArtifactId(), model.getArtifactId())) {
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

            if (parent.getRelativePath() != null
                    && !parent.getRelativePath().isEmpty()
                    && (parent.getGroupId() != null && !parent.getGroupId().isEmpty()
                            || parent.getArtifactId() != null
                                    && !parent.getArtifactId().isEmpty())
                    && validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_4_0
                    && ModelBuilder.KNOWN_MODEL_VERSIONS.contains(model.getModelVersion())
                    && !Objects.equals(model.getModelVersion(), ModelBuilder.MODEL_VERSION_4_0_0)) {
                addViolation(
                        problems,
                        Severity.WARNING,
                        Version.BASE,
                        "parent.relativePath",
                        null,
                        "only specify relativePath or groupId/artifactId in modelVersion 4.1.0",
                        parent);
            }
        }

        // Validate mixins
        if (!model.getMixins().isEmpty()) {
            // Ensure model version is at least 4.2.0 when using mixins
            if (compareModelVersions("4.2.0", model.getModelVersion()) < 0) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.V40,
                        "mixins",
                        null,
                        "Mixins are only supported in modelVersion 4.2.0 or higher, but found '"
                                + model.getModelVersion() + "'.",
                        model);
            }

            // Validate each mixin
            for (Parent mixin : model.getMixins()) {
                if (mixin.getRelativePath() != null
                        && !mixin.getRelativePath().isEmpty()
                        && (mixin.getGroupId() != null && !mixin.getGroupId().isEmpty()
                                || mixin.getArtifactId() != null
                                        && !mixin.getArtifactId().isEmpty())
                        && validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_4_0
                        && ModelBuilder.KNOWN_MODEL_VERSIONS.contains(model.getModelVersion())
                        && !Objects.equals(model.getModelVersion(), ModelBuilder.MODEL_VERSION_4_0_0)) {
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.BASE,
                            "mixins.mixin.relativePath",
                            null,
                            "only specify relativePath or groupId/artifactId for mixin",
                            mixin);
                }
            }
        }

        if (validationLevel == ModelValidator.VALIDATION_LEVEL_MINIMAL) {
            // profiles: they are essential for proper model building (may contribute profiles, dependencies...)
            HashSet<String> minProfileIds = new HashSet<>();
            for (Profile profile : model.getProfiles()) {
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
        } else if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_2_0) {
            validateStringNotEmpty(
                    "modelVersion", problems, Severity.ERROR, Version.V20, model.getModelVersion(), model);

            validateModelVersion(session, problems, model.getModelVersion(), model, ModelBuilder.KNOWN_MODEL_VERSIONS);

            Set<String> modules = new HashSet<>();
            for (int index = 0, size = model.getModules().size(); index < size; index++) {
                String module = model.getModules().get(index);
                if (!modules.add(module)) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            "modules.module[" + index + "]",
                            null,
                            "specifies duplicate child module " + module,
                            model.getLocation("modules"));
                }
            }
            String modelVersion = model.getModelVersion();
            if (Objects.equals(modelVersion, ModelBuilder.MODEL_VERSION_4_0_0)) {
                if (!model.getSubprojects().isEmpty()) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.V40,
                            "subprojects",
                            null,
                            "unexpected subprojects element",
                            model.getLocation("subprojects"));
                }
            } else {
                Set<String> subprojects = new HashSet<>();
                for (int index = 0, size = model.getSubprojects().size(); index < size; index++) {
                    String subproject = model.getSubprojects().get(index);
                    if (!subprojects.add(subproject)) {
                        addViolation(
                                problems,
                                Severity.ERROR,
                                Version.V41,
                                "subprojects.subproject[" + index + "]",
                                null,
                                "specifies duplicate subproject " + subproject,
                                model.getLocation("subprojects"));
                    }
                }
                if (!modules.isEmpty()) {
                    if (subprojects.isEmpty()) {
                        addViolation(
                                problems,
                                Severity.WARNING,
                                Version.V41,
                                "modules",
                                null,
                                "deprecated modules element, use subprojects instead",
                                model.getLocation("modules"));
                    } else {
                        addViolation(
                                problems,
                                Severity.ERROR,
                                Version.V41,
                                "modules",
                                null,
                                "cannot use both modules and subprojects element",
                                model.getLocation("modules"));
                    }
                }
            }

            Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

            boolean isModelVersion41OrMore = !Objects.equals(ModelBuilder.MODEL_VERSION_4_0_0, model.getModelVersion());
            if (isModelVersion41OrMore) {
                validateStringNoExpression("groupId", problems, Severity.FATAL, Version.V41, model.getGroupId(), model);

                validateStringNotEmpty(
                        "artifactId", problems, Severity.FATAL, Version.V20, model.getArtifactId(), model);
                validateStringNoExpression(
                        "artifactId", problems, Severity.FATAL, Version.V20, model.getArtifactId(), model);

                validateVersionNoExpression(
                        "version", problems, Severity.FATAL, Version.V41, model.getVersion(), model);

                if (parent != null) {
                    validateStringNoExpression(
                            "groupId", problems, Severity.FATAL, Version.V41, parent.getGroupId(), model);
                    validateStringNoExpression(
                            "artifactId", problems, Severity.FATAL, Version.V41, parent.getArtifactId(), model);
                    validateVersionNoExpression(
                            "version", problems, Severity.FATAL, Version.V41, parent.getVersion(), model);
                }
            } else {
                validateStringNoExpression(
                        "groupId", problems, Severity.WARNING, Version.V20, model.getGroupId(), model);
                if (parent == null) {
                    validateStringNotEmpty("groupId", problems, Severity.FATAL, Version.V20, model.getGroupId(), model);
                }

                validateStringNoExpression(
                        "artifactId", problems, Severity.WARNING, Version.V20, model.getArtifactId(), model);
                validateStringNotEmpty(
                        "artifactId", problems, Severity.FATAL, Version.V20, model.getArtifactId(), model);

                validateVersionNoExpression(
                        "version", problems, Severity.WARNING, Version.V20, model.getVersion(), model);
                if (parent == null) {
                    validateStringNotEmpty("version", problems, Severity.FATAL, Version.V20, model.getVersion(), model);
                }
            }

            validateStringNoExpression(
                    "packaging", problems, Severity.WARNING, Version.V20, model.getPackaging(), model);

            validate20RawDependencies(
                    problems,
                    model.getDependencies(),
                    "dependencies.dependency.",
                    EMPTY,
                    isModelVersion41OrMore,
                    validationLevel);

            validate20RawDependenciesSelfReferencing(
                    problems, model, model.getDependencies(), "dependencies.dependency");

            if (model.getDependencyManagement() != null) {
                validate20RawDependencies(
                        problems,
                        model.getDependencyManagement().getDependencies(),
                        "dependencyManagement.dependencies.dependency.",
                        EMPTY,
                        isModelVersion41OrMore,
                        validationLevel);
            }

            Build build = model.getBuild();
            if (build != null) {
                validate20RawPlugins(problems, build.getPlugins(), "build.plugins.plugin.", EMPTY, validationLevel);

                PluginManagement mgmt = build.getPluginManagement();
                if (mgmt != null) {
                    validate20RawPlugins(
                            problems,
                            mgmt.getPlugins(),
                            "build.pluginManagement.plugins.plugin.",
                            EMPTY,
                            validationLevel);
                }
            }

            Set<String> profileIds = new HashSet<>();

            for (Profile profile : model.getProfiles()) {
                String prefix = "profiles.profile[" + profile.getId() + "].";

                validateProfileId(prefix, "id", problems, Severity.ERROR, Version.V40, profile.getId(), null, model);

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
                        problems,
                        profile.getDependencies(),
                        prefix,
                        "dependencies.dependency.",
                        isModelVersion41OrMore,
                        validationLevel);

                if (profile.getDependencyManagement() != null) {
                    validate20RawDependencies(
                            problems,
                            profile.getDependencyManagement().getDependencies(),
                            prefix,
                            "dependencyManagement.dependencies.dependency.",
                            isModelVersion41OrMore,
                            validationLevel);
                }

                BuildBase buildBase = profile.getBuild();
                if (buildBase != null) {
                    validate20RawPlugins(problems, buildBase.getPlugins(), prefix, "plugins.plugin.", validationLevel);

                    PluginManagement mgmt = buildBase.getPluginManagement();
                    if (mgmt != null) {
                        validate20RawPlugins(
                                problems,
                                mgmt.getPlugins(),
                                prefix,
                                "pluginManagement.plugins.plugin.",
                                validationLevel);
                    }
                }
            }
        }
    }

    @Override
    public void validateRawModel(Session session, Model model, int validationLevel, ModelProblemCollector problems) {
        // Check that the model version is correctly set wrt the model definition, i.e., that the
        // user does not use an attribute or element that is not available in the modelVersion used.
        String minVersion = new MavenModelVersion().getModelVersion(model);
        if (model.getModelVersion() != null && compareModelVersions(minVersion, model.getModelVersion()) > 0) {
            addViolation(
                    problems,
                    Severity.FATAL,
                    Version.V40,
                    "model",
                    null,
                    "the model contains elements that require a model version of " + minVersion,
                    model);
        }

        Parent parent = model.getParent();

        if (parent != null) {
            validateStringNotEmpty(
                    "parent.groupId", problems, Severity.FATAL, Version.BASE, parent.getGroupId(), parent);

            validateStringNotEmpty(
                    "parent.artifactId", problems, Severity.FATAL, Version.BASE, parent.getArtifactId(), parent);

            validateStringNotEmpty(
                    "parent.version", problems, Severity.FATAL, Version.BASE, parent.getVersion(), parent);

            if (equals(parent.getGroupId(), model.getGroupId())
                    && equals(parent.getArtifactId(), model.getArtifactId())) {
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

        if (validationLevel > VALIDATION_LEVEL_MINIMAL) {
            validateRawRepositories(
                    problems, model.getRepositories(), "repositories.repository.", EMPTY, validationLevel);

            validateRawRepositories(
                    problems,
                    model.getPluginRepositories(),
                    "pluginRepositories.pluginRepository.",
                    EMPTY,
                    validationLevel);

            for (Profile profile : model.getProfiles()) {
                String prefix = "profiles.profile[" + profile.getId() + "].";

                validateRawRepositories(
                        problems, profile.getRepositories(), prefix, "repositories.repository.", validationLevel);

                validateRawRepositories(
                        problems,
                        profile.getPluginRepositories(),
                        prefix,
                        "pluginRepositories.pluginRepository.",
                        validationLevel);
            }

            DistributionManagement distMgmt = model.getDistributionManagement();
            if (distMgmt != null) {
                validateRawRepository(
                        problems, distMgmt.getRepository(), "distributionManagement.repository.", "", true);
                validateRawRepository(
                        problems,
                        distMgmt.getSnapshotRepository(),
                        "distributionManagement.snapshotRepository.",
                        "",
                        true);
            }
        }
    }

    private void validate30RawProfileActivation(ModelProblemCollector problems, Activation activation, String prefix) {
        if (activation == null) {
            return;
        }

        final Deque<ActivationFrame> stk = new LinkedList<>();

        final Supplier<String> pathSupplier = () -> {
            final boolean parallel = false;
            return StreamSupport.stream(((Iterable<ActivationFrame>) stk::descendingIterator).spliterator(), parallel)
                    .map(ActivationFrame::location)
                    .collect(Collectors.joining("."));
        };
        final Supplier<InputLocation> locationSupplier = () -> {
            if (stk.size() < 2) {
                return null;
            }
            Iterator<ActivationFrame> frameIterator = stk.iterator();

            String location = frameIterator.next().location;
            ActivationFrame parent = frameIterator.next();

            return parent.parent
                    .map(parentTracker -> parentTracker.getLocation(location))
                    .orElse(null);
        };
        final UnaryOperator<String> transformer = stringValue -> {
            if (hasProjectExpression(stringValue)) {
                String path = pathSupplier.get();
                Matcher matcher = EXPRESSION_PROJECT_NAME_PATTERN.matcher(stringValue);
                while (matcher.find()) {
                    String propertyName = matcher.group(0);

                    if ((path.startsWith("activation.file.") || path.equals("activation.condition"))
                            && "${project.basedir}".equals(propertyName)) {
                        continue;
                    }
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.V30,
                            prefix + path,
                            null,
                            "Failed to interpolate profile activation property " + stringValue + ": " + propertyName
                                    + " expressions are not supported during profile activation.",
                            locationSupplier.get());
                }
            }
            return stringValue;
        };
        new ActivationWalker(stk, transformer).transformActivation(activation);
    }

    private void validate20RawPlugins(
            ModelProblemCollector problems, List<Plugin> plugins, String prefix, String prefix2, int validationLevel) {
        Severity errOn31 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_1);

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

            if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_4_0 && plugin.getConfiguration() != null) {
                validateXmlNodeRecursively(
                        problems,
                        prefix + prefix2 + "[" + plugin.getKey() + "].configuration",
                        plugin,
                        plugin.getConfiguration());
            }

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
                if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_4_0 && exec.getConfiguration() != null) {
                    validateXmlNodeRecursively(
                            problems,
                            prefix + prefix2 + "[" + plugin.getKey() + "].executions.execution." + exec.getId(),
                            exec,
                            exec.getConfiguration());
                }
            }
        }
    }

    private void validateXmlNodeRecursively(
            ModelProblemCollector problems, String fieldPathPrefix, InputLocationTracker tracker, XmlNode xmlNode) {
        validateXmlNode(problems, fieldPathPrefix, tracker, xmlNode);
        for (XmlNode child : xmlNode.children()) {
            validateXmlNodeRecursively(problems, fieldPathPrefix + "." + xmlNode.name(), tracker, child);
        }
    }

    private void validateXmlNode(
            ModelProblemCollector problems, String fieldPathPrefix, InputLocationTracker tracker, XmlNode xmlNode) {
        String childrenCombinationModeAttribute = xmlNode.attributes()
                .getOrDefault(
                        XmlService.CHILDREN_COMBINATION_MODE_ATTRIBUTE, XmlService.DEFAULT_CHILDREN_COMBINATION_MODE);
        if (!(XmlService.CHILDREN_COMBINATION_APPEND.equals(childrenCombinationModeAttribute)
                || XmlService.CHILDREN_COMBINATION_MERGE.equals(childrenCombinationModeAttribute))) {
            addViolation(
                    problems,
                    Severity.ERROR,
                    Version.V40,
                    fieldPathPrefix + "." + xmlNode.name(),
                    SourceHint.xmlNodeInputLocation(xmlNode),
                    "Unsupported value '" + childrenCombinationModeAttribute + "' for "
                            + XmlService.CHILDREN_COMBINATION_MODE_ATTRIBUTE + " attribute. " + "Valid values are: "
                            + XmlService.CHILDREN_COMBINATION_APPEND + ", and " + XmlService.CHILDREN_COMBINATION_MERGE
                            + " (default is: " + XmlService.DEFAULT_SELF_COMBINATION_MODE + ")",
                    tracker);
        }
        String selfCombinationModeAttribute = xmlNode.attributes()
                .getOrDefault(XmlService.SELF_COMBINATION_MODE_ATTRIBUTE, XmlService.DEFAULT_SELF_COMBINATION_MODE);
        if (!(XmlService.SELF_COMBINATION_OVERRIDE.equals(selfCombinationModeAttribute)
                || XmlService.SELF_COMBINATION_MERGE.equals(selfCombinationModeAttribute)
                || XmlService.SELF_COMBINATION_REMOVE.equals(selfCombinationModeAttribute))) {
            addViolation(
                    problems,
                    Severity.ERROR,
                    Version.V40,
                    fieldPathPrefix + "." + xmlNode.name(),
                    SourceHint.xmlNodeInputLocation(xmlNode),
                    "Unsupported value '" + selfCombinationModeAttribute + "' for "
                            + XmlService.SELF_COMBINATION_MODE_ATTRIBUTE + " attribute. " + "Valid values are: "
                            + XmlService.SELF_COMBINATION_OVERRIDE + ", " + XmlService.SELF_COMBINATION_MERGE + ", and "
                            + XmlService.SELF_COMBINATION_REMOVE
                            + " (default is: " + XmlService.DEFAULT_SELF_COMBINATION_MODE + ")",
                    tracker);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public void validateEffectiveModel(
            Session session, Model model, int validationLevel, ModelProblemCollector problems) {
        validateStringNotEmpty("modelVersion", problems, Severity.ERROR, Version.BASE, model.getModelVersion(), model);

        validateCoordinatesId("groupId", problems, model.getGroupId(), model);

        validateCoordinatesId("artifactId", problems, model.getArtifactId(), model);

        validateStringNotEmpty("packaging", problems, Severity.ERROR, Version.BASE, model.getPackaging(), model);

        if (!model.getModules().isEmpty() || !model.getSubprojects().isEmpty()) {
            if (!"pom".equals(model.getPackaging())) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.BASE,
                        "packaging",
                        null,
                        "with value '" + model.getPackaging() + "' is invalid. Aggregator projects "
                                + "require 'pom' as packaging.",
                        model);
            }

            for (int index = 0, size = model.getModules().size(); index < size; index++) {
                String module = model.getModules().get(index);

                boolean isBlankModule = true;
                if (module != null) {
                    for (int charIndex = 0; charIndex < module.length(); charIndex++) {
                        if (!Character.isWhitespace(module.charAt(charIndex))) {
                            isBlankModule = false;
                        }
                    }
                }

                if (isBlankModule) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.BASE,
                            "modules.module[" + index + "]",
                            null,
                            "has been specified without a path to the project directory.",
                            model.getLocation("modules"));
                }
            }

            for (int index = 0, size = model.getSubprojects().size(); index < size; index++) {
                String subproject = model.getSubprojects().get(index);

                boolean isBlankSubproject = true;
                if (subproject != null) {
                    for (int charIndex = 0; charIndex < subproject.length(); charIndex++) {
                        if (!Character.isWhitespace(subproject.charAt(charIndex))) {
                            isBlankSubproject = false;
                        }
                    }
                }

                if (isBlankSubproject) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.BASE,
                            "subprojects.subproject[" + index + "]",
                            null,
                            "has been specified without a path to the project directory.",
                            model.getLocation("subprojects"));
                }
            }
        }

        validateStringNotEmpty("version", problems, Severity.ERROR, Version.BASE, model.getVersion(), model);

        Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

        validateEffectiveDependencies(session, problems, model, model.getDependencies(), false, validationLevel);

        DependencyManagement mgmt = model.getDependencyManagement();
        if (mgmt != null) {
            validateEffectiveDependencies(session, problems, model, mgmt.getDependencies(), true, validationLevel);
        }

        if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_2_0) {
            Severity errOn31 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_1);

            validateBannedCharacters(
                    EMPTY,
                    "version",
                    problems,
                    errOn31,
                    Version.V20,
                    model.getVersion(),
                    null,
                    model,
                    ILLEGAL_VERSION_CHARS);
            validate20ProperSnapshotVersion("version", problems, errOn31, Version.V20, model.getVersion(), null, model);
            if (hasExpression(model.getVersion())) {
                Severity versionExpressionSeverity = Severity.ERROR;
                if (model.getProperties() != null
                        && Boolean.parseBoolean(
                                model.getProperties().get(BUILD_ALLOW_EXPRESSION_IN_EFFECTIVE_PROJECT_VERSION))) {
                    versionExpressionSeverity = Severity.WARNING;
                }
                addViolation(
                        problems,
                        versionExpressionSeverity,
                        Version.V20,
                        "version",
                        null,
                        "must be a constant version but is '" + model.getVersion() + "'.",
                        model);
            }

            Build build = model.getBuild();
            if (build != null) {
                for (Plugin plugin : build.getPlugins()) {
                    validateStringNotEmpty(
                            "build.plugins.plugin.artifactId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            plugin.getArtifactId(),
                            plugin);

                    validateStringNotEmpty(
                            "build.plugins.plugin.groupId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            plugin.getGroupId(),
                            plugin);

                    validate20PluginVersion(
                            "build.plugins.plugin.version",
                            problems,
                            plugin.getVersion(),
                            SourceHint.pluginKey(plugin),
                            plugin,
                            validationLevel);

                    validateBoolean(
                            "build.plugins.plugin.inherited",
                            EMPTY,
                            problems,
                            errOn30,
                            Version.V20,
                            plugin.getInherited(),
                            SourceHint.pluginKey(plugin),
                            plugin);

                    validateBoolean(
                            "build.plugins.plugin.extensions",
                            EMPTY,
                            problems,
                            errOn30,
                            Version.V20,
                            plugin.getExtensions(),
                            SourceHint.pluginKey(plugin),
                            plugin);

                    validate20EffectivePluginDependencies(problems, plugin, validationLevel);
                }

                validate20RawResources(problems, build.getResources(), "build.resources.resource.", validationLevel);

                validate20RawResources(
                        problems, build.getTestResources(), "build.testResources.testResource.", validationLevel);
            }

            Reporting reporting = model.getReporting();
            if (reporting != null) {
                for (ReportPlugin reportPlugin : reporting.getPlugins()) {
                    validateStringNotEmpty(
                            "reporting.plugins.plugin.artifactId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            reportPlugin.getArtifactId(),
                            reportPlugin);

                    validateStringNotEmpty(
                            "reporting.plugins.plugin.groupId",
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            reportPlugin.getGroupId(),
                            reportPlugin);
                }
            }

            for (Repository repository : model.getRepositories()) {
                validate20EffectiveRepository(problems, repository, "repositories.repository.", validationLevel);
            }

            for (Repository repository : model.getPluginRepositories()) {
                validate20EffectiveRepository(
                        problems, repository, "pluginRepositories.pluginRepository.", validationLevel);
            }

            DistributionManagement distMgmt = model.getDistributionManagement();
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
                        problems, distMgmt.getRepository(), "distributionManagement.repository.", validationLevel);
                validate20EffectiveRepository(
                        problems,
                        distMgmt.getSnapshotRepository(),
                        "distributionManagement.snapshotRepository.",
                        validationLevel);
            }
        }
    }

    private void validate20RawDependencies(
            ModelProblemCollector problems,
            List<Dependency> dependencies,
            String prefix,
            String prefix2,
            boolean is41OrBeyond,
            int validationLevel) {
        Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);
        Severity errOn31 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_1);

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
                            SourceHint.dependencyManagementKey(dependency),
                            "must be 'pom' to import the managed dependencies.",
                            dependency);
                } else if (!is41OrBeyond
                        && dependency.getClassifier() != null
                        && !dependency.getClassifier().isEmpty()) {
                    addViolation(
                            problems,
                            errOn30,
                            Version.V20,
                            prefix + prefix2 + "classifier",
                            SourceHint.dependencyManagementKey(dependency),
                            "must be empty, imported POM cannot have a classifier.",
                            dependency);
                }
            } else if ("system".equals(dependency.getScope())) {

                if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_3_1) {
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.V31,
                            prefix + prefix2 + "scope",
                            SourceHint.dependencyManagementKey(dependency),
                            "declares usage of deprecated 'system' scope ",
                            dependency);
                }

                String sysPath = dependency.getSystemPath();
                if (sysPath != null && !sysPath.isEmpty()) {
                    if (!hasExpression(sysPath)) {
                        addViolation(
                                problems,
                                Severity.WARNING,
                                Version.V20,
                                prefix + prefix2 + "systemPath",
                                SourceHint.dependencyManagementKey(dependency),
                                "should use a variable instead of a hard-coded path " + sysPath,
                                dependency);
                    } else if (sysPath.contains("${basedir}") || sysPath.contains("${project.basedir}")) {
                        addViolation(
                                problems,
                                Severity.WARNING,
                                Version.V20,
                                prefix + prefix2 + "systemPath",
                                SourceHint.dependencyManagementKey(dependency),
                                "should not point at files within the project directory, " + sysPath
                                        + " will be unresolvable by dependent projects",
                                dependency);
                    }
                }
            }

            // MNG-8750: New dependency scopes are only supported starting with modelVersion 4.1.0
            // When using modelVersion 4.0.0, fail validation if one of the new scopes is present
            if (!is41OrBeyond) {
                String scope = dependency.getScope();
                if (DependencyScope.COMPILE_ONLY.id().equals(scope)
                        || DependencyScope.TEST_ONLY.id().equals(scope)
                        || DependencyScope.TEST_RUNTIME.id().equals(scope)) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.V20,
                            prefix + prefix2 + "scope",
                            SourceHint.dependencyManagementKey(dependency),
                            "scope '" + scope + "' is not supported with modelVersion 4.0.0; "
                                    + "use modelVersion 4.1.0 or remove this scope.",
                            dependency);
                }
            }

            if (equals("LATEST", dependency.getVersion()) || equals("RELEASE", dependency.getVersion())) {
                addViolation(
                        problems,
                        Severity.WARNING,
                        Version.BASE,
                        prefix + prefix2 + "version",
                        SourceHint.dependencyManagementKey(dependency),
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
            ModelProblemCollector problems, Model model, List<Dependency> dependencies, String prefix) {
        // We only check for groupId/artifactId/version/classifier cause if there is another
        // module with the same groupId/artifactId/version/classifier this will fail the build
        // earlier like "Project '...' is duplicated in the reactor.
        // So it is sufficient to check only groupId/artifactId/version/classifier and not the
        // packaging type.
        for (Dependency dependency : dependencies) {
            String key = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion()
                    + (dependency.getClassifier() != null ? ":" + dependency.getClassifier() : EMPTY);
            String modelKey = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
            if (key.equals(modelKey)) {
                // This means a module which is build has a dependency which has the same
                // groupId, artifactId, version and classifier coordinates. This is in consequence
                // a self reference or in other words a circular reference which can not being resolved.
                addViolation(
                        problems,
                        Severity.FATAL,
                        Version.V31,
                        prefix + "[" + key + "]",
                        SourceHint.gav(key),
                        "is referencing itself.",
                        dependency);
            }
        }
    }

    private void validateEffectiveDependencies(
            Session session,
            ModelProblemCollector problems,
            Model model,
            List<Dependency> dependencies,
            boolean management,
            int validationLevel) {
        Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

        String prefix = management ? "dependencyManagement.dependencies.dependency." : "dependencies.dependency.";

        for (Dependency dependency : dependencies) {
            validateEffectiveDependency(problems, dependency, management, prefix, validationLevel);

            if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_2_0) {
                validateBoolean(
                        prefix,
                        "optional",
                        problems,
                        errOn30,
                        Version.V20,
                        dependency.getOptional(),
                        SourceHint.dependencyManagementKey(dependency),
                        dependency);

                if (!management) {
                    validateVersion(
                            prefix,
                            "version",
                            problems,
                            errOn30,
                            Version.V20,
                            dependency.getVersion(),
                            SourceHint.dependencyManagementKey(dependency),
                            dependency);

                    /*
                     * Extensions like Flex Mojos use custom scopes like "merged", "internal", "external", etc. In
                     * order to not break backward-compat with those, only warn but don't error out.
                     */
                    ScopeManager scopeManager =
                            InternalSession.from(session).getSession().getScopeManager();
                    validateDependencyScope(
                            prefix,
                            "scope",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            dependency.getScope(),
                            SourceHint.dependencyManagementKey(dependency),
                            dependency,
                            scopeManager.getDependencyScopeUniverse().stream()
                                    .map(org.eclipse.aether.scope.DependencyScope::getId)
                                    .distinct()
                                    .toArray(String[]::new),
                            false);

                    validateEffectiveModelAgainstDependency(prefix, problems, model, dependency);
                } else {
                    ScopeManager scopeManager =
                            InternalSession.from(session).getSession().getScopeManager();
                    Set<String> scopes = scopeManager.getDependencyScopeUniverse().stream()
                            .map(org.eclipse.aether.scope.DependencyScope::getId)
                            .collect(Collectors.toCollection(HashSet::new));
                    scopes.add("import");
                    validateDependencyScope(
                            prefix,
                            "scope",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            dependency.getScope(),
                            SourceHint.dependencyManagementKey(dependency),
                            dependency,
                            scopes.toArray(new String[0]),
                            true);
                }
            }
        }
    }

    private void validateEffectiveModelAgainstDependency(
            String prefix, ModelProblemCollector problems, Model model, Dependency dependency) {
        String key = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion()
                + (dependency.getClassifier() != null ? ":" + dependency.getClassifier() : EMPTY);
        String modelKey = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
        if (key.equals(modelKey)) {
            // This means a module which is build has a dependency which has the same
            // groupId, artifactId, version and classifier coordinates. This is in consequence
            // a self reference or in other words a circular reference which can not being resolved.
            addViolation(
                    problems,
                    Severity.FATAL,
                    Version.V31,
                    prefix + "[" + key + "]",
                    SourceHint.gav(key),
                    "is referencing itself.",
                    dependency);
        }
    }

    private void validate20EffectivePluginDependencies(
            ModelProblemCollector problems, Plugin plugin, int validationLevel) {
        List<Dependency> dependencies = plugin.getDependencies();

        if (!dependencies.isEmpty()) {
            String prefix = "build.plugins.plugin[" + plugin.getKey() + "].dependencies.dependency.";

            Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

            for (Dependency dependency : dependencies) {
                validateEffectiveDependency(problems, dependency, false, prefix, validationLevel);

                validateVersion(
                        prefix,
                        "version",
                        problems,
                        errOn30,
                        Version.BASE,
                        dependency.getVersion(),
                        SourceHint.dependencyManagementKey(dependency),
                        dependency);

                validateEnum(
                        prefix,
                        "scope",
                        problems,
                        errOn30,
                        Version.BASE,
                        dependency.getScope(),
                        SourceHint.dependencyManagementKey(dependency),
                        dependency,
                        "compile",
                        "runtime",
                        "system");
            }
        }
    }

    private void validateEffectiveDependency(
            ModelProblemCollector problems,
            Dependency dependency,
            boolean management,
            String prefix,
            int validationLevel) {
        validateCoordinatesId(
                prefix,
                "artifactId",
                problems,
                Severity.ERROR,
                Version.BASE,
                dependency.getArtifactId(),
                SourceHint.dependencyManagementKey(dependency),
                dependency);

        validateCoordinatesId(
                prefix,
                "groupId",
                problems,
                Severity.ERROR,
                Version.BASE,
                dependency.getGroupId(),
                SourceHint.dependencyManagementKey(dependency),
                dependency);

        if (!management) {
            validateStringNotEmpty(
                    prefix,
                    "type",
                    problems,
                    Severity.ERROR,
                    Version.BASE,
                    dependency.getType(),
                    SourceHint.dependencyManagementKey(dependency),
                    dependency);

            validateDependencyVersion(problems, dependency, prefix);
        }

        if ("system".equals(dependency.getScope())) {
            String systemPath = dependency.getSystemPath();

            if (systemPath == null || systemPath.isEmpty()) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.BASE,
                        prefix + "systemPath",
                        SourceHint.dependencyManagementKey(dependency),
                        "is missing.",
                        dependency);
            } else {
                File sysFile = new File(systemPath);
                if (!sysFile.isAbsolute()) {
                    addViolation(
                            problems,
                            Severity.ERROR,
                            Version.BASE,
                            prefix + "systemPath",
                            SourceHint.dependencyManagementKey(dependency),
                            "must specify an absolute path but is " + systemPath,
                            dependency);
                } else if (!sysFile.isFile()) {
                    String msg = "refers to a non-existing file " + sysFile.getAbsolutePath() + ".";
                    addViolation(
                            problems,
                            Severity.WARNING,
                            Version.BASE,
                            prefix + "systemPath",
                            SourceHint.dependencyManagementKey(dependency),
                            msg,
                            dependency);
                }
            }
        } else if (dependency.getSystemPath() != null
                && !dependency.getSystemPath().isEmpty()) {
            addViolation(
                    problems,
                    Severity.ERROR,
                    Version.BASE,
                    prefix + "systemPath",
                    SourceHint.dependencyManagementKey(dependency),
                    "must be omitted. This field may only be specified for a dependency with system scope.",
                    dependency);
        }

        if (validationLevel >= ModelValidator.VALIDATION_LEVEL_MAVEN_2_0) {
            for (Exclusion exclusion : dependency.getExclusions()) {
                if (validationLevel < ModelValidator.VALIDATION_LEVEL_MAVEN_3_0) {
                    validateCoordinatesId(
                            prefix,
                            "exclusions.exclusion.groupId",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            exclusion.getGroupId(),
                            SourceHint.dependencyManagementKey(dependency),
                            exclusion);

                    validateCoordinatesId(
                            prefix,
                            "exclusions.exclusion.artifactId",
                            problems,
                            Severity.WARNING,
                            Version.V20,
                            exclusion.getArtifactId(),
                            SourceHint.dependencyManagementKey(dependency),
                            exclusion);
                } else {
                    validateCoordinatesIdWithWildcards(
                            prefix,
                            "exclusions.exclusion.groupId",
                            problems,
                            Severity.WARNING,
                            Version.V30,
                            exclusion.getGroupId(),
                            SourceHint.dependencyManagementKey(dependency),
                            exclusion);

                    validateCoordinatesIdWithWildcards(
                            prefix,
                            "exclusions.exclusion.artifactId",
                            problems,
                            Severity.WARNING,
                            Version.V30,
                            exclusion.getArtifactId(),
                            SourceHint.dependencyManagementKey(dependency),
                            exclusion);
                }
            }
        }
    }

    /**
     * @since 3.2.4
     */
    protected void validateDependencyVersion(ModelProblemCollector problems, Dependency dependency, String prefix) {
        validateStringNotEmpty(
                prefix,
                "version",
                problems,
                Severity.ERROR,
                Version.BASE,
                dependency.getVersion(),
                SourceHint.dependencyManagementKey(dependency),
                dependency);
    }

    private void validateRawRepositories(
            ModelProblemCollector problems,
            List<Repository> repositories,
            String prefix,
            String prefix2,
            int validationLevel) {
        Map<String, Repository> index = new HashMap<>();

        for (Repository repository : repositories) {
            validateRawRepository(problems, repository, prefix, prefix2, false);

            String key = repository.getId();

            Repository existing = index.get(key);

            if (existing != null) {
                Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

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

    private void validateRawRepository(
            ModelProblemCollector problems,
            Repository repository,
            String prefix,
            String prefix2,
            boolean allowEmptyUrl) {
        if (repository == null) {
            return;
        }
        if (validateStringNotEmpty(
                prefix, prefix2, "id", problems, Severity.ERROR, Version.V20, repository.getId(), null, repository)) {
            // Check for uninterpolated expressions in ID - these should have been interpolated by now
            Matcher matcher = EXPRESSION_NAME_PATTERN.matcher(repository.getId());
            if (matcher.find()) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.V40,
                        prefix + prefix2 + "[" + repository.getId() + "].id",
                        null,
                        "contains an uninterpolated expression.",
                        repository);
            }
        }

        if (!allowEmptyUrl
                && validateStringNotEmpty(
                        prefix,
                        prefix2,
                        "[" + repository.getId() + "].url",
                        problems,
                        Severity.ERROR,
                        Version.V20,
                        repository.getUrl(),
                        null,
                        repository)) {
            // Check for uninterpolated expressions in URL - these should have been interpolated by now
            Matcher matcher = EXPRESSION_NAME_PATTERN.matcher(repository.getUrl());
            if (matcher.find()) {
                addViolation(
                        problems,
                        Severity.ERROR,
                        Version.V40,
                        prefix + prefix2 + "[" + repository.getId() + "].url",
                        null,
                        "contains an uninterpolated expression.",
                        repository);
            }
        }
    }

    private void validate20EffectiveRepository(
            ModelProblemCollector problems, Repository repository, String prefix, int validationLevel) {
        if (repository != null) {
            Severity errOn31 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_1);

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
                        SourceHint.repoId(repository),
                        "uses the unsupported value 'legacy', artifact resolution might fail.",
                        repository);
            }
        }
    }

    private void validate20RawResources(
            ModelProblemCollector problems, List<Resource> resources, String prefix, int validationLevel) {
        Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

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
                    SourceHint.resourceDirectory(resource),
                    resource);
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    private boolean validateCoordinatesId(
            String fieldName, ModelProblemCollector problems, String id, InputLocationTracker tracker) {
        return validateCoordinatesId(EMPTY, fieldName, problems, Severity.ERROR, Version.BASE, id, null, tracker);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateCoordinatesId(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String id,
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (id != null && validCoordinatesIds.contains(id)) {
            return true;
        }
        if (!validateStringNotEmpty(prefix, fieldName, problems, severity, version, id, sourceHint, tracker)) {
            return false;
        } else {
            if (!isValidCoordinatesId(id)) {
                addViolation(
                        problems,
                        severity,
                        version,
                        prefix + fieldName,
                        sourceHint,
                        "with value '" + id + "' does not match a valid coordinate id pattern.",
                        tracker);
                return false;
            }
            validCoordinatesIds.add(id);
            return true;
        }
    }

    private boolean isValidCoordinatesId(String id) {
        for (int index = 0; index < id.length(); index++) {
            char character = id.charAt(index);
            if (!isValidCoordinatesIdCharacter(character)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidCoordinatesIdCharacter(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '_'
                || character == '.';
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateProfileId(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String id,
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (validProfileIds.contains(id)) {
            return true;
        }
        if (!validateStringNotEmpty(prefix, fieldName, problems, severity, version, id, sourceHint, tracker)) {
            return false;
        } else {
            if (!isValidProfileId(id)) {
                addViolation(
                        problems,
                        severity,
                        version,
                        prefix + fieldName,
                        sourceHint,
                        "with value '" + id + "' does not match a valid profile id pattern.",
                        tracker);
                return false;
            }
            validProfileIds.add(id);
            return true;
        }
    }

    private boolean isValidProfileId(String id) {
        return switch (id.charAt(0)) { // avoid first character that has special CLI meaning in "mvn -P xxx"
            // +: activate
            // -, !: deactivate
            // ?: optional
            case '+', '-', '!', '?' -> false;
            default -> true;
        };
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateCoordinatesIdWithWildcards(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String id,
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (!validateStringNotEmpty(prefix, fieldName, problems, severity, version, id, sourceHint, tracker)) {
            return false;
        } else {
            if (!isValidCoordinatesIdWithWildCards(id)) {
                addViolation(
                        problems,
                        severity,
                        version,
                        prefix + fieldName,
                        sourceHint,
                        "with value '" + id + "' does not match a valid coordinate id pattern.",
                        tracker);
                return false;
            }
            return true;
        }
    }

    private boolean isValidCoordinatesIdWithWildCards(String id) {
        for (int index = 0; index < id.length(); index++) {
            char character = id.charAt(index);
            if (!isValidCoordinatesIdWithWildCardCharacter(character)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidCoordinatesIdWithWildCardCharacter(char character) {
        return isValidCoordinatesIdCharacter(character) || character == '?' || character == '*';
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

        Matcher matcher = EXPRESSION_NAME_PATTERN.matcher(string.trim());
        if (matcher.find()) {
            addViolation(
                    problems,
                    severity,
                    version,
                    fieldName,
                    null,
                    "contains an expression but should be a constant.",
                    tracker);
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
            @Nullable SourceHint sourceHint,
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
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (!validateNotNull(prefix, fieldName, problems, severity, version, string, sourceHint, tracker)) {
            return false;
        }

        if (!string.isEmpty()) {
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
            @Nullable SourceHint sourceHint,
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
            @Nullable SourceHint sourceHint,
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
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (string == null || string.isEmpty()) {
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
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker,
            String... validValues) {
        if (string == null || string.isEmpty()) {
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
    private boolean validateDependencyScope(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String scope,
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker,
            String[] validScopes,
            boolean isDependencyManagement) {
        if (scope == null || scope.isEmpty()) {
            return true;
        }

        List<String> values = Arrays.asList(validScopes);

        if (values.contains(scope)) {
            return true;
        }

        // Provide a more helpful error message for the 'import' scope
        if ("import".equals(scope) && !isDependencyManagement) {
            addViolation(
                    problems,
                    severity,
                    version,
                    prefix + fieldName,
                    sourceHint,
                    "has scope 'import'. The 'import' scope is only valid in <dependencyManagement> sections.",
                    tracker);
        } else {
            addViolation(
                    problems,
                    severity,
                    version,
                    prefix + fieldName,
                    sourceHint,
                    "must be one of " + values + " but is '" + scope + "'.",
                    tracker);
        }

        return false;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateModelVersion(
            Session session,
            ModelProblemCollector problems,
            String requestedModel,
            InputLocationTracker tracker,
            List<String> validVersions) {
        if (requestedModel == null || requestedModel.isEmpty()) {
            return true;
        }

        if (validVersions.contains(requestedModel)) {
            return true;
        }

        boolean newerThanAll = true;
        boolean olderThanAll = true;
        for (String validValue : validVersions) {
            final int comparison = compareModelVersions(validValue, requestedModel);
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
                    requestedModel + "' is not supported by this Maven version ("
                            + getMavenVersionString(session)
                            + "). Supported modelVersions are: " + validVersions
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
                    requestedModel + "' is not supported by this Maven version ("
                            + getMavenVersionString(session)
                            + "). Supported modelVersions are: " + validVersions
                            + ". Building this project requires an older version of Maven.",
                    tracker);

        } else {
            addViolation(
                    problems,
                    Severity.ERROR,
                    Version.V20,
                    "modelVersion",
                    null,
                    "must be one of " + validVersions + " but is '" + requestedModel + "'.",
                    tracker);
        }

        return false;
    }

    private String getMavenVersionString(Session session) {
        try {
            return session.getMavenVersion().toString();
        } catch (Exception exception) {
            // Fallback for test contexts where RuntimeInformation might not be available
            return "unknown";
        }
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
        String[] firstSegments = first.split("\\.");
        String[] secondSegments = second.split("\\.");
        for (int index = 0; index < Math.max(firstSegments.length, secondSegments.length); index++) {
            int result = asLong(index, firstSegments).compareTo(asLong(index, secondSegments));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static Long asLong(int index, String[] segments) {
        try {
            return Long.valueOf(index < segments.length ? segments[index] : "0");
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private boolean validateBannedCharacters(
            String prefix,
            String fieldName,
            ModelProblemCollector problems,
            Severity severity,
            Version version,
            String string,
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker,
            String banned) {
        if (string != null) {
            for (int index = string.length() - 1; index >= 0; index--) {
                if (banned.indexOf(string.charAt(index)) >= 0) {
                    addViolation(
                            problems,
                            severity,
                            version,
                            prefix + fieldName,
                            sourceHint,
                            "must not contain any of these characters " + banned + " but found " + string.charAt(index),
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
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (string == null || string.isEmpty()) {
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
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker) {
        if (string == null || string.isEmpty()) {
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
            @Nullable SourceHint sourceHint,
            InputLocationTracker tracker,
            int validationLevel) {
        if (string == null) {
            addViolation(
                    problems,
                    Severity.WARNING,
                    ModelProblem.Version.V20,
                    fieldName,
                    sourceHint,
                    " is missing.",
                    tracker);
            return false;
        }

        Severity errOn30 = getSeverity(validationLevel, ModelValidator.VALIDATION_LEVEL_MAVEN_3_0);

        if (!validateVersion(EMPTY, fieldName, problems, errOn30, Version.V20, string, sourceHint, tracker)) {
            return false;
        }

        if (string.isEmpty() || "RELEASE".equals(string) || "LATEST".equals(string)) {
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
            @Nullable SourceHint sourceHint,
            String message,
            InputLocationTracker tracker) {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append('\'').append(fieldName).append('\'');

        if (sourceHint != null) {
            String hint = sourceHint.get();
            if (hint != null) {
                buffer.append(" for ").append(hint);
            }
        }

        buffer.append(' ').append(message);

        problems.add(severity, version, buffer.toString(), getLocation(fieldName, tracker));
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
                    } catch (NumberFormatException exception) {
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
        String c1 = s1 == null ? "" : s1.trim();
        String c2 = s2 == null ? "" : s2.trim();
        return c1.equals(c2);
    }

    private static Severity getSeverity(int validationLevel, int errorThreshold) {
        if (validationLevel < errorThreshold) {
            return Severity.WARNING;
        } else {
            return Severity.ERROR;
        }
    }

    private interface SourceHint extends Supplier<String> {
        static SourceHint xmlNodeInputLocation(XmlNode xmlNode) {
            return () ->
                    xmlNode.inputLocation() != null ? xmlNode.inputLocation().toString() : null;
        }

        static SourceHint gav(String gav) {
            return () -> gav; // GAV
        }

        static SourceHint dependencyManagementKey(Dependency dependency) {
            return () -> {
                String hint;
                if (dependency.getClassifier() == null
                        || dependency.getClassifier().isEmpty()) {
                    hint = "groupId=" + valueToValueString(dependency.getGroupId())
                            + ", artifactId=" + valueToValueString(dependency.getArtifactId())
                            + ", type=" + valueToValueString(dependency.getType());
                } else {
                    hint = "groupId=" + valueToValueString(dependency.getGroupId())
                            + ", artifactId=" + valueToValueString(dependency.getArtifactId())
                            + ", classifier=" + valueToValueString(dependency.getClassifier())
                            + ", type=" + valueToValueString(dependency.getType());
                }
                return hint;
            };
        }

        private static String valueToValueString(String value) {
            return value == null ? "" : "'" + value + "'";
        }

        static SourceHint pluginKey(Plugin plugin) {
            return plugin::getKey;
        }

        static SourceHint repoId(Repository repository) {
            return repository::getId;
        }

        @Nullable
        static SourceHint resourceDirectory(Resource resource) {
            return resource::getDirectory;
        }
    }
}
