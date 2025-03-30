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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Organization;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
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
        Model model = Model.newBuilder()
                .build(Build.newBuilder()
                        .sourceDirectory("correct")
                        .resources(List.of(Resource.newBuilder()
                                .directory("${project.build.sourceDirectory}")
                                .build()))
                        .build())
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, null, createModelBuildingRequest(context).build(), collector);
        assertCollectorState(0, 0, 0, collector);

        List<Resource> outResources = out.getBuild().getResources();
        Iterator<Resource> resIt = outResources.iterator();

        assertEquals(model.getBuild().getSourceDirectory(), resIt.next().getDirectory());
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
}
