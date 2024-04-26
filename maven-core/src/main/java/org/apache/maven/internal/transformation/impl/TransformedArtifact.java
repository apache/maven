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

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.internal.transformation.TransformationFailedException;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Transformed artifact is derived with some transformation from source artifact.
 *
 * @since TBD
 */
class TransformedArtifact extends DefaultArtifact {

    private static final int SHA1_BUFFER_SIZE = 8192;
    private final DefaultConsumerPomArtifactTransformer defaultConsumerPomArtifactTransformer;
    private final MavenProject project;
    private final Supplier<Path> sourcePathProvider;
    private final Path target;
    private final RepositorySystemSession session;
    private final AtomicReference<String> sourceState;

    TransformedArtifact(
            DefaultConsumerPomArtifactTransformer defaultConsumerPomArtifactTransformer,
            MavenProject project,
            Path target,
            RepositorySystemSession session,
            org.apache.maven.artifact.Artifact source,
            Supplier<Path> sourcePathProvider,
            String classifier,
            String extension) {
        super(
                source.getGroupId(),
                source.getArtifactId(),
                source.getVersionRange(),
                source.getScope(),
                extension,
                classifier,
                new TransformedArtifactHandler(
                        classifier, extension, source.getArtifactHandler().getPackaging()));
        this.defaultConsumerPomArtifactTransformer = defaultConsumerPomArtifactTransformer;
        this.project = project;
        this.target = target;
        this.session = session;
        this.sourcePathProvider = sourcePathProvider;
        this.sourceState = new AtomicReference<>(null);
    }

    @Override
    public boolean isResolved() {
        return getFile() != null;
    }

    @Override
    public void setFile(File file) {
        throw new UnsupportedOperationException("transformed artifact file cannot be set");
    }

    @Override
    public synchronized File getFile() {
        try {
            String state = mayUpdate();
            if (state == null) {
                return null;
            }
            return target.toFile();
        } catch (IOException | NoSuchAlgorithmException | XMLStreamException | ModelBuildingException e) {
            throw new TransformationFailedException(e);
        }
    }

    private String mayUpdate()
            throws IOException, NoSuchAlgorithmException, XMLStreamException, ModelBuildingException {
        String result;
        Path src = sourcePathProvider.get();
        if (src == null) {
            Files.deleteIfExists(target);
            result = null;
        } else if (!Files.exists(src)) {
            Files.deleteIfExists(target);
            result = "";
        } else {
            String current = sha1(src);
            String existing = sourceState.get();
            if (!Objects.equals(current, existing)) {
                defaultConsumerPomArtifactTransformer.transform(project, session, src, target);
                Files.setLastModifiedTime(target, Files.getLastModifiedTime(src));
            }
            result = current;
        }
        sourceState.set(result);
        return result;
    }

    static String sha1(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = Files.newInputStream(path)) {
            byte[] buffer = new byte[SHA1_BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
