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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 */
class DefaultModelInterpolatorTest {

    Map<String, String> context;
    ModelInterpolator interpolator;
    Session session;
    AtomicReference<Path> rootDirectory; // used in TestRootLocator below

    @BeforeEach
    void setUp() {
        context = new HashMap<>();
        context.put("basedir", "myBasedir");
        context.put("anotherdir", "anotherBasedir");
        context.put("project.baseUri", "myBaseUri");

        session = ApiRunner.createSession(injector ->
            injector.bindInstance(DefaultModelInterpolatorTest.class, this));
        interpolator = session.getService(Lookup.class).lookup(DefaultModelInterpolator.class);
    }

    protected void assertProblemFree(SimpleProblemCollector collector) {
        assertThat(collector.getErrors().size()).as("Expected no errors").isEqualTo(0);
        assertThat(collector.getWarnings().size()).as("Expected no warnings").isEqualTo(0);
        assertThat(collector.getFatals().size()).as("Expected no fatals").isEqualTo(0);
    }

    @SuppressWarnings("SameParameterValue")
    protected void assertCollectorState(
            int numFatals, int numErrors, int numWarnings, SimpleProblemCollector collector) {
        assertThat(collector.getErrors().size()).as("Errors").isEqualTo(numErrors);
        assertThat(collector.getWarnings().size()).as("Warnings").isEqualTo(numWarnings);
        assertThat(collector.getFatals().size()).as("Fatals").isEqualTo(numFatals);
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
    void defaultBuildTimestampFormatShouldFormatTimeIn24HourFormat() {
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

        assertThat(format.format(firstTestDate)).isEqualTo("1976-11-11T00:16:00Z");
        assertThat(format.format(secondTestDate)).isEqualTo("1976-11-11T23:16:00Z");
    }

    @Test
    void defaultBuildTimestampFormatWithLocalTimeZoneMidnightRollover() {
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
        assertThat(format.format(firstTestDate)).isEqualTo("2014-06-15T23:16:00Z");
        assertThat(format.format(secondTestDate)).isEqualTo("2014-11-16T00:16:00Z");
    }

    @Test
    void shouldNotThrowExceptionOnReferenceToNonExistentValue() throws Exception {
        Scm scm = Scm.newBuilder().connection("${test}/somepath").build();
        Model model = Model.newBuilder().scm(scm).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);

        assertProblemFree(collector);
        assertThat(out.getScm().getConnection()).isEqualTo("${test}/somepath");
    }

    @Test
    void shouldThrowExceptionOnRecursiveScmConnectionReference() throws Exception {
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
    void shouldNotThrowExceptionOnReferenceToValueContainingNakedExpression() throws Exception {
        Scm scm = Scm.newBuilder().connection("${test}/somepath").build();
        Map<String, String> props = new HashMap<>();
        props.put("test", "test");
        Model model = Model.newBuilder().scm(scm).properties(props).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);

        assertProblemFree(collector);

        assertThat(out.getScm().getConnection()).isEqualTo("test/somepath");
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

        assertThat(out.getName()).isEqualTo(orgName + " Tools");
    }

    @Test
    void shouldInterpolateDependencyVersionToSetSameAsProjectVersion() throws Exception {
        Model model = Model.newBuilder()
                .version("3.8.1")
                .dependencies(Collections.singletonList(
                        Dependency.newBuilder().version("${project.version}").build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertCollectorState(0, 0, 0, collector);

        assertThat((out.getDependencies().get(0)).getVersion()).isEqualTo("3.8.1");
    }

    @Test
    void shouldNotInterpolateDependencyVersionWithInvalidReference() throws Exception {
        Model model = Model.newBuilder()
                .version("3.8.1")
                .dependencies(Collections.singletonList(
                        Dependency.newBuilder().version("${something}").build()))
                .build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertThat((out.getDependencies().get(0)).getVersion()).isEqualTo("${something}");
    }

    @Test
    void twoReferences() throws Exception {
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

        assertThat((out.getDependencies().get(0)).getVersion()).isEqualTo("foo-3.8.1");
    }

    @Test
    void property() throws Exception {
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

        assertThat((out.getRepositories().get(0)).getUrl()).isEqualTo("file://localhost/anotherBasedir/temp-repo");
    }

    @Test
    void basedirUnx() throws Exception {
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

        assertThat((out.getRepositories().get(0)).getUrl()).isEqualTo(projectBasedir.toAbsolutePath() + "/temp-repo");
    }

    @Test
    void basedirWin() throws Exception {
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

        assertThat((out.getRepositories().get(0)).getUrl()).isEqualTo(projectBasedir.toAbsolutePath() + "/temp-repo");
    }

    @Test
    void baseUri() throws Exception {
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

        assertThat((out.getRepositories().get(0)).getUrl()).isEqualTo(projectBasedir.resolve("temp-repo").toUri().toString());
    }

    @Test
    void rootDirectory() throws Exception {
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

        assertThat((out.getRepositories().get(0)).getUrl()).isEqualTo("file:myRootDirectory/temp-repo");
    }

    @Test
    void rootDirectoryWithUri() throws Exception {
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

        assertThat((out.getRepositories().get(0)).getUrl()).isEqualTo(rootDirectory.resolve("temp-repo").toUri().toString());
    }

    @Test
    void rootDirectoryWithNull() throws Exception {
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
        IllegalStateException e = assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> interpolator.interpolateModel(
                model,
                projectDirectory,
                createModelBuildingRequest(context).build(),
                collector)).actual();

        assertThat(e.getMessage()).isEqualTo(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
    }

    @Test
    void envars() throws Exception {
        context.put("env.HOME", "/path/to/home");

        Map<String, String> modelProperties = new HashMap<>();
        modelProperties.put("outputDirectory", "${env.HOME}");

        Model model = Model.newBuilder().properties(modelProperties).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertThat(out.getProperties().get("outputDirectory")).isEqualTo("/path/to/home");
    }

    @Test
    void envarExpressionThatEvaluatesToNullReturnsTheLiteralString() throws Exception {

        Map<String, String> modelProperties = new HashMap<>();
        modelProperties.put("outputDirectory", "${env.DOES_NOT_EXIST}");

        Model model = Model.newBuilder().properties(modelProperties).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertThat(out.getProperties().get("outputDirectory")).isEqualTo("${env.DOES_NOT_EXIST}");
    }

    @Test
    void expressionThatEvaluatesToNullReturnsTheLiteralString() throws Exception {
        Map<String, String> modelProperties = new HashMap<>();
        modelProperties.put("outputDirectory", "${DOES_NOT_EXIST}");

        Model model = Model.newBuilder().properties(modelProperties).build();

        final SimpleProblemCollector collector = new SimpleProblemCollector();
        Model out = interpolator.interpolateModel(
                model, Paths.get("."), createModelBuildingRequest(context).build(), collector);
        assertProblemFree(collector);

        assertThat(out.getProperties().get("outputDirectory")).isEqualTo("${DOES_NOT_EXIST}");
    }

    @Test
    void shouldInterpolateSourceDirectoryReferencedFromResourceDirectoryCorrectly() throws Exception {
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

        assertThat(resIt.next().getDirectory()).isEqualTo(model.getBuild().getSourceDirectory());
    }

    @Test
    void shouldInterpolateUnprefixedBasedirExpression() throws Exception {
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
        assertThat(rDeps).isNotNull();
        assertThat(rDeps.size()).isEqualTo(1);
        assertThat(Paths.get(rDeps.get(0).getSystemPath()).toAbsolutePath()).isEqualTo(basedir.resolve("artifact.jar").toAbsolutePath());
    }

    @Test
    void recursiveExpressionCycleNPE() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("aa", "${bb}");
        props.put("bb", "${aa}");

        Model model = Model.newBuilder().properties(props).build();

        SimpleProblemCollector collector = new SimpleProblemCollector();

        ModelBuilderRequest request = createModelBuildingRequest(Map.of()).build();
        interpolator.interpolateModel(model, null, request, collector);

        assertCollectorState(0, 2, 0, collector);
        assertThat(collector.getErrors().get(0).contains("recursive variable reference")).isTrue();
    }

    @Disabled("per def cannot be recursive: ${basedir} is immediately going for project.basedir")
    @Test
    void recursiveExpressionCycleBaseDir() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("basedir", "${basedir}");
        ModelBuilderRequest request = createModelBuildingRequest(Map.of()).build();

        Model model = Model.newBuilder().properties(props).build();

        SimpleProblemCollector collector = new SimpleProblemCollector();
        ModelInterpolator interpolator = this.interpolator;
        interpolator.interpolateModel(model, null, request, collector);

        assertCollectorState(0, 1, 0, collector);
        assertThat(collector.getErrors().get(0)).isEqualTo("recursive variable reference: basedir");
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
        assertThat(out.getName()).isEqualTo(uninterpolatedName);
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
