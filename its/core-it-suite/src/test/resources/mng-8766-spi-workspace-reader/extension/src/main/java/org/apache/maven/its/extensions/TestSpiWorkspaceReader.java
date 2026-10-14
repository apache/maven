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
package org.apache.maven.its.extensions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.spi.WorkspaceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("test-ide")
public class TestSpiWorkspaceReader implements WorkspaceReader {

    private static final Logger log = LoggerFactory.getLogger(TestSpiWorkspaceReader.class);

    /**
     * GAV of the synthetic artifact served by this workspace reader.
     * The test project declares a dependency on this artifact.
     */
    private static final String SYNTHETIC_GROUP = "org.apache.maven.its.mng8766";

    private static final String SYNTHETIC_ARTIFACT = "virtual-dep";
    private static final String SYNTHETIC_VERSION = "1.0";

    public TestSpiWorkspaceReader() {
        log.info("[SPI-WR] created");
    }

    @Override
    public Optional<Path> findArtifact(Artifact artifact) {
        log.info("[SPI-WR] findArtifact({})", artifact.key());
        if (isSyntheticArtifact(artifact)) {
            try {
                // Return a minimal (empty) jar file so the resolver can satisfy the dependency
                Path jar = Files.createTempFile("mng8766-virtual-dep", ".jar");
                jar.toFile().deleteOnExit();
                // Write a valid empty ZIP/jar (4 bytes: end-of-central-directory signature)
                try (OutputStream os = Files.newOutputStream(jar)) {
                    os.write(new byte[] {0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
                }
                log.info("[SPI-WR] findArtifact: returning synthetic jar for {}", artifact.key());
                return Optional.of(jar);
            } catch (IOException e) {
                log.warn("[SPI-WR] could not create synthetic jar: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        log.info("[SPI-WR] findVersions({})", artifact.key());
        if (SYNTHETIC_GROUP.equals(artifact.groupId()) && SYNTHETIC_ARTIFACT.equals(artifact.artifactId())) {
            return Collections.singletonList(SYNTHETIC_VERSION);
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<Model> findModel(Artifact artifact) {
        log.info("[SPI-WR] findModel({})", artifact.key());
        if (isSyntheticArtifact(artifact)) {
            Model model = Model.newBuilder()
                    .modelVersion("4.0.0")
                    .groupId(SYNTHETIC_GROUP)
                    .artifactId(SYNTHETIC_ARTIFACT)
                    .version(SYNTHETIC_VERSION)
                    .packaging("jar")
                    .build();
            log.info("[SPI-WR] findModel: returning synthetic POM for {}", artifact.key());
            return Optional.of(model);
        }
        return Optional.empty();
    }

    private static boolean isSyntheticArtifact(Artifact artifact) {
        return SYNTHETIC_GROUP.equals(artifact.groupId())
                && SYNTHETIC_ARTIFACT.equals(artifact.artifactId())
                && SYNTHETIC_VERSION.equals(artifact.version().toString());
    }
}
