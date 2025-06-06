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

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Organization;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.model.Site;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.PathTranslator;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.api.services.model.UrlNormalizer;
import org.apache.maven.impl.DefaultUrlNormalizer;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.apache.maven.impl.model.rootlocator.DefaultRootLocator;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;

import static org.apache.maven.api.services.ModelBuilderRequest.RequestType.BUILD_PROJECT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultModelInterpolatorTest {

    Map<String, String> context;
    ModelInterpolator interpolator;
    Session session;
    AtomicReference<Path> rootDirectory; // used in TestRootLocator below

    @BeforeEach
    public void setUp() {
        context = new HashMap<>();
        context.put("basedir", "myBasedir");
        context.put("anotherdir", "anotherBasedir");
        context.put("project.baseUri", "myBaseUri");

        session = ApiRunner.createSession(injector -> {
            injector.bindInstance(DefaultModelInterpolatorTest.class, this);
        });
        interpolator = session.getService(Lookup.class).lookup(DefaultModelInterpolator.class);
    }

    protected void assertProblemFree(SimpleProblemCollector collector) {
        assertEquals(0, collector.getErrors().size(), "Expected no errors");
        assertEquals(0, collector.getWarnings().size(), "Expected no warnings");
        assertEquals(0, collector.getFatals().size(), "Expected no fatals");
    }

    @SuppressWarnings("SameParameterValue")
    protected void assertCollectorState(
            int numFatals, int numErrors, int numWarnings, SimpleProblemCollector collector) {
        assertEquals(numErrors, collector.getErrors().size(), "Errors");
        assertEquals(numWarnings, collector.getWarnings().size(), "Warnings");
        assertEquals(numFatals, collector.getFatals().size(), "Fatals");
    }

    private ModelBuilderRequest.ModelBuilderRequestBuilder createModelBuildingRequest(Map<String, String> p) {
        ModelBuilderRequest.ModelBuilderRequestBuilder config = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT);
        if (p != null) {
            config.systemProperties(p);
        }
        return config;
    }

    @Test
    public void testDefaultBuildTimestampFormatShouldFormatTimeIn24HourFormat() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        cal.set(Calendar.HOUR, 12);
        cal.set(Calendar.AM_PM, Calendar.AM);

        // just to make sure all the bases are covered...
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 16);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.YEAR, 1976);
        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        cal.set(Calendar.DATE, 11);

        Instant firstTestDate = Instant.ofEpochMilli(cal.getTime().getTime());

        cal.set(Calendar.HOUR, 11);
        cal.set(Calendar.AM_PM, Calendar.PM);

        // just to make sure all the bases are covered...
        cal.set(Calendar.HOUR_OF_DAY, 23);

        Instant secondTestDate = Instant.ofEpochMilli(cal.getTime().getTime());

        DateTimeFormatter format = DateTimeFormatter.ofPattern(MavenBuildTimestamp.DEFAULT_BUILD_TIMESTAMP_FORMAT)
                .withZone(ZoneId.of("UTC"));

        assertEquals("1976-11-11T00:16:00Z", format.format(firstTestDate));
        assertEquals("1976-11-11T23:16:00Z", format.format(secondTestDate));
    }

    @Test
    public void testDefaultBuildTimestampFormatWithLocalTimeZoneMidnightRollover() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 16);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.YEAR, 2014);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.DATE, 16);

        Instant firstTestDate = Instant.ofEpochMilli(cal.getTime().getTime());

        cal.set(Calendar.MONTH, Calendar.NOVEMBER);

        Instant secondTestDate = Instant.ofEpochMilli(cal.getTime().getTime());

        DateTimeFormatter format = DateTimeFormatter.ofPattern(MavenBuildTimestamp.DEFAULT_BUILD_TIMESTAMP_FORMAT)
                .withZone(ZoneId.of("UTC"));
        assertEquals("2014-06-15T23:16:00Z", format.format(firstTestDate));
        assertEquals("2014-11-16T00:16:00Z", format.format(secondTestDate));
    }

    @Test
    public void testShouldNotThrowExceptionOnReferenceToNonExistentValue() throws Exception {
        Scm scm = Scm.newBuilder().connection("${test}/somepath").build();
        Model model = Model.newBuilder().scm(scm).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);

        assertProblemFree(collector);
        assertEquals("${test}/somepath", out.getScm().getConnection());
    }

    @Test
    public void testShouldThrowExceptionOnRecursiveScmConnectionReference() throws Exception {
        Scm scm = Scm.newBuilder()
                .connection("${project.scm.connection}/somepath")
                .build();
        Model model = Model.newBuilder().scm(scm).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        interpolator.interpolateModel(
                model, null, createModelBuildingRequest(context).build(), collector);
        assertCollectorState(0, 1, 0, collector);
    }

    @Test
    public void testShouldNotThrowExceptionOnReferenceToValueContainingNakedExpression() throws Exception {
        Scm scm = Scm.newBuilder().connection("${test}/somepath").build();
        Map<String, String> props = new HashMap<>();
        props.put("test", "test");
        Model model = Model.newBuilder().scm(scm).properties(props).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);

        assertProblemFree(collector);

        assertEquals("test/somepath", out.getScm().getConnection());
    }

    @Test
    void shouldInterpolateOrganizationNameCorrectly() throws Exception {
        String orgName = "MyCo";

        Model model = Model.newBuilder()
                .name("${project.organization.name} Tools")
                .organization(Organization.newBuilder().name(orgName).build())
                .build();

        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), new SimpleProblemCollector());

        assertEquals(orgName + " Tools", out.getName());
    }

    @Test
    public void shouldInterpolateDependencyVersionToSetSameAsProjectVersion() throws Exception {
        Model model = Model.newBuilder()
                .version("3.8.1")
                .dependencies(Collections.singletonList(
                        Dependency.newBuilder().version("${project.version}").build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertCollectorState(0, 0, 0, collector);

        assertEquals("3.8.1", (out.getDependencies().get(0)).getVersion());
    }

    @Test
    public void testShouldNotInterpolateDependencyVersionWithInvalidReference() throws Exception {
        Model model = Model.newBuilder()
                .version("3.8.1")
                .dependencies(Collections.singletonList(
                        Dependency.newBuilder().version("${something}").build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals("${something}", (out.getDependencies().get(0)).getVersion());
    }

    @Test
    public void testTwoReferences() throws Exception {
        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .dependencies(Collections.singletonList(Dependency.newBuilder()
                        .version("${project.artifactId}-${project.version}")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertCollectorState(0, 0, 0, collector);

        assertEquals("foo-3.8.1", (out.getDependencies().get(0)).getVersion());
    }

    @Test
    public void testProperty() throws Exception {
        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(Repository.newBuilder()
                        .url("file://localhost/${anotherdir}/temp-repo")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model,
                Paths.get("projectBasedir"),
                createModelBuildingRequest(context).build(),
                collector);
        assertProblemFree(collector);

        assertEquals(
                "file://localhost/anotherBasedir/temp-repo",
                (out.getRepositories().get(0)).getUrl());
    }

    @Test
    public void testBasedirUnx() throws Exception {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path projectBasedir = fs.getPath("projectBasedir");

        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(
                        Repository.newBuilder().url("${basedir}/temp-repo").build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, projectBasedir, createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals(
                projectBasedir.toAbsolutePath() + "/temp-repo",
                (out.getRepositories().get(0)).getUrl());
    }

    @Test
    public void testBasedirWin() throws Exception {
        FileSystem fs = Jimfs.newFileSystem(Configuration.windows());
        Path projectBasedir = fs.getPath("projectBasedir");

        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(
                        Repository.newBuilder().url("${basedir}/temp-repo").build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, projectBasedir, createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals(
                projectBasedir.toAbsolutePath() + "/temp-repo",
                (out.getRepositories().get(0)).getUrl());
    }

    @Test
    public void testBaseUri() throws Exception {
        Path projectBasedir = Paths.get("projectBasedir");

        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(Repository.newBuilder()
                        .url("${project.baseUri}/temp-repo")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, projectBasedir, createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals(
                projectBasedir.resolve("temp-repo").toUri().toString(),
                (out.getRepositories().get(0)).getUrl());
    }

    @Test
    void testRootDirectory() throws Exception {
        Path rootDirectory = Paths.get("myRootDirectory");

        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(Repository.newBuilder()
                        .url("file:${project.rootDirectory}/temp-repo")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, rootDirectory, createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals("file:myRootDirectory/temp-repo", (out.getRepositories().get(0)).getUrl());
    }

    @Test
    void testRootDirectoryWithUri() throws Exception {
        Path rootDirectory = Paths.get("myRootDirectory");

        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(Repository.newBuilder()
                        .url("${project.rootDirectory.uri}/temp-repo")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, rootDirectory, createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals(
                rootDirectory.resolve("temp-repo").toUri().toString(),
                (out.getRepositories().get(0)).getUrl());
    }

    @Test
    void testRootDirectoryWithNull() throws Exception {
        Path projectDirectory = Paths.get("myProjectDirectory");
        this.rootDirectory = new AtomicReference<>(null);

        Model model = Model.newBuilder()
                .version("3.8.1")
                .artifactId("foo")
                .repositories(Collections.singletonList(Repository.newBuilder()
                        .url("file:///${project.rootDirectory}/temp-repo")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> interpolator.interpolateModel(
                        model,
                        projectDirectory,
                        createModelBuildingRequest(context).build(),
                        collector));

        assertEquals(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE, e.getMessage());
    }

    @Test
    public void testEnvars() throws Exception {
        context.put("env.HOME", "/path/to/home");

        Map<String, String> modelProperties = new HashMap<>();
        modelProperties.put("outputDirectory", "${env.HOME}");

        Model model = Model.newBuilder().properties(modelProperties).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals("/path/to/home", out.getProperties().get("outputDirectory"));
    }

    @Test
    public void envarExpressionThatEvaluatesToNullReturnsTheLiteralString() throws Exception {

        Map<String, String> modelProperties = new HashMap<>();
        modelProperties.put("outputDirectory", "${env.DOES_NOT_EXIST}");

        Model model = Model.newBuilder().properties(modelProperties).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals("${env.DOES_NOT_EXIST}", out.getProperties().get("outputDirectory"));
    }

    @Test
    public void expressionThatEvaluatesToNullReturnsTheLiteralString() throws Exception {
        Map<String, String> modelProperties = new HashMap<>();
        modelProperties.put("outputDirectory", "${DOES_NOT_EXIST}");

        Model model = Model.newBuilder().properties(modelProperties).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertEquals("${DOES_NOT_EXIST}", out.getProperties().get("outputDirectory"));
    }

    @Test
    public void shouldInterpolateSourceDirectoryReferencedFromResourceDirectoryCorrectly() throws Exception {
        final SimpleProblemCollector collector = new SimpleProblemCollector();
        assertCollectorState(0, 0, 0, collector);
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sourceDirectory("correct")
                        .resources(List.of(Resource.newBuilder()
                                .directory("${project.build.sourceDirectory}")
                                .build()))
                        .build())
                .build();
        String sourceDirectory = interpolator
                .interpolateModel(
                        model, null, createModelBuildingRequest(context).build(), collector)
                .getBuild()
                .getResources()
                .iterator()
                .next()
                .getDirectory();
        assertEquals(model.getBuild().getSourceDirectory(), sourceDirectory);
        assertCollectorState(0, 0, 0, collector);
    }

    @Test
    public void shouldInterpolateUnprefixedBasedirExpression() throws Exception {
        Path basedir = Paths.get("/test/path");
        Model model = Model.newBuilder()
                .dependencies(Collections.singletonList(Dependency.newBuilder()
                        .systemPath("${basedir}/artifact.jar")
                        .build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model result = interpolator.interpolateModel(
                model, basedir, createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        List<Dependency> rDeps = result.getDependencies();
        assertNotNull(rDeps);
        assertEquals(1, rDeps.size());
        assertEquals(
                basedir.resolve("artifact.jar").toAbsolutePath(),
                Paths.get(rDeps.get(0).getSystemPath()).toAbsolutePath());
    }

    @Test
    public void testRecursiveExpressionCycleNPE() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("aa", "${bb}");
        props.put("bb", "${aa}");

        Model model = Model.newBuilder().properties(props).build();

        SimpleProblemCollector collector = new SimpleProblemCollector();

        ModelBuilderRequest request = createModelBuildingRequest(Map.of()).build();
        interpolator.interpolateModel(model, null, request, collector);

        assertCollectorState(0, 2, 0, collector);
        assertTrue(collector.getErrors().get(0).contains("recursive variable reference"));
    }

    @Disabled("per def cannot be recursive: ${basedir} is immediately going for project.basedir")
    @Test
    public void testRecursiveExpressionCycleBaseDir() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("basedir", "${basedir}");
        ModelBuilderRequest request = createModelBuildingRequest(Map.of()).build();

        Model model = Model.newBuilder().properties(props).build();

        SimpleProblemCollector collector = new SimpleProblemCollector();
        ModelInterpolator interpolator = this.interpolator;
        interpolator.interpolateModel(model, null, request, collector);

        assertCollectorState(0, 1, 0, collector);
        assertEquals(
                "recursive variable reference: basedir", collector.getErrors().get(0));
    }

    @Test
    void shouldIgnorePropertiesWithPomPrefix() throws Exception {
        final String orgName = "MyCo";
        final String uninterpolatedName = "${pom.organization.name} Tools";

        Model model = Model.newBuilder()
                .name(uninterpolatedName)
                .organization(Organization.newBuilder().name(orgName).build())
                .build();

        SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model,
                null,
                createModelBuildingRequest(context).build(),
                // .validationLevel(ModelBuilderRequest.VALIDATION_LEVEL_MAVEN_4_0),
                collector);

        assertCollectorState(0, 0, 0, collector);
        assertEquals(uninterpolatedName, out.getName());
    }

    @Provides
    @Priority(10)
    @SuppressWarnings("unused")
    RootLocator testRootLocator() {
        return new RootLocator() {
            @Override
            public Path findRoot(Path basedir) {
                return rootDirectory != null ? rootDirectory.get() : basedir;
            }

            @Override
            public Path findMandatoryRoot(Path basedir) {
                return Optional.ofNullable(findRoot(basedir))
                        .orElseThrow(() -> new IllegalStateException(getNoRootMessage()));
            }
        };
    }

    @Test
    @DisabledOnOs(WINDOWS)
    void testProjectPropertyExtraction() throws Exception {
        Path projectDir = Paths.get("/test/path");
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertEquals("/test/path", interpolator.projectProperty(model, projectDir, "basedir", false));
        assertNull(interpolator.projectProperty(model, projectDir, "nonexistent.property", false));
    }

    @Test
    @EnabledOnOs(WINDOWS)
    void testProjectPropertyExtractionOnWindows() throws Exception {
        Path projectDir = Paths.get("/test/path");
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertEquals("D:\\test\\path", interpolator.projectProperty(model, projectDir, "basedir", false));
        assertNull(interpolator.projectProperty(model, projectDir, "nonexistent.property", false));
    }

    @Test
    void testPathHandling() {
        Path projectDir = Jimfs.newFileSystem(Configuration.windows()).getPath("test/path");
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .directory("${project.basedir}\\target")
                        .build())
                .build();
        Model out = interpolator.interpolateModel(
                model,
                projectDir,
                createModelBuildingRequest(Collections.emptyMap()).build(),
                new SimpleProblemCollector());
        assertEquals("C:\\work\\test\\path\\target", out.getBuild().getDirectory());
    }

    @Test
    void testWindowsPathHandling() {
        Path projectDir = Jimfs.newFileSystem(Configuration.windows()).getPath("C:\\test\\path");
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .directory("${project.basedir}\\target")
                        .build())
                .build();
        Model out = interpolator.interpolateModel(
                model,
                projectDir,
                createModelBuildingRequest(Collections.emptyMap()).build(),
                new SimpleProblemCollector());
        String expected = projectDir.resolve("target").toString();
        assertEquals(expected, out.getBuild().getDirectory());
    }

    @Test
    void testBasedirPropertyExtraction() throws Exception {
        Path testDir = Paths.get("/test/path").toAbsolutePath();
        assertEquals(
                testDir.toString(),
                new DefaultModelInterpolator(
                                new DefaultPathTranslator(),
                                new DefaultUrlNormalizer(),
                                new DefaultRootLocator(),
                                new DefaultInterpolator())
                        .projectProperty(Model.newBuilder().build(), testDir, "basedir", false));
    }

    @Test
    void testBasedirPrefixedPropertyExtraction() throws Exception {
        Path testDir = Paths.get("/test/path").toAbsolutePath();
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertEquals(
                testDir.getParent().toString(), interpolator.projectProperty(model, testDir, "basedir.parent", false));
        assertEquals(
                testDir.getFileName().toString(),
                interpolator.projectProperty(model, testDir, "basedir.fileName", false));
    }

    @Test
    void testBasedirPropertyWithNullProjectDir() throws Exception {
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertNull(interpolator.projectProperty(model, null, "basedir", false));
        assertNull(interpolator.projectProperty(model, null, "basedir.any", false));
    }

    @Test
    void testBaseUriPropertyExtraction() throws Exception {
        Path testDir = Paths.get("/test/path").toAbsolutePath();
        URI testUri = testDir.toUri();
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertEquals(testUri.toASCIIString(), interpolator.projectProperty(model, testDir, "baseUri", true));
    }

    @Test
    void testBaseUriPropertyWithNullProjectDir() throws Exception {
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertNull(interpolator.projectProperty(model, null, "baseUri", true));
        assertNull(interpolator.projectProperty(model, null, "baseUri.any", true));
    }

    @Test
    void testBaseUriPropertyWithoutPrefix() throws Exception {
        Path testDir = Paths.get("/test/path").toAbsolutePath();
        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertNull(interpolator.projectProperty(model, testDir, "baseUri", false));
        assertNull(interpolator.projectProperty(model, testDir, "baseUri.path", false));
    }

    @Test
    void testBuildTimestampInterpolation() {
        Map<String, String> props = new HashMap<>();
        props.put("maven.build.timestamp.format", "yyyy-MM-dd");

        Model model = Model.newBuilder().properties(props).build();
        ModelBuilderRequest request = createModelBuildingRequest(Collections.emptyMap())
                .session(session)
                .build();

        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());

        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertEquals(expectedDate, interpolator.doCallback(model, null, request, "build.timestamp"));
        assertEquals(expectedDate, interpolator.doCallback(model, null, request, "maven.build.timestamp"));
    }

    @Test
    void testBuildTimestampWithDefaultFormat() {
        assertThat(Math.abs(Instant.parse(new DefaultModelInterpolator(
                                                new DefaultPathTranslator(),
                                                new DefaultUrlNormalizer(),
                                                new DefaultRootLocator(),
                                                new DefaultInterpolator())
                                        .doCallback(
                                                Model.newBuilder().build(),
                                                null,
                                                createModelBuildingRequest(Collections.emptyMap())
                                                        .session(session)
                                                        .build(),
                                                "build.timestamp"))
                                .getEpochSecond()
                        - Instant.now().getEpochSecond()))
                .isLessThan(3);
    }

    @Test
    void testNonTimestampExpressions() {
        Model model = Model.newBuilder().build();
        ModelBuilderRequest request = createModelBuildingRequest(Collections.emptyMap())
                .session(session)
                .build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());
        assertNull(interpolator.doCallback(model, null, request, "not.a.timestamp"));
        assertNull(interpolator.doCallback(model, null, request, "build.something.else"));
    }

    @Test
    void testBaseUriPropertyExtractionWithReflection() throws Exception {
        Path testDir = Paths.get("/test/path").toAbsolutePath();
        URI testUri = testDir.toUri();

        Model model = Model.newBuilder().build();
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());

        // Test direct baseUri access
        assertEquals(testUri.toASCIIString(), interpolator.projectProperty(model, testDir, "baseUri", true));

        // Test URI path component
        assertEquals(testUri.getPath(), interpolator.projectProperty(model, testDir, "baseUri.path", true));

        // Test URI scheme component
        assertEquals(testUri.getScheme(), interpolator.projectProperty(model, testDir, "baseUri.scheme", true));

        // Test URI host component
        assertEquals(testUri.getHost(), interpolator.projectProperty(model, testDir, "baseUri.host", true));

        // Test non-existent URI property
        assertNull(interpolator.projectProperty(model, testDir, "baseUri.nonexistent", true));

        // Test with null project fs
        assertNull(interpolator.projectProperty(model, null, "baseUri.path", true));
    }

    @Test
    void testUrlNormalizationThroughInterpolation() {
        // Setup
        DefaultUrlNormalizer urlNormalizer = new DefaultUrlNormalizer();
        DefaultPathTranslator pathTranslator = new DefaultPathTranslator();
        DefaultRootLocator rootLocator = new DefaultRootLocator();
        DefaultInterpolator interpolator = new DefaultInterpolator();

        DefaultModelInterpolator modelInterpolator =
                new DefaultModelInterpolator(pathTranslator, urlNormalizer, rootLocator, interpolator);

        Path projectDir = Paths.get("/test/path");
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(BUILD_PROJECT)
                .build();

        SimpleProblemCollector problems = new SimpleProblemCollector();

        // Test project.url normalization
        Model urlModel = Model.newBuilder().url("http://example.com/../path").build();
        Model interpolatedUrlModel = modelInterpolator.interpolateModel(urlModel, projectDir, request, problems);
        assertEquals("http://example.com/../path", interpolatedUrlModel.getUrl());

        // Test project.scm.url normalization
        Model scmModel = Model.newBuilder()
                .scm(Scm.newBuilder().url("http://example.com/scm/../path").build())
                .build();
        Model interpolatedScmModel = modelInterpolator.interpolateModel(scmModel, projectDir, request, problems);
        assertEquals(
                "http://example.com/scm/../path", interpolatedScmModel.getScm().getUrl());

        // Test project.scm.connection normalization
        Model scmConnectionModel = Model.newBuilder()
                .scm(Scm.newBuilder()
                        .connection("scm:git:http://example.com/repo/../path.git")
                        .build())
                .build();
        Model interpolatedScmConnectionModel =
                modelInterpolator.interpolateModel(scmConnectionModel, projectDir, request, problems);
        assertEquals(
                "scm:git:http://example.com/repo/../path.git",
                interpolatedScmConnectionModel.getScm().getConnection());

        // Test project.distributionManagement.site.url normalization
        Model siteModel = Model.newBuilder()
                .distributionManagement(DistributionManagement.newBuilder()
                        .site(Site.newBuilder()
                                .url("http://example.com/site/../path")
                                .build())
                        .build())
                .build();
        Model interpolatedSiteModel = modelInterpolator.interpolateModel(siteModel, projectDir, request, problems);
        assertEquals(
                "http://example.com/site/../path",
                interpolatedSiteModel.getDistributionManagement().getSite().getUrl());

        // Verify no problems were reported
        assertTrue(problems.getErrors().isEmpty());
        assertTrue(problems.getWarnings().isEmpty());
        assertTrue(problems.getFatals().isEmpty());
    }

    @Test
    void testProjectPrefixes31Interpolation() {
        // Setup
        DefaultModelInterpolator interpolator = new DefaultModelInterpolator(
                new DefaultPathTranslator(),
                new DefaultUrlNormalizer(),
                new DefaultRootLocator(),
                new DefaultInterpolator());

        Path projectDir = Paths.get("/test/path");

        // Create request with RequestType that should use PROJECT_PREFIXES_3_1
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_CONSUMER) // Use PROJECT instead of OTHER
                .build();

        SimpleProblemCollector problems = new SimpleProblemCollector();

        // Create model with properties to test both pom. and project. prefixes
        Model model = Model.newBuilder()
                .version("1.0")
                .artifactId("test-artifact")
                .properties(Map.of(
                        "testProp", "${pom.version}",
                        "testProp2", "${project.version}",
                        "testProp3", "${pom.artifactId}",
                        "testProp4", "${project.artifactId}"))
                .build();

        // Execute interpolation
        Model result = interpolator.interpolateModel(model, projectDir, request, problems);

        // Verify both pom. and project. prefixes work
        assertEquals("1.0", result.getProperties().get("testProp"));
        assertEquals("1.0", result.getProperties().get("testProp2"));
        assertEquals("test-artifact", result.getProperties().get("testProp3"));
        assertEquals("test-artifact", result.getProperties().get("testProp4"));

        // Verify no problems were reported
        assertTrue(problems.getErrors().isEmpty());
        assertTrue(problems.getWarnings().isEmpty());
        assertTrue(problems.getFatals().isEmpty());

        // Verify the correct prefixes list is used
        assertEquals(Arrays.asList("pom.", "project."), interpolator.getProjectPrefixes(request));
    }

    private static final Set<String> URL_EXPRESSIONS = Set.of(
            "project.url",
            "project.scm.url",
            "project.scm.connection",
            "project.scm.developerConnection",
            "project.distributionManagement.site.url");

    @Test
    void testUrlNormalizationInPostProcess() throws Exception {
        PathTranslator mockPathTranslator = new DefaultPathTranslator();
        UrlNormalizer mockUrlNormalizer = spy(new DefaultUrlNormalizer());
        RootLocator mockRootLocator = new DefaultRootLocator();
        Interpolator mockInterpolator = new DefaultInterpolator();

        // Create test instance with mocks
        DefaultModelInterpolator interpolator =
                new DefaultModelInterpolator(mockPathTranslator, mockUrlNormalizer, mockRootLocator, mockInterpolator);

        // Prepare test data
        Path projectDir = Paths.get("/test/path");
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();

        // Test URL expressions that should be normalized
        for (String urlExpression : URL_EXPRESSIONS) {
            String testValue = "http://example.com/../test";
            String expectedNormalized = "http://example.com/test";

            // Configure mock to return normalized value
            when(mockUrlNormalizer.normalize(testValue)).thenReturn(expectedNormalized);

            // Call private method using reflection
            Method postProcess = DefaultModelInterpolator.class.getDeclaredMethod(
                    "postProcess", Path.class, ModelBuilderRequest.class, String.class, String.class);
            postProcess.setAccessible(true);

            String result = (String) postProcess.invoke(interpolator, projectDir, request, urlExpression, testValue);

            assertEquals(expectedNormalized, result);
        }

        // Test non-URL expression that should NOT be normalized
        String nonUrlExpression = "project.name";
        String nonUrlValue = "http://example.com/../should-not-normalize";

        Method postProcess = DefaultModelInterpolator.class.getDeclaredMethod(
                "postProcess", Path.class, ModelBuilderRequest.class, String.class, String.class);
        postProcess.setAccessible(true);

        String result = (String) postProcess.invoke(interpolator, projectDir, request, nonUrlExpression, nonUrlValue);

        // Verify normalization was NOT called
        verify(mockUrlNormalizer, never()).normalize(nonUrlValue);
        assertEquals(nonUrlValue, result);
    }
}
