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
package org.apache.maven.internal.impl.resolver;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.maven.api.metadata.Metadata;
import org.apache.maven.metadata.v4.MetadataStaxReader;
import org.apache.maven.metadata.v4.MetadataStaxWriter;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.metadata.AbstractMetadata;
import org.eclipse.aether.metadata.MergeableMetadata;

/**
 */
abstract class MavenMetadata extends AbstractMetadata implements MergeableMetadata {

    static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    static DateFormat fmt;

    static {
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(timezone);
    }

    protected Metadata metadata;

    private final Path path;

    protected final Date timestamp;

    private boolean merged;

    @Deprecated
    protected MavenMetadata(Metadata metadata, File file, Date timestamp) {
        this(metadata, file != null ? file.toPath() : null, timestamp);
    }

    protected MavenMetadata(Metadata metadata, Path path, Date timestamp) {
        this.metadata = metadata;
        this.path = path;
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return MAVEN_METADATA_XML;
    }

    @Deprecated
    @Override
    public File getFile() {
        return path != null ? path.toFile() : null;
    }

    @Override
    public Path getPath() {
        return path;
    }

    public void merge(File existing, File result) throws RepositoryException {
        merge(existing != null ? existing.toPath() : null, result != null ? result.toPath() : null);
    }

    @Override
    public void merge(Path existing, Path result) throws RepositoryException {
        Metadata recessive = read(existing);

        merge(recessive);

        write(result, metadata);

        merged = true;
    }

    @Override
    public boolean isMerged() {
        return merged;
    }

    protected abstract void merge(Metadata recessive);

    static Metadata read(Path metadataPath) throws RepositoryException {
        if (!Files.exists(metadataPath)) {
            return Metadata.newInstance();
        }

        try (InputStream input = Files.newInputStream(metadataPath)) {
            return new MetadataStaxReader().read(input, false);
        } catch (IOException | XMLStreamException e) {
            throw new RepositoryException("Could not parse metadata " + metadataPath + ": " + e.getMessage(), e);
        }
    }

    private void write(Path metadataPath, Metadata metadata) throws RepositoryException {
        try {
            Files.createDirectories(metadataPath.getParent());
            try (OutputStream output = Files.newOutputStream(metadataPath)) {
                new MetadataStaxWriter().write(output, metadata);
            }
        } catch (IOException | XMLStreamException e) {
            throw new RepositoryException("Could not write metadata " + metadataPath + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public org.eclipse.aether.metadata.Metadata setProperties(Map<String, String> properties) {
        return this;
    }
}
