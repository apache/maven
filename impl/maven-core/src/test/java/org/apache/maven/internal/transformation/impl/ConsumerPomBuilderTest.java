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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationOS;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Dependency;
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
     * Verifies that repositories not declared in the project's own POM file (e.g. inherited from a
     * parent POM or injected by an active settings.xml profile into the effective model) are pruned
     * from the consumer POM, while repositories the project itself declares are retained. The central
     * repository is always removed.
     */
    @Test
    void testConsumerPomKeepsOnlyDeclaredRepositories() {
        Model model = Model.newBuilder()
                .groupId("test")
                .artifactId("test")
                .version("1.0")
                .repositories(List.of(
                        Repository.newBuilder()
                                .id("central")
                                .url("https://repo.maven.apache.org/maven2")
                                .build(),
                        Repository.newBuilder()
                                .id("own-repo")
                                .url("https://repo.example.com/releases")
                                .build(),
                        Repository.newBuilder()
                                .id("corp-nexus")
                                .url("https://nexus.corp.internal/repo")
                                .build()))
                .build();

        Model transformed = DefaultConsumerPomBuilder.transformNonPom(model, null, Set.of("own-repo"));

        assertEquals(
                List.of("own-repo"),
                transformed.getRepositories().stream().map(Repository::getId).toList());
    }

    /**
     * Verifies the legacy behavior when repository sanitization is disabled (a {@code null} set of
     * declared repository ids): every repository except central is published in the consumer POM.
     */
    @Test
    void testAllNonCentralRepositoriesKeptWhenSanitizationDisabled() {
        Model model = Model.newBuilder()
                .groupId("test")
                .artifactId("test")
                .version("1.0")
                .repositories(List.of(
                        Repository.newBuilder()
                                .id("central")
                                .url("https://repo.maven.apache.org/maven2")
                                .build(),
                        Repository.newBuilder()
                                .id("corp-nexus")
                                .url("https://nexus.corp.internal/repo")
                                .build()))
                .build();

        Model transformed = DefaultConsumerPomBuilder.transformNonPom(model, null, null);

        assertEquals(
                List.of("corp-nexus"),
                transformed.getRepositories().stream().map(Repository::getId).toList());
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
}
