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
import java.util.List;
import java.util.Properties;
import java.util.function.UnaryOperator;

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public class DefaultModelValidatorTest extends TestCase {

    private ModelValidator validator;

    private Model read(String pom) throws Exception {
        String resource = "/poms/validation/" + pom;
        InputStream is = getClass().getResourceAsStream(resource);
        assertNotNull("missing resource: " + resource, is);
        return new MavenXpp3Reader().read(is);
    }

    private SimpleProblemCollector validate(String pom) throws Exception {
        return validateEffective(pom, UnaryOperator.identity());
    }

    private SimpleProblemCollector validateRaw(String pom) throws Exception {
        return validateRaw(pom, UnaryOperator.identity());
    }

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
        assertTrue("\"" + substring + "\" was not found in: " + msg, msg.contains(substring));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        validator = new DefaultModelValidator(new DefaultModelVersionProcessor());
    }

    @Override
    protected void tearDown() throws Exception {
        this.validator = null;

        super.tearDown();
    }

    private void assertViolations(SimpleProblemCollector result, int fatals, int errors, int warnings) {
        assertEquals(
                String.valueOf(result.getFatals()), fatals, result.getFatals().size());
        assertEquals(
                String.valueOf(result.getErrors()), errors, result.getErrors().size());
        assertEquals(
                String.valueOf(result.getWarnings()),
                warnings,
                result.getWarnings().size());
    }

    public void testMissingModelVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-modelVersion-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals("'modelVersion' is missing.", result.getErrors().get(0));
    }

    public void testBadModelVersion() throws Exception {
        SimpleProblemCollector result =
                validateRaw("bad-modelVersion.xml", ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertViolations(result, 1, 0, 0);

        assertTrue(result.getFatals().get(0).contains("modelVersion"));
    }

    public void testModelVersion40() throws Exception {
        SimpleProblemCollector result =
                validateRaw("modelVersion-4_0.xml", ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("'modelVersion' must be one of"));
    }

    public void testMissingArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals("'artifactId' is missing.", result.getErrors().get(0));
    }

    public void testMissingGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-groupId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals("'groupId' is missing.", result.getErrors().get(0));
    }

    public void testInvalidIds() throws Exception {
        SimpleProblemCollector result = validate("invalid-ids-pom.xml");

        assertViolations(result, 0, 2, 0);

        assertEquals(
                "'groupId' with value 'o/a/m' does not match a valid id pattern.",
                result.getErrors().get(0));

        assertEquals(
                "'artifactId' with value 'm$-do$' does not match a valid id pattern.",
                result.getErrors().get(1));
    }

    public void testMissingType() throws Exception {
        SimpleProblemCollector result = validate("missing-type-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals("'packaging' is missing.", result.getErrors().get(0));
    }

    public void testMissingVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-version-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals("'version' is missing.", result.getErrors().get(0));
    }

    public void testInvalidAggregatorPackaging() throws Exception {
        SimpleProblemCollector result = validate("invalid-aggregator-packaging-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("Aggregator projects require 'pom' as packaging."));
    }

    public void testMissingDependencyArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors()
                .get(0)
                .contains("'dependencies.dependency.artifactId' for groupId:null:jar is missing"));
    }

    public void testMissingDependencyGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-groupId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors()
                .get(0)
                .contains("'dependencies.dependency.groupId' for null:artifactId:jar is missing"));
    }

    public void testMissingDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-version-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors()
                .get(0)
                .contains("'dependencies.dependency.version' for groupId:artifactId:jar is missing"));
    }

    public void testMissingDependencyManagementArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-mgmt-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors()
                .get(0)
                .contains("'dependencyManagement.dependencies.dependency.artifactId' for groupId:null:jar is missing"));
    }

    public void testMissingDependencyManagementGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-mgmt-groupId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors()
                .get(0)
                .contains("'dependencyManagement.dependencies.dependency.groupId' for null:artifactId:jar is missing"));
    }

    public void testMissingAll() throws Exception {
        SimpleProblemCollector result = validate("missing-1-pom.xml");

        assertViolations(result, 0, 4, 0);

        List<String> messages = result.getErrors();

        assertTrue(messages.contains("\'modelVersion\' is missing."));
        assertTrue(messages.contains("\'groupId\' is missing."));
        assertTrue(messages.contains("\'artifactId\' is missing."));
        assertTrue(messages.contains("\'version\' is missing."));
        // type is inherited from the super pom
    }

    public void testMissingPluginArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-artifactId-pom.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals(
                "'build.plugins.plugin.artifactId' is missing.",
                result.getErrors().get(0));
    }

    public void testEmptyPluginVersion() throws Exception {
        SimpleProblemCollector result = validate("empty-plugin-version.xml");

        assertViolations(result, 0, 1, 0);

        assertEquals(
                "'build.plugins.plugin.version' for org.apache.maven.plugins:maven-it-plugin"
                        + " must be a valid version but is ''.",
                result.getErrors().get(0));
    }

    public void testMissingRepositoryId() throws Exception {
        SimpleProblemCollector result =
                validateRaw("missing-repository-id-pom.xml", ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        assertViolations(result, 0, 4, 0);

        assertEquals(
                "'repositories.repository.id' is missing.", result.getErrors().get(0));

        assertEquals(
                "'repositories.repository.[null].url' is missing.",
                result.getErrors().get(1));

        assertEquals(
                "'pluginRepositories.pluginRepository.id' is missing.",
                result.getErrors().get(2));

        assertEquals(
                "'pluginRepositories.pluginRepository.[null].url' is missing.",
                result.getErrors().get(3));
    }

    public void testMissingResourceDirectory() throws Exception {
        SimpleProblemCollector result = validate("missing-resource-directory-pom.xml");

        assertViolations(result, 0, 2, 0);

        assertEquals(
                "'build.resources.resource.directory' is missing.",
                result.getErrors().get(0));

        assertEquals(
                "'build.testResources.testResource.directory' is missing.",
                result.getErrors().get(1));
    }

    public void testBadPluginDependencyScope() throws Exception {
        SimpleProblemCollector result = validate("bad-plugin-dependency-scope.xml");

        assertViolations(result, 0, 3, 0);

        assertTrue(result.getErrors().get(0).contains("test:d"));

        assertTrue(result.getErrors().get(1).contains("test:e"));

        assertTrue(result.getErrors().get(2).contains("test:f"));
    }

    public void testBadDependencyScope() throws Exception {
        SimpleProblemCollector result = validate("bad-dependency-scope.xml");

        assertViolations(result, 0, 0, 2);

        assertTrue(result.getWarnings().get(0).contains("test:f"));

        assertTrue(result.getWarnings().get(1).contains("test:g"));
    }

    public void testBadDependencyManagementScope() throws Exception {
        SimpleProblemCollector result = validate("bad-dependency-management-scope.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(result.getWarnings().get(0), "test:g");
    }

    public void testBadDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-dependency-version.xml");

        assertViolations(result, 0, 2, 0);

        assertContains(
                result.getErrors().get(0), "'dependencies.dependency.version' for test:b:jar must be a valid version");
        assertContains(
                result.getErrors().get(1),
                "'dependencies.dependency.version' for test:c:jar must not contain any of these characters");
    }

    public void testDuplicateModule() throws Exception {
        SimpleProblemCollector result = validate("duplicate-module.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("child"));
    }

    public void testDuplicateProfileId() throws Exception {
        SimpleProblemCollector result = validateRaw("duplicate-profile-id.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("non-unique-id"));
    }

    public void testBadPluginVersion() throws Exception {
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

    public void testDistributionManagementStatus() throws Exception {
        SimpleProblemCollector result = validate("distribution-management-status.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("distributionManagement.status"));
    }

    public void testIncompleteParent() throws Exception {
        SimpleProblemCollector result = validateRaw("incomplete-parent.xml");

        assertViolations(result, 3, 0, 0);

        assertTrue(result.getFatals().get(0).contains("parent.groupId"));
        assertTrue(result.getFatals().get(1).contains("parent.artifactId"));
        assertTrue(result.getFatals().get(2).contains("parent.version"));
    }

    public void testHardCodedSystemPath() throws Exception {
        SimpleProblemCollector result = validateRaw("hard-coded-system-path.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.systemPath' for test:a:jar should use a variable instead of a hard-coded path");

        SimpleProblemCollector result_31 =
                validateRaw("hard-coded-system-path.xml", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        assertViolations(result_31, 0, 0, 3);

        assertContains(
                result_31.getWarnings().get(0),
                "'dependencies.dependency.scope' for test:a:jar declares usage of deprecated 'system' scope");
        assertContains(
                result_31.getWarnings().get(1),
                "'dependencies.dependency.systemPath' for test:a:jar should use a variable instead of a hard-coded path");
        assertContains(
                result_31.getWarnings().get(2),
                "'dependencies.dependency.scope' for test:b:jar declares usage of deprecated 'system' scope");
    }

    public void testEmptyModule() throws Exception {
        SimpleProblemCollector result = validate("empty-module.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("'modules.module[0]' has been specified without a path"));
    }

    public void testDuplicatePlugin() throws Exception {
        SimpleProblemCollector result = validateRaw("duplicate-plugin.xml");

        assertViolations(result, 0, 0, 4);

        assertTrue(result.getWarnings().get(0).contains("duplicate declaration of plugin test:duplicate"));
        assertTrue(result.getWarnings().get(1).contains("duplicate declaration of plugin test:managed-duplicate"));
        assertTrue(result.getWarnings().get(2).contains("duplicate declaration of plugin profile:duplicate"));
        assertTrue(result.getWarnings().get(3).contains("duplicate declaration of plugin profile:managed-duplicate"));
    }

    public void testDuplicatePluginExecution() throws Exception {
        SimpleProblemCollector result = validateRaw("duplicate-plugin-execution.xml");

        assertViolations(result, 0, 4, 0);

        assertContains(result.getErrors().get(0), "duplicate execution with id a");
        assertContains(result.getErrors().get(1), "duplicate execution with id default");
        assertContains(result.getErrors().get(2), "duplicate execution with id c");
        assertContains(result.getErrors().get(3), "duplicate execution with id b");
    }

    public void testReservedRepositoryId() throws Exception {
        SimpleProblemCollector result = validate("reserved-repository-id.xml");

        assertViolations(result, 0, 0, 4);

        assertContains(result.getWarnings().get(0), "'repositories.repository.id'" + " must not be 'local'");
        assertContains(result.getWarnings().get(1), "'pluginRepositories.pluginRepository.id' must not be 'local'");
        assertContains(result.getWarnings().get(2), "'distributionManagement.repository.id' must not be 'local'");
        assertContains(
                result.getWarnings().get(3), "'distributionManagement.snapshotRepository.id' must not be 'local'");
    }

    public void testMissingPluginDependencyGroupId() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-dependency-groupId.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains(":a:"));
    }

    public void testMissingPluginDependencyArtifactId() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-dependency-artifactId.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("test:"));
    }

    public void testMissingPluginDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-plugin-dependency-version.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("test:a"));
    }

    public void testBadPluginDependencyVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-plugin-dependency-version.xml");

        assertViolations(result, 0, 1, 0);

        assertTrue(result.getErrors().get(0).contains("test:b"));
    }

    public void testBadVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-version.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(result.getWarnings().get(0), "'version' must not contain any of these characters");
    }

    public void testBadSnapshotVersion() throws Exception {
        SimpleProblemCollector result = validate("bad-snapshot-version.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(result.getWarnings().get(0), "'version' uses an unsupported snapshot version format");
    }

    public void testBadRepositoryId() throws Exception {
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

    public void testBadDependencyExclusionId() throws Exception {
        SimpleProblemCollector result =
                validateEffective("bad-dependency-exclusion-id.xml", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0);

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0), "'dependencies.dependency.exclusions.exclusion.groupId' for gid:aid:jar");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.exclusions.exclusion.artifactId' for gid:aid:jar");

        // MNG-3832: Aether (part of M3+) supports wildcard expressions for exclusions

        SimpleProblemCollector result_30 = validate("bad-dependency-exclusion-id.xml");

        assertViolations(result_30, 0, 0, 0);
    }

    public void testMissingDependencyExclusionId() throws Exception {
        SimpleProblemCollector result = validate("missing-dependency-exclusion-id.xml");

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.exclusions.exclusion.groupId' for gid:aid:jar is missing");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.exclusions.exclusion.artifactId' for gid:aid:jar is missing");
    }

    public void testBadImportScopeType() throws Exception {
        SimpleProblemCollector result = validateRaw("bad-import-scope-type.xml");

        assertViolations(result, 0, 0, 1);

        assertContains(
                result.getWarnings().get(0),
                "'dependencyManagement.dependencies.dependency.type' for test:a:jar must be 'pom'");
    }

    public void testBadImportScopeClassifier() throws Exception {
        SimpleProblemCollector result = validateRaw("bad-import-scope-classifier.xml");

        assertViolations(result, 0, 1, 0);

        assertContains(
                result.getErrors().get(0),
                "'dependencyManagement.dependencies.dependency.classifier' for test:a:pom:cls must be empty");
    }

    public void testSystemPathRefersToProjectBasedir() throws Exception {
        SimpleProblemCollector result = validateRaw("basedir-system-path.xml");

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.systemPath' for test:a:jar should not point at files within the project directory");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.systemPath' for test:b:jar should not point at files within the project directory");

        SimpleProblemCollector result_31 =
                validateRaw("basedir-system-path.xml", ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1);

        assertViolations(result_31, 0, 0, 4);

        assertContains(
                result_31.getWarnings().get(0),
                "'dependencies.dependency.scope' for test:a:jar declares usage of deprecated 'system' scope");
        assertContains(
                result_31.getWarnings().get(1),
                "'dependencies.dependency.systemPath' for test:a:jar should not point at files within the project directory");
        assertContains(
                result_31.getWarnings().get(2),
                "'dependencies.dependency.scope' for test:b:jar declares usage of deprecated 'system' scope");
        assertContains(
                result_31.getWarnings().get(3),
                "'dependencies.dependency.systemPath' for test:b:jar should not point at files within the project directory");
    }

    public void testInvalidVersionInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-plugin-version-pluginManagement.xml");

        assertViolations(result, 1, 0, 0);

        assertEquals(
                "'build.pluginManagement.plugins.plugin.(groupId:artifactId)' version of a plugin must be defined. ",
                result.getFatals().get(0));
    }

    public void testInvalidGroupIdInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-groupId-pluginManagement.xml");

        assertViolations(result, 1, 0, 0);

        assertEquals(
                "'build.pluginManagement.plugins.plugin.(groupId:artifactId)' groupId of a plugin must be defined. ",
                result.getFatals().get(0));
    }

    public void testInvalidArtifactIdInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-artifactId-pluginManagement.xml");

        assertViolations(result, 1, 0, 0);

        assertEquals(
                "'build.pluginManagement.plugins.plugin.(groupId:artifactId)' artifactId of a plugin must be defined. ",
                result.getFatals().get(0));
    }

    public void testInvalidGroupAndArtifactIdInPluginManagement() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/missing-ga-pluginManagement.xml");

        assertViolations(result, 2, 0, 0);

        assertEquals(
                "'build.pluginManagement.plugins.plugin.(groupId:artifactId)' groupId of a plugin must be defined. ",
                result.getFatals().get(0));

        assertEquals(
                "'build.pluginManagement.plugins.plugin.(groupId:artifactId)' artifactId of a plugin must be defined. ",
                result.getFatals().get(1));
    }

    public void testMissingReportPluginVersion() throws Exception {
        SimpleProblemCollector result = validate("missing-report-version-pom.xml");

        assertViolations(result, 0, 0, 0);
    }

    public void testDeprecatedDependencyMetaversionsLatestAndRelease() throws Exception {
        SimpleProblemCollector result = validateRaw("deprecated-dependency-metaversions-latest-and-release.xml");

        assertViolations(result, 0, 0, 2);

        assertContains(
                result.getWarnings().get(0),
                "'dependencies.dependency.version' for test:a:jar is either LATEST or RELEASE (both of them are being deprecated)");
        assertContains(
                result.getWarnings().get(1),
                "'dependencies.dependency.version' for test:b:jar is either LATEST or RELEASE (both of them are being deprecated)");
    }

    public void testSelfReferencingDependencyInRawModel() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/self-referencing.xml");

        assertViolations(result, 1, 0, 0);

        assertEquals(
                "'dependencies.dependency[com.example.group:testinvalidpom:0.0.1-SNAPSHOT]' for com.example.group:testinvalidpom:0.0.1-SNAPSHOT is referencing itself.",
                result.getFatals().get(0));
    }

    public void testSelfReferencingDependencyWithClassifierInRawModel() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/self-referencing-classifier.xml");

        assertViolations(result, 0, 0, 0);
    }

    public void testCiFriendlySha1() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-sha1.xml");
        assertViolations(result, 0, 0, 0);
    }

    public void testCiFriendlyRevision() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-revision.xml");
        assertViolations(result, 0, 0, 0);
    }

    public void testCiFriendlyChangeList() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-changelist.xml");
        assertViolations(result, 0, 0, 0);
    }

    public void testCiFriendlyAllExpressions() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/ok-ci-friendly-all-expressions.xml");
        assertViolations(result, 0, 0, 0);
    }

    public void testCiFriendlyBad() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-ci-friendly.xml");
        assertViolations(result, 0, 0, 1);
        assertEquals(
                "'version' contains an expression but should be a constant.",
                result.getWarnings().get(0));
    }

    public void testCiFriendlyBadSha1Plus() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-ci-friendly-sha1plus.xml");
        assertViolations(result, 0, 0, 1);
        assertEquals(
                "'version' contains an expression but should be a constant.",
                result.getWarnings().get(0));
    }

    public void testCiFriendlyBadSha1Plus2() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-ci-friendly-sha1plus2.xml");
        assertViolations(result, 0, 0, 1);
        assertEquals(
                "'version' contains an expression but should be a constant.",
                result.getWarnings().get(0));
    }

    public void testParentVersionLATEST() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-parent-version-latest.xml");
        assertViolations(result, 0, 0, 1);
        assertEquals(
                "'parent.version' is either LATEST or RELEASE (both of them are being deprecated)",
                result.getWarnings().get(0));
    }

    public void testParentVersionRELEASE() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/bad-parent-version-release.xml");
        assertViolations(result, 0, 0, 1);
        assertEquals(
                "'parent.version' is either LATEST or RELEASE (both of them are being deprecated)",
                result.getWarnings().get(0));
    }

    public void testRepositoryWithExpression() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/repository-with-expression.xml");
        assertViolations(result, 0, 0, 0);
    }

    public void testRepositoryWithBasedirExpression() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/repository-with-basedir-expression.xml");
        assertViolations(result, 0, 0, 0);
    }

    public void testProfileActivationWithAllowedExpression() throws Exception {
        SimpleProblemCollector result = validateRaw(
                "raw-model/profile-activation-file-with-allowed-expressions.xml",
                mbr -> mbr.setUserProperties(new Properties() {
                    private static final long serialVersionUID = 1L;

                    {
                        setProperty("foo", "foo");
                        setProperty("bar", "foo");
                    }
                }));
        assertViolations(result, 0, 0, 0);
    }

    public void testProfileActivationFileWithProjectExpression() throws Exception {
        SimpleProblemCollector result = validateRaw("raw-model/profile-activation-file-with-project-expressions.xml");
        assertViolations(result, 0, 0, 2);

        assertEquals(
                "'profiles.profile[exists-project-version].activation.file.exists' "
                        + "Failed to interpolate profile activation property ${project.version}/test.txt: "
                        + "${project.version} expressions are not supported during profile activation.",
                result.getWarnings().get(0));

        assertEquals(
                "'profiles.profile[missing-project-version].activation.file.missing' "
                        + "Failed to interpolate profile activation property ${project.version}/test.txt: "
                        + "${project.version} expressions are not supported during profile activation.",
                result.getWarnings().get(1));
    }

    public void testProfileActivationPropertyWithProjectExpression() throws Exception {
        SimpleProblemCollector result =
                validateRaw("raw-model/profile-activation-property-with-project-expressions.xml");
        assertViolations(result, 0, 0, 2);

        assertEquals(
                "'profiles.profile[property-name-project-version].activation.property.name' "
                        + "Failed to interpolate profile activation property ${project.version}: "
                        + "${project.version} expressions are not supported during profile activation.",
                result.getWarnings().get(0));

        assertEquals(
                "'profiles.profile[property-value-project-version].activation.property.value' "
                        + "Failed to interpolate profile activation property ${project.version}: "
                        + "${project.version} expressions are not supported during profile activation.",
                result.getWarnings().get(1));
    }
}
