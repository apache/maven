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
package org.apache.maven.impl.standalone;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.RequestTraceHelper;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.spi.connector.transport.http.ChecksumExtractor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTraceTest {

    @Test
    void testTraces() {
        Session session = ApiRunner.createSession(injector -> {
            injector.bindInstance(RequestTraceTest.class, this);
        });

        ModelBuilder builder = session.getService(ModelBuilder.class);
        ModelBuilderResult result = builder.newSession()
                .build(ModelBuilderRequest.builder()
                        .session(session)
                        .source(Sources.buildSource(Path.of("pom.xml").toAbsolutePath()))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .recursive(true)
                        .build());
        assertNotNull(result.getEffectiveModel());

        List<RepositoryEvent> events = new CopyOnWriteArrayList<>();
        ((DefaultRepositorySystemSession) InternalSession.from(session).getSession())
                .setRepositoryListener(new AbstractRepositoryListener() {
                    @Override
                    public void artifactResolved(RepositoryEvent event) {
                        events.add(event);
                    }
                });

        ArtifactCoordinates coords =
                session.createArtifactCoordinates("org.apache.maven:maven-api-core:4.0.0-alpha-13");
        DownloadedArtifact res = session.resolveArtifact(coords);
        assertNotNull(res);
        assertNotNull(res.getPath());
        assertTrue(Files.exists(res.getPath()));

        Node node = session.getService(DependencyResolver.class)
                .collect(DependencyResolverRequest.builder()
                        .trace(new RequestTrace("collect", null, null))
                        .session(session)
                        .requestType(DependencyResolverRequest.RequestType.COLLECT)
                        .dependency(session.createDependencyCoordinates(coords))
                        .pathScope(PathScope.MAIN_RUNTIME)
                        .build())
                .getRoot()
                .getChildren()
                .getFirst();

        for (RepositoryEvent event : events) {
            org.eclipse.aether.RequestTrace trace = event.getTrace();
            assertNotNull(trace);

            RequestTrace rTrace = RequestTraceHelper.toMaven("collect", trace);
            assertNotNull(rTrace);
            assertNotNull(rTrace.parent());
        }

        assertNotNull(node);
        assertEquals(6, node.getChildren().size());
    }

    @Provides
    @Named(FileTransporterFactory.NAME)
    static FileTransporterFactory newFileTransporterFactory() {
        return new FileTransporterFactory();
    }

    @Provides
    @Named(ApacheTransporterFactory.NAME)
    static ApacheTransporterFactory newApacheTransporterFactory(
            ChecksumExtractor checksumExtractor, PathProcessor pathProcessor) {
        return new ApacheTransporterFactory(checksumExtractor, pathProcessor);
    }
}
