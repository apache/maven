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

import java.io.InputStream;
import java.io.Serial;
import java.util.List;
import java.util.Properties;
import java.util.function.UnaryOperator;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
@Deprecated
class DefaultModelValidatorTest {

    private ModelValidator validator;

    private Model read(String pom) throws Exception {
        String resource = "/poms/validation/" + pom;
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            assertThat(is).as("missing resource: " + resource).isNotNull();
            return new Model(new MavenStaxReader().read(is));
        }
    }

    private SimpleProblemCollector validate(String pom) throws Exception {
        return validateEffective(pom, UnaryOperator.identity());
    }

    private SimpleProblemCollector validateRaw(String pom) throws Exception {
        return validateRaw(pom, UnaryOperator.identity());
    }

    @SuppressWarnings("SameParameterValue")
    private SimpleProblemCollector validateEffective(String pom, int level) throws Exception {
        return validateEffective(pom, mbr -> mbr.setValidationLevel(level));
    }

    private SimpleProblemCollector validateEffective(String pom, UnaryOperator<ModelBuildingRequest> requestConfigurer)
            throws Exception {
        Model model = read(pom);

        SimpleProblemCollector problems = new SimpleProblemCollector(model);

        validator.validateEffectiveModel(model, requestConfigurer.apply(new DefaultModelBuildingRequest()), problems);

        return problems;
    }

    @SuppressWarnings("SameParameterValue")
    private SimpleProblemCollector validateRaw(String pom, int level) throws Exception {
        return validateRaw(pom, mbr -> mbr.setValidationLevel(level));
    }

    private SimpleProblemCollector validateRaw(String pom, UnaryOperator<ModelBuildingRequest> requestConfigurer)
            throws Exception {
        Model model = read(pom);

        SimpleProblemCollector problems = new SimpleProblemCollector(model);

        ModelBuildingRequest request = requestConfigurer.apply(new DefaultModelBuildingRequest());

        validator.validateRawModel(model, request, problems);

        return problems;
    }

    private void assertContains(String msg, String substring) {
        assertThat(msg.contains(substring)).as("\"" + substring + "\" was not found in: " + msg).isTrue();
    }

    @BeforeEach
    void setUp() throws Exception {
        validator = new DefaultModelValidator(new DefaultModelVersionProcessor());
    }

    @AfterEach
    void tearDown() throws Exception {
        this.validator = null;
    }

    private void assertViolations(SimpleProblemCollector result, int fatals, int errors, int warnings) {
        assertThat(result.getFatals().size()).as(String.valueOf(result.getFatals())).isEqualTo(fatals);
        assertThat(result.getErrors().size()).as(String.valueOf(result.getErrors())).isEqualTo(errors);
        assertThat(result.getWarnings().size()).as(String.valueOf(result.getWarnings())).isEqualTo(warnings);
    }

    @Test
    void missingModelVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-modelVersion-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'modelVersion' is missing.");
    }

    @Test
    void badModelVersion() throws Exception {
        SimpleProblemCollector result =
                validateRaw("bad-modelVersion.xml", ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertViolations(result, 1, 0, 0);

        assertThat(result.getFatals().get(0).contains("modelVersion")).isTrue();
    }

    @Test
    void modelVersionMessage() throws Exception {
        SimpleProblemCollector result =
                validateRaw("modelVersion-4_0.xml", ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("'modelVersion' must be one of")).isTrue();
    }

    @Test
    void missingArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'artifactId' is missing.");
    }

    @Test
    void missingGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-groupId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'groupId' is missing.");
    }

    @Test
    void invalidCoordinateIds() throws Exception {
        SimpleProblemCollector result = validate("invalid-coordinate-ids-pom.xml");

        assertViolations(result, 0, 2, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'groupId' with value 'o/a/m' does not match a valid id pattern.");

        assertThat(result.getErrors().get(1)).isEqualTo("'artifactId' with value 'm$-do$' does not match a valid id pattern.");
    }

    @Test
    void missingType() throws Exception {
        SimpleProblemCollector result = validate("missing-type-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'packaging' is missing.");
    }

    @Test
    void missingVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-version-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'version' is missing.");
    }

    @Test
    void invalidAggregatorPackaging() throws Exception {
        SimpleProblemCollector result = validate("invalid-aggregator-packaging-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("Aggregator projects require 'pom' as packaging.")).isTrue();
    }

    @Test
    void missingDependencyArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors()
                .get(0)
                .contains("'dependencies.dependency.artifactId' for groupId:null:jar is missing")).isTrue();
    }

    @Test
    void missingDependencyGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-groupId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors()
                .get(0)
                .contains("'dependencies.dependency.groupId' for null:artifactId:jar is missing")).isTrue();
    }

    @Test
    void missingDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-version-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors()
                .get(0)
                .contains("'dependencies.dependency.version' for groupId:artifactId:jar is missing")).isTrue();
    }

    @Test
    void missingDependencyManagementArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-mgmt-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors()
                .get(0)
                .contains("'dependencyManagement.dependencies.dependency.artifactId' for groupId:null:jar is missing")).isTrue();
    }

    @Test
    void missingDependencyManagementGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-mgmt-groupId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors()
                .get(0)
                .contains("'dependencyManagement.dependencies.dependency.groupId' for null:artifactId:jar is missing")).isTrue();
    }

    @Test
    void missingAll() throws Exception {
        SimpleProblemCollector result = validate("missing-1-pom.xml");

        assertViolations(result, 0, 4, 0);

        List<String> messages = result.getErrors();

        assertThat(messages.contains("'modelVersion' is missing.")).isTrue();
        assertThat(messages.contains("'groupId' is missing.")).isTrue();
        assertThat(messages.contains("'artifactId' is missing.")).isTrue();
        assertThat(messages.contains("'version' is missing.")).isTrue();
        // type is inherited from the super pom
    }

    @Test
    void missingPluginArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'build.plugins.plugin.artifactId' is missing.");
    }

    @Test
    void emptyPluginVersion() throws Exception {
        SimpleProblemCollector result = validate("empty-plugin-version.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'build.plugins.plugin.version' for org.apache.maven.plugins:maven-it-plugin"
                + " must be a valid version but is ''.");
    }

    @Test
    void missingRepositoryId() throws Exception {
        SimpleProblemCollector result =
                validateRaw("missing-repository-id-pom.xml", ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertViolations(result, 0, 4, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'repositories.repository.id' is missing.");

        assertThat(result.getErrors().get(1)).isEqualTo("'repositories.repository.[null].url' is missing.");

        assertThat(result.getErrors().get(2)).isEqualTo("'pluginRepositories.pluginRepository.id' is missing.");

        assertThat(result.getErrors().get(3)).isEqualTo("'pluginRepositories.pluginRepository.[null].url' is missing.");
    }

    @Test
    void missingResourceDirectory() throws Exception {
        SimpleProblemCollector result = validate("missing-resource-directory-pom.xml");

        assertViolations(result, 0, 2, 0);

        assertThat(result.getErrors().get(0)).isEqualTo("'build.resources.resource.directory' is missing.");

        assertThat(result.getErrors().get(1)).isEqualTo("'build.testResources.testResource.directory' is missing.");
    }

    @Test
    void badPluginDependencyScope() throws Exception {
        SimpleProblemCollector result = validate("bad-plugin-dependency-scope.xml");

        assertViolations(result, 0, 3, 0);

        assertThat(result.getErrors().get(0).contains("test:d")).isTrue();

        assertThat(result.getErrors().get(1).contains("test:e")).isTrue();

        assertThat(result.getErrors().get(2).contains("test:f")).isTrue();
    }

    @Test
    void badDependencyScope() throws Exception {
        SimpleProblemCollector result = validate("bad-dependency-scope.xml");

        assertViolations(result, 0, 0, 2);

        assertThat(result.getWarnings().get(0).contains("test:f")).isTrue();

        assertThat(result.getWarnings().get(1).contains("test:g")).isTrue();
    }

    @Test
    void badDependencyManagementScope() throws Exception {
        SimpleProblemCollector result = validate("bad-dependency-management-scope.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(result.getWarnings().get(0), "test:g");
    }

    @Test
    void badDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-dependency-version.xml");

        assertViolations(result, 0, 2, 0);

        assertContains(
                result.getErrors().get(0), "'dependencies.dependency.version' for test:b:jar must be a valid version");
        assertContains(
                result.getErrors().get(1),
                "'dependencies.dependency.version' for test:c:jar must not contain any of these characters");
    }

    @Test
    void duplicateModule() throws Exception {
        SimpleProblemCollector result = validate("duplicate-module.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("child")).isTrue();
    }

    @Test
    void duplicateProfileId() throws Exception {
        SimpleProblemCollector result = validateRaw("duplicate-profile-id.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("non-unique-id")).isTrue();
    }

    @Test
    void badPluginVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-plugin-version.xml");

        assertViolations(result, 0, 4, 0);

        assertContains(
                result.getErrors().get(0), "'build.plugins.plugin.version' for test:mip must be a valid version");
        assertContains(
                result.getErrors().get(1), "'build.plugins.plugin.version' for test:rmv must be a valid version");
        assertContains(
                result.getErrors().get(2), "'build.plugins.plugin.version' for test:lmv must be a valid version");
        assertContains(
                result.getErrors().get(3),
                "'build.plugins.plugin.version' for test:ifsc must not contain any of these characters");
    }

    @Test
    void distributionManagementStatus() throws Exception {
        SimpleProblemCollector result = validate("distribution-management-status.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("distributionManagement.status")).isTrue();
    }

    @Test
    void incompleteParent() throws Exception {
        SimpleProblemCollector result = validateRaw("incomplete-parent.xml");

        assertViolations(result, 3, 0, 0);
        assertThat(result.getFatals().get(0).contains("parent.groupId")).isTrue();
        assertThat(result.getFatals().get(1).contains("parent.artifactId")).isTrue();
        assertThat(result.getFatals().get(2).contains("parent.version")).isTrue();
    }

    @Test
    void hardCodedSystemPath() throws Exception {
        SimpleProblemCollector result = validateRaw("hard-coded-system-path.xml");

        assertViolations(result, 0, 0, 1);

        assertViolations(result, 0, 0, 1);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.systemPath' for test:a:jar should use a variable instead of a hard-coded path");

        SimpleProblemCollector result31 =
                validateRaw("hard-coded-system-path.xml", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        assertViolations(result31, 0, 0, 3);

        assertContains(
                result31.getWarnings().get(0),
                "'dependencies.dependency.scope' for test:a:jar declares usage of deprecated 'system' scope");
        assertContains(
                result31.getWarnings().get(1),
                "'dependencies.dependency.systemPath' for test:a:jar should use a variable instead of a hard-coded path");
        assertContains(
                result31.getWarnings().get(2),
                "'dependencies.dependency.scope' for test:b:jar declares usage of deprecated 'system' scope");
    }

    @Test
    void emptyModule() throws Exception {
        SimpleProblemCollector result = validate("empty-module.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("'modules.module[0]' has been specified without a path")).isTrue();
    }

    @Test
    void duplicatePlugin() throws Exception {
        SimpleProblemCollector result = validateRaw("duplicate-plugin.xml");

        assertViolations(result, 0, 0, 4);

        assertThat(result.getWarnings().get(0).contains("duplicate declaration of plugin test:duplicate")).isTrue();
        assertThat(result.getWarnings().get(1).contains("duplicate declaration of plugin test:managed-duplicate")).isTrue();
        assertThat(result.getWarnings().get(2).contains("duplicate declaration of plugin profile:duplicate")).isTrue();
        assertThat(result.getWarnings().get(3).contains("duplicate declaration of plugin profile:managed-duplicate")).isTrue();
    }

    @Test
    void duplicatePluginExecution() throws Exception {
        SimpleProblemCollector result = validateRaw("duplicate-plugin-execution.xml");

        assertViolations(result, 0, 4, 0);

        assertContains(result.getErrors().get(0), "duplicate execution with id a");
        assertContains(result.getErrors().get(1), "duplicate execution with id default");
        assertContains(result.getErrors().get(2), "duplicate execution with id c");
        assertContains(result.getErrors().get(3), "duplicate execution with id b");
    }

    @Test
    void reservedRepositoryId() throws Exception {
        SimpleProblemCollector result = validate("reserved-repository-id.xml");

        assertViolations(result, 0, 0, 4);

        assertContains(result.getWarnings().get(0), "'repositories.repository.id'" + " must not be 'local'");
        assertContains(result.getWarnings().get(1), "'pluginRepositories.pluginRepository.id' must not be 'local'");
        assertContains(result.getWarnings().get(2), "'distributionManagement.repository.id' must not be 'local'");
        assertContains(
                result.getWarnings().get(3), "'distributionManagement.snapshotRepository.id' must not be 'local'");
    }

    @Test
    void missingPluginDependencyGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-dependency-groupId.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains(":a:")).isTrue();
    }

    @Test
    void missingPluginDependencyArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-dependency-artifactId.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("test:")).isTrue();
    }

    @Test
    void missingPluginDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-dependency-version.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("test:a")).isTrue();
    }

    @Test
    void badPluginDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-plugin-dependency-version.xml");

        assertViolations(result, 0, 1, 0);

        assertThat(result.getErrors().get(0).contains("test:b")).isTrue();
    }

    @Test
    void badVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-version.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(result.getWarnings().get(0), "'version' must not contain any of these characters");
    }

    @Test
    void badSnapshotVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-snapshot-version.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(result.getWarnings().get(0), "'version' uses an unsupported snapshot version format");
    }

    @Test
    void badRepositoryId() throws Exception {
        SimpleProblemCollector result = validate("bad-repository-id.xml");

        assertViolations(result, 0, 0, 4);

        assertContains(
                result.getWarnings().get(0), "'repositories.repository.id' must not contain any of these characters");
        assertContains(
                result.getWarnings().get(1),
                "'pluginRepositories.pluginRepository.id' must not contain any of these characters");
        assertContains(
                result.getWarnings().get(2),
                "'distributionManagement.repository.id' must not contain any of these characters");
        assertContains(
                result.getWarnings().get(3),
                "'distributionManagement.snapshotRepository.id' must not contain any of these characters");
    }

    @Test
    void badDependencyExclusionId() throws Exception {
        SimpleProblemCollector result =
                validateEffective("bad-dependency-exclusion-id.xml", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0);

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0), "'dependencies.dependency.exclusions.exclusion.groupId' for gid:aid:jar");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.exclusions.exclusion.artifactId' for gid:aid:jar");

        // MNG-3832: Aether (part of M3+) supports wildcard expressions for exclusions

        SimpleProblemCollector result30 = validate("bad-dependency-exclusion-id.xml");

        assertViolations(result30, 0, 0, 0);
    }

    @Test
    void missingDependencyExclusionId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-exclusion-id.xml");

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.exclusions.exclusion.groupId' for gid:aid:jar is missing");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.exclusions.exclusion.artifactId' for gid:aid:jar is missing");
    }

    @Test
    void badImportScopeType() throws Exception {
        SimpleProblemCollector result = validateRaw("bad-import-scope-type.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(
                result.getWarnings().get(0),
                "'dependencyManagement.dependencies.dependency.type' for test:a:jar must be 'pom'");
    }

    @Test
    void badImportScopeClassifier() throws Exception {
        SimpleProblemCollector result = validateRaw("bad-import-scope-classifier.xml");

        assertViolations(result, 0, 1, 0);

        assertContains(
                result.getErrors().get(0),
                "'dependencyManagement.dependencies.dependency.classifier' for test:a:pom:cls must be empty");
    }

    @Test
    void systemPathRefersToProjectBasedir() throws Exception {
        SimpleProblemCollector result = validateRaw("basedir-system-path.xml");

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.systemPath' for test:a:jar should not point at files within the project directory");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.systemPath' for test:b:jar should not point at files within the project directory");

        SimpleProblemCollector result31 =
                validateRaw("basedir-system-path.xml", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        assertViolations(result31, 0, 0, 4);

        assertContains(
                result31.getWarnings().get(0),
                "'dependencies.dependency.scope' for test:a:jar declares usage of deprecated 'system' scope");
        assertContains(
                result31.getWarnings().get(1),
                "'dependencies.dependency.systemPath' for test:a:jar should not point at files within the project directory");
        assertContains(
                result31.getWarnings().get(2),
                "'dependencies.dependency.scope' for test:b:jar declares usage of deprecated 'system' scope");
        assertContains(
                result31.getWarnings().get(3),
                "'dependencies.dependency.systemPath' for test:b:jar should not point at files within the project directory");
    }

    @Test
    void invalidVersionInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-plugin-version-pluginManagement.xml");

        assertViolations(result, 1, 0, 0);

        assertThat(result.getFatals().get(0)).isEqualTo("'build.pluginManagement.plugins.plugin.(groupId:artifactId)' version of a plugin must be defined. ");
    }

    @Test
    void invalidGroupIdInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-groupId-pluginManagement.xml");

        assertViolations(result, 1, 0, 0);

        assertThat(result.getFatals().get(0)).isEqualTo("'build.pluginManagement.plugins.plugin.(groupId:artifactId)' groupId of a plugin must be defined. ");
    }

    @Test
    void invalidArtifactIdInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-artifactId-pluginManagement.xml");

        assertViolations(result, 1, 0, 0);

        assertThat(result.getFatals().get(0)).isEqualTo("'build.pluginManagement.plugins.plugin.(groupId:artifactId)' artifactId of a plugin must be defined. ");
    }

    @Test
    void invalidGroupAndArtifactIdInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-ga-pluginManagement.xml");

        assertViolations(result, 2, 0, 0);

        assertThat(result.getFatals().get(0)).isEqualTo("'build.pluginManagement.plugins.plugin.(groupId:artifactId)' groupId of a plugin must be defined. ");

        assertThat(result.getFatals().get(1)).isEqualTo("'build.pluginManagement.plugins.plugin.(groupId:artifactId)' artifactId of a plugin must be defined. ");
    }

    @Test
    void missingReportPluginVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-report-version-pom.xml");

        assertViolations(result, 0, 0, 0);
    }

    @Test
    void deprecatedDependencyMetaversionsLatestAndRelease() throws Exception {
        SimpleProblemCollector result = validateRaw("deprecated-dependency-metaversions-latest-and-release.xml");

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.version' for test:a:jar is either LATEST or RELEASE (both of them are being deprecated)");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.version' for test:b:jar is either LATEST or RELEASE (both of them are being deprecated)");
    }

    @Test
    void selfReferencingDependencyInRawModel() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/self-referencing.xml");

        assertViolations(result, 1, 0, 0);

        assertThat(result.getFatals().get(0)).isEqualTo("'dependencies.dependency[com.example.group:testinvalidpom:0.0.1-SNAPSHOT]' for com.example.group:testinvalidpom:0.0.1-SNAPSHOT is referencing itself.");
    }

    @Test
    void selfReferencingDependencyWithClassifierInRawModel() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/self-referencing-classifier.xml");

        assertViolations(result, 0, 0, 0);
    }

    @Test
    void ciFriendlySha1() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-sha1.xml");
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void ciFriendlyRevision() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-revision.xml");
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void ciFriendlyChangeList() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-changelist.xml");
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void ciFriendlyAllExpressions() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-all-expressions.xml");
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void ciFriendlyBad() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-ci-friendly.xml");
        assertViolations(result, 0, 0, 1);
        assertThat(result.getWarnings().get(0)).isEqualTo("'version' contains an expression but should be a constant.");
    }

    @Test
    void ciFriendlyBadSha1Plus() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-ci-friendly-sha1plus.xml");
        assertViolations(result, 0, 0, 1);
        assertThat(result.getWarnings().get(0)).isEqualTo("'version' contains an expression but should be a constant.");
    }

    @Test
    void ciFriendlyBadSha1Plus2() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-ci-friendly-sha1plus2.xml");
        assertViolations(result, 0, 0, 1);
        assertThat(result.getWarnings().get(0)).isEqualTo("'version' contains an expression but should be a constant.");
    }

    @Test
    void parentVersionLATEST() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-parent-version-latest.xml");
        assertViolations(result, 0, 0, 1);
        assertThat(result.getWarnings().get(0)).isEqualTo("'parent.version' is either LATEST or RELEASE (both of them are being deprecated)");
    }

    @Test
    void parentVersionRELEASE() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-parent-version-release.xml");
        assertViolations(result, 0, 0, 1);
        assertThat(result.getWarnings().get(0)).isEqualTo("'parent.version' is either LATEST or RELEASE (both of them are being deprecated)");
    }

    @Test
    void repositoryWithExpression() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/repository-with-expression.xml");
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void repositoryWithBasedirExpression() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/repository-with-basedir-expression.xml");
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void profileActivationWithAllowedExpression() throws Exception {
        SimpleProblemCollector result = validateRaw(
                "raw-model/profile-activation-file-with-allowed-expressions.xml",
                mbr -> mbr.setUserProperties(new Properties() {
                    @Serial
                    private static final long serialVersionUID = 1L;

                    {
                        setProperty("foo", "foo");
                        setProperty("bar", "foo");
                    }
                }));
        assertViolations(result, 0, 0, 0);
    }

    @Test
    void profileActivationFileWithProjectExpression() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/profile-activation-file-with-project-expressions.xml");
        assertViolations(result, 0, 0, 2);

        assertThat(result.getWarnings().get(0)).isEqualTo("'profiles.profile[exists-project-version].activation.file.exists' "
                + "Failed to interpolate profile activation property ${project.version}/test.txt: "
                + "${project.version} expressions are not supported during profile activation.");

        assertThat(result.getWarnings().get(1)).isEqualTo("'profiles.profile[missing-project-version].activation.file.missing' "
                + "Failed to interpolate profile activation property ${project.version}/test.txt: "
                + "${project.version} expressions are not supported during profile activation.");
    }

    @Test
    void profileActivationPropertyWithProjectExpression() throws Exception {
        SimpleProblemCollector result =
                validateRaw("raw-model/profile-activation-property-with-project-expressions.xml");
        assertViolations(result, 0, 0, 2);

        assertThat(result.getWarnings().get(0)).isEqualTo("'profiles.profile[property-name-project-version].activation.property.name' "
                + "Failed to interpolate profile activation property ${project.version}: "
                + "${project.version} expressions are not supported during profile activation.");

        assertThat(result.getWarnings().get(1)).isEqualTo("'profiles.profile[property-value-project-version].activation.property.value' "
                + "Failed to interpolate profile activation property ${project.version}: "
                + "${project.version} expressions are not supported during profile activation.");
    }
}
