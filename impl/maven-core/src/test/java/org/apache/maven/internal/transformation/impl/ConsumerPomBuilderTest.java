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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.Sources;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.impl.DefaultArtifactCoordinatesFactory;
import org.apache.maven.impl.DefaultDependencyCoordinatesFactory;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.cache.DefaultRequestCacheFactory;
import org.apache.maven.impl.resolver.MavenVersionScheme;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.transformation.AbstractRepositoryTestCase;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerPomBuilderTest extends AbstractRepositoryTestCase {

    @Inject
    PomBuilder builder;

    @Inject
    ModelBuilder modelBuilder;

    @Override
    protected List<Object> getSessionServices() {
        List<Object> services = new ArrayList<>(super.getSessionServices());

        DependencyResolver dependencyResolver = Mockito.mock(DependencyResolver.class);
        DependencyResolverResult resolverResult = Mockito.mock(DependencyResolverResult.class);
        Mockito.when(dependencyResolver.collect(
                        Mockito.any(Session.class),
                        Mockito.any(DependencyCoordinates.class),
                        Mockito.any(PathScope.class)))
                .thenReturn(resolverResult);
        Node node = Mockito.mock(Node.class);
        Mockito.when(resolverResult.getRoot()).thenReturn(node);
        Node child = Mockito.mock(Node.class);
        Mockito.when(node.getChildren()).thenReturn(List.of(child));

        services.addAll(List.of(
                new DefaultRequestCacheFactory(),
                new DefaultArtifactCoordinatesFactory(),
                new DefaultDependencyCoordinatesFactory(),
                new DefaultVersionParser(new DefaultModelVersionParser(new MavenVersionScheme())),
                dependencyResolver));
        return services;
    }

    /**
     * Configures {@link #session} with the root directory of a test in {@code src/test/resources/consumer}.
     * Returns the request in case the caller wants to apply more configuration.
     */
    private MavenExecutionRequest setRootDirectory(String test) {
        MavenExecutionRequest request = InternalMavenSession.from(InternalSession.from(session))
                .getMavenSession()
                .getRequest();
        request.setRootDirectory(Paths.get("src/test/resources/consumer", test));
        return request;
    }

    /**
     * Builds the effective model for the given {@code pom.xml} file.
     */
    private MavenProject getEffectiveModel(Path file) {
        ModelBuilder.ModelBuilderSession mbs = modelBuilder.newSession();
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), mbs);
        Model orgModel = mbs.build(ModelBuilderRequest.builder()
                        .session(InternalSession.from(session))
                        .source(Sources.buildSource(file))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .build())
                .getEffectiveModel();

        MavenProject project = new MavenProject(orgModel);
        project.setOriginalModel(new org.apache.maven.model.Model(orgModel));
        return project;
    }

    private Model getConsumerModel(Path file, boolean raw) {
        ModelBuilder.ModelBuilderSession mbs = modelBuilder.newSession();
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), mbs);
        var result = mbs.build(ModelBuilderRequest.builder()
                .session(InternalSession.from(session))
                .source(Sources.buildSource(file))
                .requestType(ModelBuilderRequest.RequestType.BUILD_CONSUMER)
                .build());
        return raw ? result.getRawModel() : result.getEffectiveModel();
    }

    @Test
    void testTrivialConsumer() throws Exception {
        setRootDirectory("trivial");
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = builder.build(session, project, Sources.buildSource(file));

        assertNotNull(model);
        assertNotNull(model.getDependencies());
    }

    @Test
    void testPackagingActivatedProfiles() throws Exception {
        setRootDirectory("packaging-profiles");
        Path file = Paths.get("src/test/resources/consumer/packaging-profiles/pom.xml");

        MavenProject project = getEffectiveModel(file);

        Model model = DefaultConsumerPomBuilder.transformNonPom(getConsumerModel(file, false), project);

        assertNotNull(model);

        assertEquals(1, model.getProfiles().size());
        org.apache.maven.api.model.Profile mixedProfile = model.getProfiles().get(0);
        assertEquals("mixed-profile", mixedProfile.getId());
        assertNotNull(mixedProfile.getActivation());
        assertNull(mixedProfile.getActivation().getPackaging());
        assertNotNull(mixedProfile.getActivation().getProperty());
        assertEquals("foo", mixedProfile.getActivation().getProperty().getName());

        assertNotNull(model.getDependencies());
        assertEquals(1, model.getDependencies().size());
        assertEquals("slf4j-api", model.getDependencies().get(0).getArtifactId());
    }

    @Test
    void testParentPomPackagingActivatedProfilesArePreserved() throws Exception {
        setRootDirectory("packaging-parent-profiles");
        Path file = Paths.get("src/test/resources/consumer/packaging-parent-profiles/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = DefaultConsumerPomBuilder.transformPom(getConsumerModel(file, true), project);

        assertEquals(1, model.getProfiles().size());
        Profile profile = model.getProfiles().get(0);
        assertEquals("jar-profile", profile.getId());
        assertEquals("jar", profile.getActivation().getPackaging());
        assertEquals("slf4j-api", profile.getDependencies().get(0).getArtifactId());
    }

    @Test
    void testBomPackagingActivatedProfilesArePreserved() throws Exception {
        setRootDirectory("packaging-bom-profiles");
        Path file = Paths.get("src/test/resources/consumer/packaging-bom-profiles/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = DefaultConsumerPomBuilder.transformBom(getConsumerModel(file, true), project);

        assertEquals(1, model.getProfiles().size());
        Profile profile = model.getProfiles().get(0);
        assertEquals("jar-profile", profile.getId());
        assertEquals("jar", profile.getActivation().getPackaging());
        assertEquals(
                "slf4j-api",
                profile.getDependencyManagement().getDependencies().get(0).getArtifactId());
    }

    @Test
    void testSimpleConsumer() throws Exception {
        MavenExecutionRequest request = setRootDirectory("simple");
        request.getUserProperties().setProperty("changelist", "MNG6957");
        Path file = Paths.get("src/test/resources/consumer/simple/simple-parent/simple-weather/pom.xml");

        MavenProject project = getEffectiveModel(file);
        request.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
        Model model = builder.build(session, project, Sources.buildSource(file));

        assertNotNull(model);
        assertFalse(model.getDependencies().isEmpty());
        assertTrue(model.getProfiles().isEmpty());
    }

    @Test
    void testMultiModuleConsumer() throws Exception {
        setRootDirectory("multi-module");
        Path file = Paths.get("src/test/resources/consumer/multi-module/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = builder.build(session, project, Sources.buildSource(file));

        assertNotNull(model);
        assertNull(model.getBuild());
        assertTrue(model.getDependencies().isEmpty());
        assertFalse(model.getDependencyManagement().getDependencies().isEmpty());
    }

    /**
     * Same test as {@link #testMultiModuleConsumer()}, but verifies that
     * {@code <build>} is preserved when {@code preserveModelVersion=true}.
     */
    @Test
    void testMultiModuleConsumerPreserveModelVersion() throws Exception {
        setRootDirectory("multi-module");
        Path file = Paths.get("src/test/resources/consumer/multi-module/pom.xml");

        MavenProject project = getEffectiveModel(file);
        Model model = getEffectiveModel(file).getModel().getDelegate();
        model = Model.newBuilder(model, true).preserveModelVersion(true).build();

        Model transformed = DefaultConsumerPomBuilder.transformPom(model, project);

        assertNotNull(transformed);
        assertNotNull(transformed.getBuild());
        assertTrue(transformed.getDependencies().isEmpty());
        assertFalse(transformed.getDependencyManagement().getDependencies().isEmpty());
    }

    @Test
    void testParentWithConditionsFailsConsumerPom() throws Exception {
        setRootDirectory("parent-with-conditions");
        Path file = Paths.get("src/test/resources/consumer/parent-with-conditions/pom.xml");

        MavenProject project = getEffectiveModel(file);
        // A parent POM with profile conditions cannot be downgraded to 4.0.0,
        // so building the consumer POM should fail with actionable guidance.
        MavenException ex =
                assertThrows(MavenException.class, () -> builder.build(session, project, Sources.buildSource(file)));
        assertTrue(ex.getMessage().contains("cannot be downgraded to model version 4.0.0"));
    }

    @Test
    void testScmInheritance() throws Exception {
        Model model = Model.newBuilder()
                .scm(Scm.newBuilder()
                        .connection("scm:git:https://github.com/apache/maven-project.git")
                        .developerConnection("scm:git:https://github.com/apache/maven-project.git")
                        .url("https://github.com/apache/maven-project")
                        .childScmConnectionInheritAppendPath("true")
                        .childScmUrlInheritAppendPath("true")
                        .childScmDeveloperConnectionInheritAppendPath("true")
                        .build())
                .build();
        Model transformed = DefaultConsumerPomBuilder.transformNonPom(model, null);
        assertNull(transformed.getScm().getChildScmConnectionInheritAppendPath());
        assertNull(transformed.getScm().getChildScmUrlInheritAppendPath());
        assertNull(transformed.getScm().getChildScmDeveloperConnectionInheritAppendPath());
    }

    /**
     * Verifies that the consumer POM builder passes the project's remote repositories
     * to the model builder request, so that BOM imports from non-central repositories
     * (e.g. repositories defined in settings.xml profiles) can be resolved.
     * <p>
     * Without the fix in {@code DefaultConsumerPomBuilder.buildModel()}, the
     * {@code ModelBuilderRequest} is constructed without repositories, profiles, or
     * active profile IDs. This causes the model builder to only see Maven Central
     * when resolving BOM imports, leading to "Non-resolvable import POM" failures
     * for artifacts hosted in private/corporate repositories.
     */
    @Test
    void testConsumerPomPassesProjectRepositoriesToModelBuilder() throws Exception {
        setRootDirectory("trivial");
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");

        MavenProject project = getEffectiveModel(file);

        // Add a custom remote repository to the project, simulating a repository
        // injected from settings.xml profile (e.g. a corporate/private repository)
        RemoteRepository customRepo =
                new RemoteRepository.Builder("custom-repo", "default", "https://repo.example.com/maven2").build();
        project.getRemoteProjectRepositories().add(customRepo);

        // Spy on the ModelBuilderSession to capture the ModelBuilderRequest
        ModelBuilder.ModelBuilderSession originalMbs = modelBuilder.newSession();
        ModelBuilder.ModelBuilderSession spyMbs = Mockito.spy(originalMbs);
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), spyMbs);

        // Build the consumer POM
        builder.build(session, project, Sources.buildSource(file));

        // Capture the ModelBuilderRequest passed to the ModelBuilderSession
        ArgumentCaptor<ModelBuilderRequest> requestCaptor = ArgumentCaptor.forClass(ModelBuilderRequest.class);
        Mockito.verify(spyMbs, Mockito.atLeastOnce()).build(requestCaptor.capture());

        // Find the BUILD_CONSUMER request (there may be multiple calls)
        ModelBuilderRequest consumerRequest = requestCaptor.getAllValues().stream()
                .filter(r -> r.getRequestType() == ModelBuilderRequest.RequestType.BUILD_CONSUMER)
                .findFirst()
                .orElse(null);

        assertNotNull(consumerRequest, "Expected a BUILD_CONSUMER request to be made");

        // Verify that repositories were passed to the request.
        // Without the fix, getRepositories() returns null because buildModel() never sets them.
        assertNotNull(
                consumerRequest.getRepositories(),
                "Consumer POM model builder request should include repositories from the project. "
                        + "Without this, BOM imports from non-central repositories (e.g. settings.xml profiles) "
                        + "cannot be resolved, causing 'Non-resolvable import POM' errors.");
        assertFalse(
                consumerRequest.getRepositories().isEmpty(),
                "Consumer POM model builder request should have at least one repository");

        // Verify the custom repository is included
        boolean hasCustomRepo =
                consumerRequest.getRepositories().stream().anyMatch(r -> "custom-repo".equals(r.getId()));
        assertTrue(hasCustomRepo, "Consumer POM model builder request should include the project's custom repository");
    }

    @Test
    void testInlinePackagingActivatedProfiles() {
        Dependency dep1 = Dependency.newBuilder()
                .groupId("g")
                .artifactId("a1")
                .version("1")
                .build();
        Dependency dep2 = Dependency.newBuilder()
                .groupId("g")
                .artifactId("a2")
                .version("2")
                .build();

        Profile profileMatching = Profile.newBuilder()
                .id("matching")
                .activation(Activation.newBuilder().packaging("jar").build())
                .dependencies(List.of(dep1))
                .build();

        Profile profileNotMatching = Profile.newBuilder()
                .id("not-matching")
                .activation(Activation.newBuilder().packaging("war").build())
                .dependencies(List.of(dep2))
                .build();

        Profile profileMixed = Profile.newBuilder()
                .id("mixed")
                .activation(Activation.newBuilder().packaging("jar").jdk("11").build())
                .dependencies(List.of(dep2))
                .build();

        Model model = Model.newBuilder()
                .packaging("jar")
                .profiles(List.of(profileMatching, profileNotMatching, profileMixed))
                .build();

        Model transformed = DefaultConsumerPomBuilder.inlinePackagingActivatedProfiles(model, "jar");

        // matching profile should be removed and its deps added to model
        // not-matching profile should be dropped entirely since its packaging condition can never be met
        // mixed profile should be kept but packaging activation stripped
        assertEquals(1, transformed.getProfiles().size());

        assertEquals("mixed", transformed.getProfiles().get(0).getId());
        assertNull(transformed.getProfiles().get(0).getActivation().getPackaging());
        assertEquals("11", transformed.getProfiles().get(0).getActivation().getJdk());

        assertEquals(1, transformed.getDependencies().size());
        assertEquals("a1", transformed.getDependencies().get(0).getArtifactId());
    }

    @Test
    void testInlinePackagingActivatedProfilesDependencyManagement() {
        Dependency managedDep = Dependency.newBuilder()
                .groupId("g")
                .artifactId("managed1")
                .version("1.0")
                .build();

        Profile profileWithDM = Profile.newBuilder()
                .id("dm-profile")
                .activation(Activation.newBuilder().packaging("jar").build())
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(Collections.singletonList(managedDep))
                        .build())
                .build();

        Model model = Model.newBuilder()
                .packaging("jar")
                .profiles(List.of(profileWithDM))
                .build();

        Model transformed = DefaultConsumerPomBuilder.inlinePackagingActivatedProfiles(model, "jar");

        // Profile should be removed
        assertTrue(transformed.getProfiles().isEmpty());

        // Managed dependency should be inlined
        assertNotNull(transformed.getDependencyManagement());
        assertEquals(1, transformed.getDependencyManagement().getDependencies().size());
        assertEquals(
                "managed1",
                transformed.getDependencyManagement().getDependencies().get(0).getArtifactId());
    }

    @Test
    void testInlinePackagingActivatedProfilesRepositories() {
        Repository repo = Repository.newBuilder()
                .id("custom-repo")
                .url("https://repo.example.com/maven2")
                .build();

        Profile profileWithRepo = Profile.newBuilder()
                .id("repo-profile")
                .activation(Activation.newBuilder().packaging("jar").build())
                .repositories(Collections.singletonList(repo))
                .build();

        Model model = Model.newBuilder()
                .packaging("jar")
                .profiles(List.of(profileWithRepo))
                .build();

        Model transformed = DefaultConsumerPomBuilder.inlinePackagingActivatedProfiles(model, "jar");

        // Profile should be removed
        assertTrue(transformed.getProfiles().isEmpty());

        // Repository should be inlined
        assertEquals(1, transformed.getRepositories().size());
        assertEquals("custom-repo", transformed.getRepositories().get(0).getId());
    }

    @Test
    void testInlinePackagingActivatedProfilesDeduplication() {
        Dependency existingDep = Dependency.newBuilder()
                .groupId("g")
                .artifactId("a1")
                .version("1.0")
                .build();

        // Profile re-declares the same dependency with a different version
        Dependency duplicateDep = Dependency.newBuilder()
                .groupId("g")
                .artifactId("a1")
                .version("2.0")
                .build();

        Profile profileWithDuplicate = Profile.newBuilder()
                .id("dup-profile")
                .activation(Activation.newBuilder().packaging("jar").build())
                .dependencies(List.of(duplicateDep))
                .build();

        Model model = Model.newBuilder()
                .packaging("jar")
                .dependencies(List.of(existingDep))
                .profiles(List.of(profileWithDuplicate))
                .build();

        Model transformed = DefaultConsumerPomBuilder.inlinePackagingActivatedProfiles(model, "jar");

        // Should NOT have duplicates — existing model dependency takes precedence
        assertEquals(1, transformed.getDependencies().size());
        assertEquals(
                "1.0",
                transformed.getDependencies().get(0).getVersion(),
                "Existing model dependency should take precedence over profile duplicate");
    }
}
