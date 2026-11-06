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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.RootLocator;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParentTraversalTest {
    private Session session;
    private DefaultModelBuilder builder;
    private ModelResolver externalResolver;

    @BeforeEach
    void setUp() {
        session = ApiRunner.createSession();
        builder = (DefaultModelBuilder) session.getService(ModelBuilder.class);
    }

    @Test
    void testDeepParentsPreserveProfilesAndCache(@TempDir Path directory) throws Exception {
        Path pom = hierarchy(directory, 1000, false);
        var state = state(pom);
        Set<String> chain = new LinkedHashSet<>(Set.of("caller"));
        var active = context(List.of("root-profile"), List.of()).start();
        Model model = state.readAsParentModel(active, chain);
        assertEquals("root", model.getProperties().get("inherited"));
        assertEquals("999", model.getProperties().get("nearest"));
        assertEquals("active", model.getProperties().get("profile-property"));
        assertNull(model.getParent());
        assertTrue(model.getProfiles().isEmpty());
        assertEquals(Set.of("caller"), chain);
        assertEquals(Boolean.TRUE, active.stop().usedActiveProfiles.get("root-profile"));

        var repeated = context(List.of("root-profile"), List.of()).start();
        assertSame(model, state.readAsParentModel(repeated, chain));
        assertEquals(Boolean.TRUE, repeated.stop().usedActiveProfiles.get("root-profile"));
        assertEquals(Set.of("caller"), chain);

        Model inactive = state.readAsParentModel(context(List.of(), List.of("root-profile")), chain);
        assertNull(inactive.getProperties().get("profile-property"));
        assertEquals("root", inactive.getProperties().get("inherited"));
        assertEquals("active", model.getProperties().get("profile-property"));
        assertEquals(Set.of("caller"), chain);
    }

    @Test
    void testFailedReadRestoresCallerAncestry(@TempDir Path directory) throws Exception {
        Path pom = hierarchy(directory, 10, true);
        Set<String> chain = new LinkedHashSet<>(Set.of("caller"));
        for (int attempt = 0; attempt < 2; attempt++) {
            ModelBuilderException error = assertThrows(
                    ModelBuilderException.class,
                    () -> state(pom).readAsParentModel(context(List.of(), List.of()), chain));
            assertTrue(error.getMessage().contains("The parents form a cycle"), error.getMessage());
            assertEquals(Set.of("caller"), chain);
        }
    }

    @Test
    void testMissingModelVersionUsesSuperModel(@TempDir Path directory) throws Exception {
        Path pom = hierarchy(directory, 1, false);
        Model parent =
                state(pom).readParent(Model.newInstance(), null, context(List.of(), List.of()), new LinkedHashSet<>());
        assertEquals("4.0.0", parent.getModelVersion());
    }

    @Test
    void testMultipleMixinsKeepDeclarationOrder(@TempDir Path directory) throws Exception {
        hierarchy(directory, 1, false);
        for (int i = 1; i <= 2; i++) {
            Files.writeString(directory.resolve("mixin-" + i + ".xml"), """
                    <project xmlns="http://maven.apache.org/POM/4.2.0">
                      <modelVersion>4.2.0</modelVersion>
                      <groupId>test</groupId>
                      <artifactId>mixin-%d</artifactId>
                      <version>1</version>
                      <packaging>pom</packaging>
                      <properties><nearest>%d</nearest><mixin-%d>present</mixin-%d></properties>
                    </project>
                    """.formatted(i, i, i, i));
        }
        Path pom = Files.createDirectory(directory.resolve("child")).resolve("pom.xml");
        Files.writeString(pom, """
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                  <modelVersion>4.2.0</modelVersion>
                  <parent>
                    <groupId>test</groupId>
                    <artifactId>parent-0</artifactId>
                    <version>1</version>
                    <relativePath>../parent-0/pom.xml</relativePath>
                  </parent>
                  <groupId>test</groupId>
                  <artifactId>child</artifactId>
                  <version>1</version>
                  <properties><nearest>child</nearest></properties>
                  <mixins>
                    <mixin>
                      <groupId>test</groupId>
                      <artifactId>mixin-1</artifactId>
                      <version>1</version>
                      <relativePath>../mixin-1.xml</relativePath>
                    </mixin>
                    <mixin>
                      <groupId>test</groupId>
                      <artifactId>mixin-2</artifactId>
                      <version>1</version>
                      <relativePath>../mixin-2.xml</relativePath>
                    </mixin>
                  </mixins>
                </project>
                """);
        var state = state(pom);
        Set<String> chain = new LinkedHashSet<>();
        Model model = state.readAsParentModel(context(List.of(), List.of()), chain);
        assertEquals("root", model.getProperties().get("inherited"));
        assertEquals("2", model.getProperties().get("nearest"));
        assertEquals("present", model.getProperties().get("mixin-1"));
        assertEquals("present", model.getProperties().get("mixin-2"));
        assertTrue(chain.isEmpty());
        assertSame(model, state.readAsParentModel(context(List.of(), List.of()), chain));
    }

    @Test
    void testExternalSelfMixinCycle(@TempDir Path directory) throws Exception {
        assertExternalMixinCycle(directory, 1, false);
    }

    @Test
    void testExternalMixinCycle(@TempDir Path directory) throws Exception {
        assertExternalMixinCycle(directory, 2, false);
    }

    @Test
    void testOpaqueExternalSelfMixinCycle(@TempDir Path directory) throws Exception {
        assertExternalMixinCycle(directory, 1, true);
    }

    @Test
    void testOpaqueExternalMixinCycle(@TempDir Path directory) throws Exception {
        assertExternalMixinCycle(directory, 2, true);
    }

    @Test
    void testExternalMixinsCanReuseCompletedSources(@TempDir Path directory) throws Exception {
        assertExternalMixinReuse(directory, false);
    }

    @Test
    void testOpaqueExternalMixinsWithSameDisplayLocation(@TempDir Path directory) throws Exception {
        assertExternalMixinReuse(directory, true);
    }

    @Test
    void testOpaqueMixinsDistinguishClassifierAndExtension() {
        AtomicInteger resolutions = new AtomicInteger();
        ModelResolver resolver = mock(ModelResolver.class);
        when(resolver.resolveModel(any(ModelResolver.ModelResolverRequest.class)))
                .thenAnswer(invocation -> {
                    assertTrue(resolutions.incrementAndGet() <= 16, "External mixin traversal did not terminate");
                    ModelResolver.ModelResolverRequest request = invocation.getArgument(0);
                    ModelSource source;
                    if ("left".equals(request.classifier())) {
                        source = opaqueMixin("shared", "left", "right", "pom");
                    } else if ("pom".equals(request.extension())) {
                        source = opaqueMixin("shared", "middle", "right", "xml");
                    } else {
                        source = opaqueMixin("shared", "right", null, null);
                    }
                    return new ModelResolver.ModelResolverResult(request, source, null);
                });
        bindExternalResolver(resolver);
        var state = builder.new ModelBuilderSessionState(ModelBuilderRequest.builder()
                .session(session)
                .source(opaqueMixin("root", "root", "left", "pom"))
                .requestType(ModelBuilderRequest.RequestType.CONSUMER_PARENT)
                .build());
        Model model = state.readAsParentModel(context(List.of(), List.of()), new LinkedHashSet<>());
        assertEquals(3, resolutions.get());
        for (String stage : List.of("root", "left", "middle", "right")) {
            assertEquals("present", model.getProperties().get(stage));
        }
    }

    private ModelSource opaqueMixin(String artifactId, String stage, String classifier, String extension) {
        String mixin = classifier == null ? "" : """
                <mixins><mixin>
                  <groupId>test</groupId>
                  <artifactId>shared</artifactId>
                  <version>1</version>
                  <classifier>%s</classifier>
                  <extension>%s</extension>
                </mixin></mixins>
                """.formatted(classifier, extension);
        return opaqueSource("""
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                  <modelVersion>4.2.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>%s</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  %s
                  <properties><%s>present</%s></properties>
                </project>
                """.formatted(artifactId, mixin, stage, stage).getBytes(StandardCharsets.UTF_8));
    }

    private void assertExternalMixinReuse(Path directory, boolean opaque) throws Exception {
        writeExternalMixin(directory, "root", List.of("left", "right"));
        writeExternalMixin(directory, "left", List.of("shared"));
        writeExternalMixin(directory, "right", List.of("shared"));
        writeExternalMixin(directory, "shared", List.of());
        AtomicInteger resolutions = externalMixinResolver(directory, opaque);
        var state = externalState(directory, "root", opaque);
        Set<String> markers = Set.of("test:root:1", state.request.getSource().getLocation());
        Set<String> chain = new LinkedHashSet<>(markers);
        Model model = state.readAsParentModel(context(List.of(), List.of()), chain);
        for (String artifactId : List.of("root", "left", "right", "shared")) {
            assertEquals("present", model.getProperties().get(artifactId));
        }
        assertEquals(4, resolutions.get());
        assertEquals(markers, chain);
        assertSame(model, state.readAsParentModel(context(List.of(), List.of()), chain));
        assertEquals(4, resolutions.get());
        assertEquals(markers, chain);
    }

    private void assertExternalMixinCycle(Path directory, int length, boolean opaque) throws Exception {
        for (int i = 0; i < length; i++) {
            writeExternalMixin(directory, "mixin-" + i, List.of("mixin-" + ((i + 1) % length)));
        }
        AtomicInteger resolutions = externalMixinResolver(directory, opaque);
        var state = externalState(directory, "mixin-0", opaque);
        Set<String> chain = new LinkedHashSet<>(Set.of("caller"));
        for (int attempt = 0; attempt < 2; attempt++) {
            // Parent failures remain in the owning model's collector. Retry with a fresh collector,
            // retaining the request caches and caller ancestry to exercise failed-traversal cleanup.
            var attemptState = state.derive(state.request);
            resolutions.set(0);
            ModelBuilderException error = assertThrows(
                    ModelBuilderException.class,
                    () -> attemptState.readAsParentModel(context(List.of(), List.of()), chain));
            assertTrue(error.getMessage().contains("cycle"), error.getMessage());
            assertTrue(attemptState.getProblemCollector().hasFatalProblems());
            assertEquals(length + (opaque ? 1 : 0), resolutions.get(), "Resolution count on attempt " + attempt);
            assertEquals(Set.of("caller"), chain);
        }
    }

    private AtomicInteger externalMixinResolver(Path directory, boolean opaque) {
        AtomicInteger resolutions = new AtomicInteger();
        ModelResolver resolver = mock(ModelResolver.class);
        when(resolver.resolveModel(any(ModelResolver.ModelResolverRequest.class)))
                .thenAnswer(invocation -> {
                    // Bound the regression itself if cycle detection is removed.
                    assertTrue(resolutions.incrementAndGet() <= 16, "External mixin traversal did not terminate");
                    ModelResolver.ModelResolverRequest request = invocation.getArgument(0);
                    return new ModelResolver.ModelResolverResult(
                            request, externalSource(directory, request.artifactId(), opaque), null);
                });
        bindExternalResolver(resolver);
        return resolutions;
    }

    private void bindExternalResolver(ModelResolver resolver) {
        externalResolver = resolver;
        session = ApiRunner.createSession(injector -> injector.bindInstance(ParentTraversalTest.class, this));
        assertSame(resolver, session.getService(ModelResolver.class));
        builder = (DefaultModelBuilder) session.getService(ModelBuilder.class);
    }

    @Provides
    @Priority(10)
    ModelResolver externalResolver() {
        return externalResolver;
    }

    private DefaultModelBuilder.ModelBuilderSessionState externalState(
            Path directory, String artifactId, boolean opaque) throws IOException {
        return builder.new ModelBuilderSessionState(ModelBuilderRequest.builder()
                .session(session)
                .source(externalSource(directory, artifactId, opaque))
                .requestType(ModelBuilderRequest.RequestType.CONSUMER_PARENT)
                .build());
    }

    private ModelSource externalSource(Path directory, String artifactId, boolean opaque) throws IOException {
        Path path = directory.resolve(artifactId + ".xml");
        if (!opaque) {
            return Sources.resolvedSource(path, "test:" + artifactId + ":1");
        }
        return opaqueSource(Files.readAllBytes(path));
    }

    private ModelSource opaqueSource(byte[] content) {
        return new ModelSource() {
            @Override
            public Path getPath() {
                return null;
            }

            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(content);
            }

            @Override
            public String getLocation() {
                return "<memory>";
            }

            @Override
            public ModelSource resolve(String relative) {
                return null;
            }

            @Override
            public ModelSource resolve(ModelLocator locator, String relative) {
                return null;
            }
        };
    }

    private void writeExternalMixin(Path directory, String artifactId, List<String> mixins) throws IOException {
        StringBuilder references = new StringBuilder();
        for (String mixin : mixins) {
            references.append("""
                    <mixin>
                      <groupId>test</groupId>
                      <artifactId>%s</artifactId>
                      <version>1</version>
                    </mixin>
                    """.formatted(mixin));
        }
        Files.writeString(
                directory.resolve(artifactId + ".xml"), """
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                  <modelVersion>4.2.0</modelVersion>
                  <groupId>test</groupId>
                  <artifactId>%s</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <mixins>%s</mixins>
                  <properties><%s>present</%s></properties>
                </project>
                """.formatted(artifactId, references, artifactId, artifactId));
    }

    private DefaultModelBuilder.ModelBuilderSessionState state(Path pom) {
        return builder.new ModelBuilderSessionState(ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(pom))
                .requestType(ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                .build());
    }

    private DefaultProfileActivationContext context(List<String> active, List<String> inactive) {
        return new DefaultProfileActivationContext(
                session.getService(RootLocator.class),
                session.getService(Interpolator.class),
                active,
                inactive,
                Map.of(),
                Map.of(),
                Model.newInstance());
    }

    private Path hierarchy(Path directory, int depth, boolean cyclic) throws IOException {
        for (int i = 0; i < depth; i++) {
            Path pom = Files.createDirectory(directory.resolve("parent-" + i)).resolve("pom.xml");
            int parent = i == 0 && cyclic ? depth - 1 : i - 1;
            String ancestry = parent < 0 ? "" : """
                    <parent>
                      <groupId>test</groupId>
                      <artifactId>parent-%d</artifactId>
                      <version>1</version>
                      <relativePath>../parent-%d/pom.xml</relativePath>
                    </parent>
                    """.formatted(parent, parent);
            String root = i != 0 ? "" : """
                    <profiles>
                      <profile>
                        <id>root-profile</id>
                        <properties><profile-property>active</profile-property></properties>
                      </profile>
                    </profiles>
                    """;
            Files.writeString(pom, """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      %s
                      <groupId>test</groupId>
                      <artifactId>parent-%d</artifactId>
                      <version>1</version>
                      <packaging>pom</packaging>
                      <properties>%s<nearest>%d</nearest></properties>
                      %s
                    </project>
                    """.formatted(ancestry, i, i == 0 ? "<inherited>root</inherited>" : "", i, root));
        }
        return directory.resolve("parent-" + (depth - 1)).resolve("pom.xml");
    }
}
