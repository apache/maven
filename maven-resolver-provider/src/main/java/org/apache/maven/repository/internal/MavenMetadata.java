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
package org.apache.maven.repository.internal;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.MetadataStaxReader;
import org.apache.maven.artifact.repository.metadata.io.MetadataStaxWriter;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.metadata.AbstractMetadata;
import org.eclipse.aether.metadata.MergeableMetadata;

/**
 */
abstract class MavenMetadata extends AbstractMetadata implements MergeableMetadata {

    static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    protected Metadata metadata;

    private final File file;

    protected final Date timestamp;

    private boolean merged;

    protected MavenMetadata(Metadata metadata, File file, Date timestamp) {
        this.metadata = metadata;
        this.file = file;
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return MAVEN_METADATA_XML;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void merge(File existing, File result) throws RepositoryException {
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

    static Metadata read(File metadataFile) throws RepositoryException {
        if (metadataFile.length() <= 0) {
            return new Metadata();
        }

        try (InputStream input = Files.newInputStream(metadataFile.toPath())) {
            return new Metadata(new MetadataStaxReader().read(input, false));
        } catch (IOException | XMLStreamException e) {
            throw new RepositoryException("Could not parse metadata " + metadataFile + ": " + e.getMessage(), e);
        }
    }

    private void write(File metadataFile, Metadata metadata) throws RepositoryException {
        metadataFile.getParentFile().mkdirs();
        try (OutputStream output = Files.newOutputStream(metadataFile.toPath())) {
            new MetadataStaxWriter().write(output, metadata.getDelegate());
        } catch (IOException | XMLStreamException e) {
            throw new RepositoryException("Could not write metadata " + metadataFile + ": " + e.getMessage(), e);
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
