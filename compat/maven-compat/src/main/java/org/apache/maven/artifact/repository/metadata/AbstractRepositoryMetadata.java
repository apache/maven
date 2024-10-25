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
package org.apache.maven.artifact.repository.metadata;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.metadata.v4.MetadataStaxReader;
import org.apache.maven.metadata.v4.MetadataStaxWriter;

/**
 * Shared methods of the repository metadata handling.
 *
 */
@Deprecated
public abstract class AbstractRepositoryMetadata implements RepositoryMetadata {
    private static final String LS = System.lineSeparator();

    private Metadata metadata;

    protected AbstractRepositoryMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getRemoteFilename() {
        return "maven-metadata.xml";
    }

    public String getLocalFilename(ArtifactRepository repository) {
        return "maven-metadata-" + repository.getKey() + ".xml";
    }

    public void storeInLocalRepository(ArtifactRepository localRepository, ArtifactRepository remoteRepository)
            throws RepositoryMetadataStoreException {
        try {
            updateRepositoryMetadata(localRepository, remoteRepository);
        } catch (IOException | XMLStreamException e) {
            throw new RepositoryMetadataStoreException("Error updating group repository metadata", e);
        }
    }

    protected void updateRepositoryMetadata(ArtifactRepository localRepository, ArtifactRepository remoteRepository)
            throws IOException, XMLStreamException {
        Metadata metadata = null;

        File metadataFile = new File(
                localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(this, remoteRepository));

        if (metadataFile.length() == 0) {
            if (!metadataFile.delete()) {
                // sleep for 10ms just in case this is windows holding a file lock
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // ignore
                }
                metadataFile.delete(); // if this fails, forget about it, we'll try to overwrite it anyway so no need
                // to delete on exit
            }
        } else if (metadataFile.exists()) {
            try (InputStream input = Files.newInputStream(metadataFile.toPath())) {
                metadata = new Metadata(new MetadataStaxReader().read(input, false));
            }
        }

        boolean changed;

        // If file could not be found or was not valid, start from scratch
        if (metadata == null) {
            metadata = this.metadata;

            changed = true;
        } else {
            changed = metadata.merge(this.metadata);
        }

        // beware meta-versions!
        String version = metadata.getVersion();
        if (Artifact.LATEST_VERSION.equals(version) || Artifact.RELEASE_VERSION.equals(version)) {
            // meta-versions are not valid <version/> values...don't write them.
            metadata.setVersion(null);
        }

        if (changed || !metadataFile.exists()) {
            metadataFile.getParentFile().mkdirs();
            try (OutputStream output = Files.newOutputStream(metadataFile.toPath())) {
                MetadataStaxWriter mappingWriter = new MetadataStaxWriter();
                mappingWriter.write(output, metadata.getDelegate());
            }
        } else {
            metadataFile.setLastModified(System.currentTimeMillis());
        }
    }

    public String toString() {
        return "repository metadata for: '" + getKey() + "'";
    }

    protected static Metadata createMetadata(Artifact artifact, Versioning versioning) {
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifact.getGroupId());
        metadata.setArtifactId(artifact.getArtifactId());
        metadata.setVersion(artifact.getVersion());
        metadata.setVersioning(versioning);
        return metadata;
    }

    protected static Versioning createVersioning(Snapshot snapshot) {
        Versioning versioning = new Versioning();
        versioning.setSnapshot(snapshot);
        return versioning;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void merge(org.apache.maven.repository.legacy.metadata.ArtifactMetadata metadata) {
        // TODO not sure that it should assume this, maybe the calls to addMetadata should pre-merge, then artifact
        // replaces?
        AbstractRepositoryMetadata repoMetadata = (AbstractRepositoryMetadata) metadata;
        this.metadata.merge(repoMetadata.getMetadata());
    }

    public void merge(ArtifactMetadata metadata) {
        // TODO not sure that it should assume this, maybe the calls to addMetadata should pre-merge, then artifact
        // replaces?
        AbstractRepositoryMetadata repoMetadata = (AbstractRepositoryMetadata) metadata;
        this.metadata.merge(repoMetadata.getMetadata());
    }

    public String extendedToString() {
        StringBuilder buffer = new StringBuilder(256);

        buffer.append(LS).append("Repository Metadata").append(LS).append("--------------------------");
        buffer.append(LS).append("GroupId: ").append(getGroupId());
        buffer.append(LS).append("ArtifactId: ").append(getArtifactId());
        buffer.append(LS).append("Metadata Type: ").append(getClass().getName());

        return buffer.toString();
    }

    public int getNature() {
        return RELEASE;
    }

    public ArtifactRepositoryPolicy getPolicy(ArtifactRepository repository) {
        int nature = getNature();
        if ((nature & RepositoryMetadata.RELEASE_OR_SNAPSHOT) == RepositoryMetadata.RELEASE_OR_SNAPSHOT) {
            ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy(repository.getReleases());
            policy.merge(repository.getSnapshots());
            return policy;
        } else if ((nature & RepositoryMetadata.SNAPSHOT) != 0) {
            return repository.getSnapshots();
        } else {
            return repository.getReleases();
        }
    }
}
