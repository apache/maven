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
import java.util.Map;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationOS;
import org.apache.maven.api.model.ActivationProperty;
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
    void testBomPackagingActivatedProfilesArePreserved() {
        // Build the model directly since 'bom' packaging is not supported
        // by the model builder on the 4.0.x branch
        Dependency managedDep = Dependency.newBuilder()
                .groupId("org.slf4j")
                .artifactId("slf4j-api")
                .version("2.0.0")
                .build();

        Profile jarProfile = Profile.newBuilder()
                .id("jar-profile")
                .activation(Activation.newBuilder().packaging("jar").build())
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(managedDep))
                        .build())
                .build();

        Model model = Model.newBuilder()
                .groupId("org.my.group")
                .artifactId("packaging-bom-profiles-test")
                .version("1.0.0-SNAPSHOT")
                .packaging("pom")
                .profiles(List.of(jarProfile))
                .build();

        Model transformed = DefaultConsumerPomBuilder.transformBom(model, new MavenProject(model));

        assertEquals(1, transformed.getProfiles().size());
        Profile profile = transformed.getProfiles().get(0);
        assertEquals("jar-profile", profile.getId());
        assertEquals("jar", profile.getActivation().getPackaging());
        assertEquals(
                "slf4j-api",
                profile.getDependencyManagement().getDependencies().get(0).getArtifactId());
    }

    @Test
    void testImportScopedManagedDepsAreFilteredFromInlinedProfiles() {
        // Verifies that import-scoped managed dependencies (BOM imports) inside
        // a packaging-activated profile are NOT re-added to the consumer POM.
        // Import-scoped entries are flattened during resolution and must not leak.
        Dependency bomImport = Dependency.newBuilder()
                .groupId("org.example")
                .artifactId("some-bom")
                .version("1.0.0")
                .type("pom")
                .scope("import")
                .build();

        Dependency regularManagedDep = Dependency.newBuilder()
                .groupId("org.slf4j")
                .artifactId("slf4j-api")
                .version("2.0.0")
                .build();

        Profile profile = Profile.newBuilder()
                .id("jar-profile")
                .activation(Activation.newBuilder().packaging("jar").build())
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(bomImport, regularManagedDep))
                        .build())
                .build();

        Model model = Model.newBuilder()
                .groupId("org.test")
                .artifactId("import-filter-test")
                .version("1.0.0-SNAPSHOT")
                .packaging("jar")
                .profiles(List.of(profile))
                .build();

        Model result = DefaultConsumerPomBuilder.inlinePackagingActivatedProfiles(model, "jar");

        // Profile should be inlined (removed)
        assertTrue(result.getProfiles().isEmpty());

        // Managed deps should contain only the regular dep, NOT the import-scoped BOM
        assertNotNull(result.getDependencyManagement());
        List<Dependency> managedDeps = result.getDependencyManagement().getDependencies();
        assertEquals(1, managedDeps.size());
        assertEquals("slf4j-api", managedDeps.get(0).getArtifactId());
        assertNull(managedDeps.get(0).getScope());
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

    /**
     * Verifies that the consumer POM builder injects properties from the project's
     * active profiles into the BUILD_CONSUMER request's user properties.
     * <p>
     * Without the fix in {@code DefaultConsumerPomBuilder.buildModel()}, the
     * {@code ModelBuilderRequest} only carries session user properties. When a
     * dependency version is defined via a profile property (e.g.
     * {@code <version>${my.version}</version>}), model validation would reject the
     * unresolved expression as an invalid version.
     * <p>
     * Session user properties ({@code -D} flags) must still take precedence over
     * profile-defined properties.
     */
    @Test
    void testConsumerPomIncludesActiveProfileProperties() throws Exception {
        setRootDirectory("trivial");
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");

        MavenProject project = getEffectiveModel(file);

        // Create an active profile with a property, simulating a profile that defines
        // a dependency version (the scenario fixed by MNG-8709).
        org.apache.maven.api.model.Profile apiProfile = org.apache.maven.api.model.Profile.newBuilder()
                .id("version-profile")
                .properties(Map.of("dep.version", "1.0.0"))
                .build();
        org.apache.maven.model.Profile modelProfile = new org.apache.maven.model.Profile(apiProfile);
        project.setActiveProfiles(List.of(modelProfile));

        // Spy on the ModelBuilderSession to capture the ModelBuilderRequest
        ModelBuilder.ModelBuilderSession originalMbs = modelBuilder.newSession();
        ModelBuilder.ModelBuilderSession spyMbs = Mockito.spy(originalMbs);
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), spyMbs);

        // Build the consumer POM
        builder.build(session, project, Sources.buildSource(file));

        // Capture the ModelBuilderRequest passed to the ModelBuilderSession
        ArgumentCaptor<ModelBuilderRequest> requestCaptor = ArgumentCaptor.forClass(ModelBuilderRequest.class);
        Mockito.verify(spyMbs, Mockito.atLeastOnce()).build(requestCaptor.capture());

        // Find the BUILD_CONSUMER request
        ModelBuilderRequest consumerRequest = requestCaptor.getAllValues().stream()
                .filter(r -> r.getRequestType() == ModelBuilderRequest.RequestType.BUILD_CONSUMER)
                .findFirst()
                .orElse(null);

        assertNotNull(consumerRequest, "Expected a BUILD_CONSUMER request to be made");

        // Verify that user properties contain the profile property
        Map<String, String> userProps = consumerRequest.getUserProperties();
        assertNotNull(userProps, "User properties should not be null");
        assertTrue(
                userProps.containsKey("dep.version"), "User properties should contain properties from active profiles");
        assertTrue(
                "1.0.0".equals(userProps.get("dep.version")),
                "Profile property 'dep.version' should have value '1.0.0'");
    }

    /**
     * Verifies that session user properties ({@code -D} flags) take precedence
     * over profile-defined properties in the BUILD_CONSUMER request.
     */
    @Test
    void testConsumerPomSessionPropertiesOverrideProfileProperties() throws Exception {
        MavenExecutionRequest execRequest = setRootDirectory("trivial");
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");

        MavenProject project = getEffectiveModel(file);

        // Create an active profile with a property
        org.apache.maven.api.model.Profile apiProfile = org.apache.maven.api.model.Profile.newBuilder()
                .id("version-profile")
                .properties(Map.of("dep.version", "1.0.0"))
                .build();
        org.apache.maven.model.Profile modelProfile = new org.apache.maven.model.Profile(apiProfile);
        project.setActiveProfiles(List.of(modelProfile));

        // Set the same property as a session user property (simulating -Ddep.version=2.0.0)
        execRequest.getUserProperties().setProperty("dep.version", "2.0.0");

        // Spy on the ModelBuilderSession to capture the ModelBuilderRequest
        ModelBuilder.ModelBuilderSession originalMbs = modelBuilder.newSession();
        ModelBuilder.ModelBuilderSession spyMbs = Mockito.spy(originalMbs);
        InternalSession.from(session).getData().set(SessionData.key(ModelBuilder.ModelBuilderSession.class), spyMbs);

        // Build the consumer POM
        builder.build(session, project, Sources.buildSource(file));

        // Capture the BUILD_CONSUMER request
        ArgumentCaptor<ModelBuilderRequest> requestCaptor = ArgumentCaptor.forClass(ModelBuilderRequest.class);
        Mockito.verify(spyMbs, Mockito.atLeastOnce()).build(requestCaptor.capture());

        ModelBuilderRequest consumerRequest = requestCaptor.getAllValues().stream()
                .filter(r -> r.getRequestType() == ModelBuilderRequest.RequestType.BUILD_CONSUMER)
                .findFirst()
                .orElse(null);

        assertNotNull(consumerRequest, "Expected a BUILD_CONSUMER request to be made");

        // Session user properties must override profile properties
        Map<String, String> userProps = consumerRequest.getUserProperties();
        assertTrue(
                "2.0.0".equals(userProps.get("dep.version")),
                "Session user property should override profile property; expected '2.0.0' but got '"
                        + userProps.get("dep.version") + "'");
    }

    // ── executable() condition stripping (GH-12570) ──────────────────────────

    /**
     * A profile whose only activation trigger is an {@code executable()} condition
     * should have its activation removed entirely after stripping.
     */
    @Test
    void testStripExecutableConditionsOnlyExecutableRemovesActivation() {
        Profile profile = Profile.newBuilder()
                .id("exec-only")
                .activation(Activation.newBuilder()
                        .condition("executable('musl-gcc')")
                        .build())
                .dependencies(List.of(Dependency.newBuilder()
                        .groupId("org.example")
                        .artifactId("some-lib")
                        .version("1.0")
                        .build()))
                .build();

        List<Profile> result = DefaultConsumerPomBuilder.stripExecutableConditions(List.of(profile));

        assertEquals(1, result.size());
        assertNull(result.get(0).getActivation(), "Activation should be null when executable() was the only trigger");
    }

    /**
     * When a profile has {@code executable()} in its condition but also has another
     * activation trigger (e.g. OS), the condition should be stripped but the
     * remaining activation preserved.
     */
    @Test
    void testStripExecutableConditionsMixedActivationPreservesOtherTriggers() {
        Profile profile = Profile.newBuilder()
                .id("exec-and-os")
                .activation(Activation.newBuilder()
                        .condition("executable('gcc') && ${os.name} == 'linux'")
                        .os(ActivationOS.newBuilder().name("linux").build())
                        .build())
                .build();

        List<Profile> result = DefaultConsumerPomBuilder.stripExecutableConditions(List.of(profile));

        assertEquals(1, result.size());
        Activation activation = result.get(0).getActivation();
        assertNotNull(activation, "Activation should be preserved when other triggers exist");
        assertNull(activation.getCondition(), "Condition should be stripped");
        assertNotNull(activation.getOs(), "OS trigger should be preserved");
    }

    /**
     * Profiles without {@code executable()} in their condition should pass through
     * unchanged.
     */
    @Test
    void testStripExecutableConditionsNoExecutableUnchanged() {
        Activation originalActivation =
                Activation.newBuilder().condition("${os.name} == 'linux'").build();
        Profile profile = Profile.newBuilder()
                .id("no-exec")
                .activation(originalActivation)
                .build();

        List<Profile> result = DefaultConsumerPomBuilder.stripExecutableConditions(List.of(profile));

        assertEquals(1, result.size());
        Activation activation = result.get(0).getActivation();
        assertNotNull(activation);
        assertEquals("${os.name} == 'linux'", activation.getCondition());
    }

    /**
     * Profiles with no activation at all should pass through unchanged.
     */
    @Test
    void testStripExecutableConditionsNullActivationUnchanged() {
        Profile profile = Profile.newBuilder().id("no-activation").build();

        List<Profile> result = DefaultConsumerPomBuilder.stripExecutableConditions(List.of(profile));

        assertEquals(1, result.size());
        assertNull(result.get(0).getActivation());
    }

    /**
     * A negated {@code executable()} call (e.g. {@code not(executable(...))})
     * should also be stripped from the condition.
     */
    @Test
    void testStripExecutableConditionsNegatedExecutableStripped() {
        Profile profile = Profile.newBuilder()
                .id("negated-exec")
                .activation(Activation.newBuilder()
                        .condition("not(executable('musl-gcc'))")
                        .build())
                .build();

        List<Profile> result = DefaultConsumerPomBuilder.stripExecutableConditions(List.of(profile));

        assertEquals(1, result.size());
        assertNull(result.get(0).getActivation(), "Negated executable() should also be stripped");
    }

    /**
     * When {@code executable()} is combined with a property trigger in the
     * activation, stripping should remove the condition but preserve the
     * property trigger.
     */
    @Test
    void testStripExecutableConditionsWithPropertyTriggerPreservesProperty() {
        Profile profile = Profile.newBuilder()
                .id("exec-and-property")
                .activation(Activation.newBuilder()
                        .condition("executable('docker')")
                        .property(ActivationProperty.newBuilder()
                                .name("docker.enabled")
                                .value("true")
                                .build())
                        .build())
                .build();

        List<Profile> result = DefaultConsumerPomBuilder.stripExecutableConditions(List.of(profile));

        assertEquals(1, result.size());
        Activation activation = result.get(0).getActivation();
        assertNotNull(activation, "Activation should be preserved when property trigger exists");
        assertNull(activation.getCondition(), "Condition should be stripped");
        assertNotNull(activation.getProperty(), "Property trigger should be preserved");
        assertEquals("docker.enabled", activation.getProperty().getName());
    }

    /**
     * Verifies that {@code transformNonPom} strips {@code executable()} conditions
     * from profiles via the {@code prune()} path.
     */
    @Test
    void testTransformNonPomStripsExecutableCondition() {
        Model model = Model.newBuilder()
                .profiles(List.of(Profile.newBuilder()
                        .id("exec-profile")
                        .activation(Activation.newBuilder()
                                .condition("executable('tool')")
                                .build())
                        .dependencies(List.of(Dependency.newBuilder()
                                .groupId("org.example")
                                .artifactId("tool-support")
                                .version("1.0")
                                .build()))
                        .build()))
                .build();

        Model result = DefaultConsumerPomBuilder.transformNonPom(model, null);

        // The profile had executable() activation and dependencies.
        // After pruning: activation is stripped (executable-only), build is stripped,
        // properties stripped, etc. The profile retains dependencies, so it survives
        // the isEmpty filter, but its activation should be null.
        if (!result.getProfiles().isEmpty()) {
            assertNull(
                    result.getProfiles().get(0).getActivation(),
                    "executable() condition should be stripped from transformNonPom profiles");
        }
    }

    /**
     * Verifies that {@code transformPom} strips {@code executable()} conditions
     * from profiles in parent/POM-packaged projects.
     */
    @Test
    void testTransformPomStripsExecutableCondition() throws Exception {
        setRootDirectory("trivial");
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");
        MavenProject project = getEffectiveModel(file);

        Model model = project.getModel().getDelegate();
        // Add a profile with an executable() condition to the model
        Profile execProfile = Profile.newBuilder()
                .id("exec-profile")
                .activation(
                        Activation.newBuilder().condition("executable('gcc')").build())
                .properties(Map.of("native.enabled", "true"))
                .build();
        model = model.withProfiles(List.of(execProfile));

        Model result = DefaultConsumerPomBuilder.transformPom(model, project);

        // The profile should have its activation stripped
        assertFalse(result.getProfiles().isEmpty(), "Profile should survive (it has properties)");
        assertNull(
                result.getProfiles().get(0).getActivation(),
                "executable() condition should be stripped from transformPom profiles");
    }

    // ── GH-12981: extension-contributed user property interpolation ──────────

    /**
     * Verifies that {@code interpolatePomVersions} resolves extension-contributed
     * property references (not present in the effective model's properties) in
     * dependency management versions.
     * <p>
     * This is the core fix for GH-12981: properties from {@code PropertyContributor}
     * extensions are NOT added to the model's {@code <properties>} (the model
     * builder's merge only overrides existing keys). Downstream consumers cannot
     * resolve them through the parent chain, so they must be interpolated.
     */
    @Test
    void testInterpolatePomVersionsResolvesExtensionProperties() {
        // Raw model: versions reference an extension-contributed property
        // that is NOT in the effective model's properties
        Dependency rawDep1 = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("common")
                .version("${ext.dynamicVersion}")
                .build();
        Dependency rawDep2 = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("app")
                .version("${ext.dynamicVersion}")
                .build();

        Model rawModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("bom")
                .version("1.0.0")
                .packaging("pom")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(rawDep1, rawDep2))
                        .build())
                .build();

        // Effective model: versions resolved, but properties does NOT contain ext.dynamicVersion
        // (it came from an extension, not the POM)
        Dependency effectiveDep1 = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("common")
                .version("1.0.0")
                .build();
        Dependency effectiveDep2 = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("app")
                .version("1.0.0")
                .build();

        Model effectiveModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("bom")
                .version("1.0.0")
                .packaging("pom")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(effectiveDep1, effectiveDep2))
                        .build())
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        List<Dependency> deps = result.getDependencyManagement().getDependencies();
        assertEquals(2, deps.size());
        assertEquals("1.0.0", deps.get(0).getVersion(), "Extension property should be interpolated");
        assertEquals("1.0.0", deps.get(1).getVersion(), "Extension property should be interpolated");
    }

    /**
     * Verifies that properties defined in the POM or parent chain are NOT
     * interpolated — downstream consumers can resolve them through the parent
     * reference preserved in the consumer POM.
     */
    @Test
    void testInterpolatePomVersionsPreservesModelProperties() {
        // Raw model: version references a property defined in the POM
        Dependency rawDep = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("lib")
                .version("${my.version}")
                .build();

        Model rawModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .properties(Map.of("my.version", "2.0.0"))
                .dependencies(List.of(rawDep))
                .build();

        // Effective model: version resolved, AND the property IS in properties
        Dependency effectiveDep = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("lib")
                .version("2.0.0")
                .build();

        Model effectiveModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .properties(Map.of("my.version", "2.0.0"))
                .dependencies(List.of(effectiveDep))
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        assertEquals(1, result.getDependencies().size());
        assertEquals(
                "${my.version}",
                result.getDependencies().get(0).getVersion(),
                "Model-defined property should be preserved as ${...} for consumers to resolve");
    }

    /**
     * Verifies that {@code project.*} built-in properties are treated as
     * resolvable and left as {@code ${...}} references.
     */
    @Test
    void testInterpolatePomVersionsPreservesBuiltInProperties() {
        Dependency rawDep = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("sibling")
                .version("${project.version}")
                .build();

        Model rawModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(rawDep))
                        .build())
                .build();

        Dependency effectiveDep = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("sibling")
                .version("1.0.0")
                .build();

        Model effectiveModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(effectiveDep))
                        .build())
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        assertEquals(
                "${project.version}",
                result.getDependencyManagement().getDependencies().get(0).getVersion(),
                "Built-in ${project.version} should be preserved");
    }

    /**
     * Verifies that mixed dependencies — some using model-defined properties,
     * some using extension properties — are handled correctly: only extension
     * properties are interpolated.
     */
    @Test
    void testInterpolatePomVersionsMixedModelAndExtensionProperties() {
        Dependency rawModelProp = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("stable")
                .version("${my.version}")
                .build();
        Dependency rawExtProp = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("dynamic")
                .version("${ext.version}")
                .build();

        Model rawModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(rawModelProp, rawExtProp))
                        .build())
                .build();

        Dependency effectiveModelProp = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("stable")
                .version("3.0.0")
                .build();
        Dependency effectiveExtProp = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("dynamic")
                .version("4.2.1")
                .build();

        // Effective model has "my.version" in properties (from POM/parent)
        // but NOT "ext.version" (from extension)
        Model effectiveModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .properties(Map.of("my.version", "3.0.0"))
                .dependencyManagement(DependencyManagement.newBuilder()
                        .dependencies(List.of(effectiveModelProp, effectiveExtProp))
                        .build())
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        List<Dependency> deps = result.getDependencyManagement().getDependencies();
        assertEquals(2, deps.size());
        assertEquals("${my.version}", deps.get(0).getVersion(), "Model-defined property should be preserved");
        assertEquals("4.2.1", deps.get(1).getVersion(), "Extension property should be interpolated");
    }

    /**
     * Verifies that an empty model is handled without errors.
     */
    @Test
    void testInterpolatePomVersionsEmptyModel() {
        Model rawModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("empty")
                .version("1.0.0")
                .packaging("pom")
                .build();

        Model effectiveModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("empty")
                .version("1.0.0")
                .packaging("pom")
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        assertTrue(result.getDependencies().isEmpty());
        assertNull(result.getDependencyManagement());
    }

    /**
     * Verifies that extension-contributed properties in direct dependencies
     * are interpolated.
     */
    @Test
    void testInterpolatePomVersionsResolvesDirectDependencyExtensionProperties() {
        Dependency rawDep = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("lib")
                .version("${ext.version}")
                .build();

        Model rawModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .dependencies(List.of(rawDep))
                .build();

        Dependency effectiveDep = Dependency.newBuilder()
                .groupId("com.example")
                .artifactId("lib")
                .version("2.5.0")
                .build();

        Model effectiveModel = Model.newBuilder()
                .groupId("com.example")
                .artifactId("parent")
                .version("1.0.0")
                .packaging("pom")
                .dependencies(List.of(effectiveDep))
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        assertEquals(1, result.getDependencies().size());
        assertEquals("2.5.0", result.getDependencies().get(0).getVersion());
    }

    /**
     * Verifies that properties defined in profiles of the raw model are treated as
     * consumer-resolvable, even when they are absent from the effective model's
     * properties (because BUILD_CONSUMER does not re-activate profiles and the
     * model builder's merge() never adds new user-property keys).
     * This is the exact scenario from MavenITmng8709ProfileDependencyVersionTest:
     * a profile with {@code <activeByDefault>true</activeByDefault>} defines
     * {@code junit.version}, and the consumer POM must preserve {@code ${junit.version}}.
     */
    @Test
    void testInterpolatePomVersionsPreservesProfileProperties() {
        Dependency rawDep = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("${junit.version}")
                .build();

        // Raw model has the property in a profile, NOT in top-level <properties>
        Profile profile = Profile.newBuilder()
                .id("default-versions")
                .activation(Activation.newBuilder().activeByDefault(true).build())
                .properties(Map.of("junit.version", "5.11.0"))
                .build();

        Model rawModel = Model.newBuilder()
                .groupId("org.apache.maven.its.mng8709")
                .artifactId("profile-version")
                .version("1.0")
                .profiles(List.of(profile))
                .dependencies(List.of(rawDep))
                .build();

        // Effective model: version is resolved, but junit.version is NOT in
        // effectiveModel.getProperties() because BUILD_CONSUMER didn't activate
        // the profile and merge() doesn't add new user-property keys
        Dependency effectiveDep = Dependency.newBuilder()
                .groupId("org.junit.jupiter")
                .artifactId("junit-jupiter-api")
                .version("5.11.0")
                .build();

        Model effectiveModel = Model.newBuilder()
                .groupId("org.apache.maven.its.mng8709")
                .artifactId("profile-version")
                .version("1.0")
                // No "junit.version" in properties — simulates BUILD_CONSUMER behavior
                .dependencies(List.of(effectiveDep))
                .build();

        Model result = DefaultConsumerPomBuilder.interpolatePomVersions(rawModel, effectiveModel);

        assertEquals(1, result.getDependencies().size());
        assertEquals(
                "${junit.version}",
                result.getDependencies().get(0).getVersion(),
                "Profile-defined property should be preserved as ${...} for consumers to resolve");
    }

    // ── hasNonModelProperties unit tests ─────────────────────────────────────

    @Test
    void testHasNonModelPropertiesReturnsTrueForExtensionProperty() {
        assertTrue(
                DefaultConsumerPomBuilder.hasNonModelProperties("${ext.version}", Map.of()),
                "Extension property not in model should return true");
    }

    @Test
    void testHasNonModelPropertiesReturnsFalseForModelProperty() {
        assertFalse(
                DefaultConsumerPomBuilder.hasNonModelProperties("${my.version}", Map.of("my.version", "1.0")),
                "Property defined in model should return false");
    }

    @Test
    void testHasNonModelPropertiesReturnsFalseForBuiltInProjectProperty() {
        assertFalse(
                DefaultConsumerPomBuilder.hasNonModelProperties("${project.version}", Map.of()),
                "Built-in project.version should return false");
    }

    @Test
    void testHasNonModelPropertiesReturnsTrueForMixedProperties() {
        assertTrue(
                DefaultConsumerPomBuilder.hasNonModelProperties(
                        "${my.version}-${ext.qualifier}", Map.of("my.version", "1.0")),
                "Should return true when at least one property is not in model");
    }

    @Test
    void testHasNonModelPropertiesReturnsFalseForAllModelProperties() {
        assertFalse(
                DefaultConsumerPomBuilder.hasNonModelProperties(
                        "${major}.${minor}", Map.of("major", "1", "minor", "0")),
                "Should return false when all properties are in model");
    }
}
