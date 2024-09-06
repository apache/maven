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
package org.apache.maven.internal.impl.standalone;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestApiStandalone {

    @Test
    void testStandalone() {
        Session session = ApiRunner.createSession();

        ModelBuilder builder = session.getService(ModelBuilder.class);
        ModelBuilderResult result = builder.newSession()
                .build(ModelBuilderRequest.builder()
                        .session(session)
                        .source(ModelSource.fromPath(Paths.get("pom.xml").toAbsolutePath()))
                        .requestType(ModelBuilderRequest.RequestType.BUILD_POM)
                        .build());
        assertNotNull(result.getEffectiveModel());

        ArtifactCoordinates coords =
                session.createArtifactCoordinates("org.apache.maven:maven-api-core:4.0.0-alpha-13");
        DownloadedArtifact res = session.resolveArtifact(coords);
        assertNotNull(res);
        assertNotNull(res.getPath());
        assertTrue(Files.exists(res.getPath()));

        Node node = session.collectDependencies(session.createDependencyCoordinates(coords));
        assertNotNull(node);
        assertEquals(8, node.getChildren().size());
    }
}
