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
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Node;
import org.apache.maven.api.Session;
import org.apache.maven.di.Injector;
import org.apache.maven.internal.impl.DefaultArtifactCoordinateFactory;
import org.apache.maven.internal.impl.DefaultArtifactDeployer;
import org.apache.maven.internal.impl.DefaultArtifactFactory;
import org.apache.maven.internal.impl.DefaultArtifactInstaller;
import org.apache.maven.internal.impl.DefaultArtifactResolver;
import org.apache.maven.internal.impl.DefaultChecksumAlgorithmService;
import org.apache.maven.internal.impl.DefaultDependencyCollector;
import org.apache.maven.internal.impl.DefaultDependencyCoordinateFactory;
import org.apache.maven.internal.impl.DefaultLocalRepositoryManager;
import org.apache.maven.internal.impl.DefaultMessageBuilderFactory;
import org.apache.maven.internal.impl.DefaultModelXmlFactory;
import org.apache.maven.internal.impl.DefaultRepositoryFactory;
import org.apache.maven.internal.impl.DefaultSettingsBuilder;
import org.apache.maven.internal.impl.DefaultSettingsXmlFactory;
import org.apache.maven.internal.impl.DefaultToolchainsBuilder;
import org.apache.maven.internal.impl.DefaultToolchainsXmlFactory;
import org.apache.maven.internal.impl.DefaultTransportProvider;
import org.apache.maven.internal.impl.DefaultVersionParser;
import org.apache.maven.internal.impl.DefaultVersionRangeResolver;
import org.apache.maven.internal.impl.DefaultVersionResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestApiStandalone {

    @Test
    void testStandalone() {
        Injector injector = Injector.create();
        injector.bindInstance(Injector.class, injector);
        injector.bindImplicit(ApiRunner.class);
        injector.bindImplicit(DefaultArtifactCoordinateFactory.class);
        injector.bindImplicit(DefaultArtifactDeployer.class);
        injector.bindImplicit(DefaultArtifactFactory.class);
        injector.bindImplicit(DefaultArtifactInstaller.class);
        injector.bindImplicit(DefaultArtifactResolver.class);
        injector.bindImplicit(DefaultChecksumAlgorithmService.class);
        injector.bindImplicit(DefaultDependencyCollector.class);
        injector.bindImplicit(DefaultDependencyCoordinateFactory.class);
        injector.bindImplicit(DefaultLocalRepositoryManager.class);
        injector.bindImplicit(DefaultMessageBuilderFactory.class);
        injector.bindImplicit(DefaultModelXmlFactory.class);
        injector.bindImplicit(DefaultRepositoryFactory.class);
        injector.bindImplicit(DefaultSettingsBuilder.class);
        injector.bindImplicit(DefaultSettingsXmlFactory.class);
        injector.bindImplicit(DefaultToolchainsBuilder.class);
        injector.bindImplicit(DefaultToolchainsXmlFactory.class);
        injector.bindImplicit(DefaultTransportProvider.class);
        injector.bindImplicit(DefaultVersionParser.class);
        injector.bindImplicit(DefaultVersionRangeResolver.class);
        injector.bindImplicit(DefaultVersionResolver.class);

        Session session = injector.getInstance(Session.class);
        ArtifactCoordinate coord = session.createArtifactCoordinate("org.apache.maven:maven-api-core:4.0.0-alpha-13");
        Map.Entry<Artifact, Path> res = session.resolveArtifact(coord);
        assertNotNull(res);
        assertNotNull(res.getValue());
        assertTrue(Files.exists(res.getValue()));

        Node node = session.collectDependencies(session.createDependencyCoordinate(coord));
        assertNotNull(node);
        assertEquals(8, node.getChildren().size());
    }
}
