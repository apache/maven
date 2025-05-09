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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.assertj.XmlAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ConsumerPomArtifactTransformerTest {

    @Test
    void transform() throws Exception {
        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);

        Path beforePomFile =
                Paths.get("src/test/resources/projects/transform/before.pom").toAbsolutePath();
        Path afterPomFile =
                Paths.get("src/test/resources/projects/transform/after.pom").toAbsolutePath();
        Path tempFile = Files.createTempFile("", ".pom");
        Files.delete(tempFile);
        try (InputStream expected = Files.newInputStream(beforePomFile)) {
            Model model = new Model(new MavenStaxReader().read(expected));
            MavenProject project = new MavenProject(model);
            project.setOriginalModel(model);
            DefaultConsumerPomArtifactTransformer t = new DefaultConsumerPomArtifactTransformer((s, p, f) -> {
                try (InputStream is = Files.newInputStream(f)) {
                    return DefaultConsumerPomBuilder.transformPom(new MavenStaxReader().read(is), project);
                }
            });

            t.transform(project, systemSessionMock, beforePomFile, tempFile);
        }
        XmlAssert.assertThat(tempFile.toFile()).and(afterPomFile.toFile()).areIdentical();
    }

    @Test
    void transformJarConsumerPom() throws Exception {
        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);

        Path beforePomFile = Paths.get("src/test/resources/projects/transform/jar/before.pom")
                .toAbsolutePath();
        Path afterPomFile =
                Paths.get("src/test/resources/projects/transform/jar/after.pom").toAbsolutePath();
        Path tempFile = Files.createTempFile("", ".pom");
        Files.delete(tempFile);
        try (InputStream expected = Files.newInputStream(beforePomFile)) {
            Model model = new Model(new MavenStaxReader().read(expected));
            MavenProject project = new MavenProject(model);
            project.setOriginalModel(model);
            DefaultConsumerPomArtifactTransformer t = new DefaultConsumerPomArtifactTransformer((s, p, f) -> {
                try (InputStream is = Files.newInputStream(f)) {
                    return DefaultConsumerPomBuilder.transformNonPom(new MavenStaxReader().read(is), project);
                }
            });

            t.transform(project, systemSessionMock, beforePomFile, tempFile);
        }
        XmlAssert.assertThat(afterPomFile.toFile()).and(tempFile.toFile()).areIdentical();
    }

    @Test
    void injectTransformedArtifactsWithoutPomShouldNotInjectAnyArtifacts() throws IOException {
        MavenProject emptyProject = new MavenProject();

        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);

        new DefaultConsumerPomArtifactTransformer((session, project, src) -> null)
                .injectTransformedArtifacts(systemSessionMock, emptyProject);

        assertThat(emptyProject.getAttachedArtifacts()).isEmpty();
    }
}
