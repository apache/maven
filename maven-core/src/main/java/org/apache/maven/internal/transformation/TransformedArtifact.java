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
package org.apache.maven.internal.transformation;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;

import static java.util.Objects.requireNonNull;

/**
 * Transformed artifact is derived with some transformation from source artifact.
 *
 * @since TBD
 */
abstract class TransformedArtifact extends DefaultArtifact {

    private final OnChangeTransformer onChangeTransformer;

    TransformedArtifact(
            Artifact source,
            Supplier<Path> sourcePathProvider,
            String classifier,
            String extension,
            Path targetPath,
            BiConsumer<Path, Path> transformerConsumer) {
        super(
                source.getGroupId(),
                source.getArtifactId(),
                source.getVersionRange(),
                source.getScope(),
                extension,
                classifier,
                new TransformedArtifactHandler(
                        classifier, extension, source.getArtifactHandler().getPackaging()));
        this.onChangeTransformer =
                new OnChangeTransformer(sourcePathProvider, targetPath, TransformedArtifact::sha1, transformerConsumer);
    }

    @Override
    public boolean isResolved() {
        return getFile() != null;
    }

    @Override
    public void setFile(File file) {
        throw new IllegalStateException("transformed artifact file cannot be set");
    }

    @Override
    public File getFile() {
        Path result = onChangeTransformer.get();
        if (result == null) {
            return null;
        }
        return result.toFile();
    }

    private static final int BUFFER_SIZE = 8192;

    private static String sha1(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (InputStream fis = Files.newInputStream(path)) {
                byte[] buffer = new byte[BUFFER_SIZE];
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TransformedArtifactHandler implements ArtifactHandler {
        private final String classifier;

        private final String extension;

        private final String packaging;

        private TransformedArtifactHandler(String classifier, String extension, String packaging) {
            this.classifier = classifier;
            this.extension = requireNonNull(extension);
            this.packaging = requireNonNull(packaging);
        }

        public String getClassifier() {
            return classifier;
        }

        public String getDirectory() {
            return null;
        }

        public String getExtension() {
            return extension;
        }

        public String getLanguage() {
            return "none";
        }

        public String getPackaging() {
            return packaging;
        }

        public boolean isAddedToClasspath() {
            return false;
        }

        public boolean isIncludesDependencies() {
            return false;
        }
    }
}
