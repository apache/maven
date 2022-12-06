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
package org.apache.maven.internal.transform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.transform.ArtifactTransformer;
import org.eclipse.aether.transform.Identity;
import org.eclipse.aether.transform.TransformException;
import org.eclipse.aether.transform.TransformedArtifact;
import org.eclipse.aether.util.FileUtils;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * Artifact signing transformer: it provides artifact signatures.
 *
 * @since TBD
 */
@Singleton
@Named
public class SigningArtifactTransformer implements ArtifactTransformer {
    private static final String EXT = ".asc";

    @Override
    public TransformedArtifact transformInstallArtifact(RepositorySystemSession session, Artifact artifact) {
        return new Identity.IdentityTransformedArtifact(artifact);
    }

    @Override
    public TransformedArtifact transformDeployArtifact(RepositorySystemSession repositorySystemSession,
                                                       Artifact artifact) throws IOException {
        Path signatureFile = Files.createTempFile("artifact", EXT); // TODO: where? maybe target?
        FileUtils.writeFile(signatureFile, p -> Files.write(p, "fake-signature".getBytes(StandardCharsets.UTF_8)));

        SubArtifact signature =
                new SubArtifact(artifact, "", artifact.getExtension() + EXT, signatureFile.toFile());
        return new TransformedArtifact() {
            @Override
            public Artifact getTransformedArtifact() {
                return signature;
            }
        };
    }
}
