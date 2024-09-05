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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelResolver;
import org.apache.maven.api.services.ModelResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.spi.ModelTransformer;
import org.apache.maven.api.spi.ModelTransformerException;
import org.apache.maven.di.Injector;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.internal.impl.model.DefaultModelBuilder;
import org.apache.maven.internal.transformation.AbstractRepositoryTestCase;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerPomBuilderTest extends AbstractRepositoryTestCase {

    @Inject
    ConsumerPomBuilder builder;

    @BeforeEach
    void setupTransformerContext() throws Exception {
        // We need to hack things a bit here to get the transformer context to work
        // * we cannot use the CIFriendlyVersionModelTransformer directly because
        //    it's a session scoped bean and all tests using a model builder would have
        //    to use a session and initialize the scope in order for DI to start
        // * the transformer context is supposed to be immutable but in this case
        //    we don't build the full projects before, so we need to pass a mutable
        //    context to the model builder
        // * we also need to bind the model resolver explicitly to avoid going
        //    to maven central

        getContainer().lookup(Injector.class).bindImplicit(MyModelResolver.class);
        InternalSession iSession = InternalSession.from(session);
        DefaultModelBuilder modelBuilder = (DefaultModelBuilder) getContainer().lookup(ModelBuilder.class);
        Field transformersField = DefaultModelBuilder.class.getDeclaredField("transformers");
        transformersField.setAccessible(true);
        transformersField.set(modelBuilder, List.of(new CIFriendlyVersionModelTransformer(iSession)));
        //        ModelTransformerContextBuilder tcb = modelBuilder.newTransformerContextBuilder();
        //        ModelTransformerContext context = tcb.initialize(
        //                ModelBuilderRequest.builder()
        //                        .projectBuild(true)
        //                        .session(iSession)
        //                        .transformerContextBuilder(tcb)
        //                        .build(),
        //                modelBuilder.newCollector());
        //        iSession.getData().set(ModelTransformerContext.KEY, context);
        iSession.getData().set(SessionData.key(ModelResolver.class), new MyModelResolver());
    }

    @Test
    void testTrivialConsumer() throws Exception {
        MavenProject project;
        Path file = Paths.get("src/test/resources/consumer/trivial/child/pom.xml");
        try (InputStream inputStream = Files.newInputStream(file)) {
            org.apache.maven.model.Model model =
                    new org.apache.maven.model.Model(new MavenStaxReader().read(inputStream));
            project = new MavenProject(model);
            project.setRootDirectory(Paths.get("src/test/resources/consumer/trivial"));
            project.setOriginalModel(model);
        }
        Model model = builder.build(session, project, file);

        assertNotNull(model);
    }

    @Test
    void testSimpleConsumer() throws Exception {
        MavenProject project;
        Path file = Paths.get("src/test/resources/consumer/simple/simple-parent/simple-weather/pom.xml");

        InternalMavenSession.from(InternalSession.from(session))
                .getMavenSession()
                .getRequest()
                .getUserProperties()
                .setProperty("changelist", "MNG6957");
        try (InputStream inputStream = Files.newInputStream(file)) {
            org.apache.maven.model.Model model =
                    new org.apache.maven.model.Model(new MavenStaxReader().read(inputStream));
            project = new MavenProject(model);
            project.setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
            project.setOriginalModel(model);
        }
        InternalMavenSession.from(InternalSession.from(session))
                .getMavenSession()
                .getRequest()
                .setRootDirectory(Paths.get("src/test/resources/consumer/simple"));
        Model model = builder.build(session, project, file);

        assertNotNull(model);
        assertTrue(model.getProfiles().isEmpty());
    }

    static class MyModelResolver implements ModelResolver {
        @Override
        public ModelSource resolveModel(
                Session session,
                List<RemoteRepository> repositories,
                String groupId,
                String artifactId,
                String version,
                Consumer<String> resolvedVersion)
                throws ModelResolverException {
            String id = groupId + ":" + artifactId + ":" + version;
            if (id.startsWith("org.sonatype.mavenbook.multi:parent:")) {
                return ModelSource.fromPath(Paths.get("src/test/resources/consumer/simple/pom.xml"));
            } else if (id.startsWith("org.sonatype.mavenbook.multi:simple-parent:")) {
                return ModelSource.fromPath(Paths.get("src/test/resources/consumer/simple/simple-parent/pom.xml"));
            } else if (id.startsWith("org.my.group:parent:")) {
                return ModelSource.fromPath(Paths.get("src/test/resources/consumer/trivial/pom.xml"));
            }
            return null;
        }
    }

    static class CIFriendlyVersionModelTransformer implements ModelTransformer {
        private static final String SHA1_PROPERTY = "sha1";
        private static final String CHANGELIST_PROPERTY = "changelist";
        private static final String REVISION_PROPERTY = "revision";
        private final Session session;

        CIFriendlyVersionModelTransformer(Session session) {
            this.session = session;
        }

        @Override
        public Model transformFileModel(Model model) throws ModelTransformerException {
            return model.with()
                    .version(replaceCiFriendlyVersion(model.getVersion()))
                    .parent(replaceParent(model.getParent()))
                    .build();
        }

        Parent replaceParent(Parent parent) {
            return parent != null ? parent.withVersion(replaceCiFriendlyVersion(parent.getVersion())) : null;
        }

        String replaceCiFriendlyVersion(String version) {
            if (version != null) {
                for (String key : Arrays.asList(SHA1_PROPERTY, CHANGELIST_PROPERTY, REVISION_PROPERTY)) {
                    String val = session.getUserProperties().get(key);
                    if (val != null) {
                        version = version.replace("${" + key + "}", val);
                    }
                }
            }
            return version;
        }
    }
}
