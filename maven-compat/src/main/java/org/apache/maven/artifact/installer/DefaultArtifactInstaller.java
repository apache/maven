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
package org.apache.maven.artifact.installer;

import java.io.File;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.MetadataBridge;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * @author Jason van Zyl
 */
@Component(role = ArtifactInstaller.class)
public class DefaultArtifactInstaller extends AbstractLogEnabled implements ArtifactInstaller {

    @Requirement
    private RepositorySystem repoSystem;

    @Requirement
    private LegacySupport legacySupport;

    /** @deprecated we want to use the artifact method only, and ensure artifact.file is set correctly. */
    @Deprecated
    public void install(String basedir, String finalName, Artifact artifact, ArtifactRepository localRepository)
            throws ArtifactInstallationException {
        String extension = artifact.getArtifactHandler().getExtension();
        File source = new File(basedir, finalName + "." + extension);

        install(source, artifact, localRepository);
    }

    public void install(File source, Artifact artifact, ArtifactRepository localRepository)
            throws ArtifactInstallationException {
        RepositorySystemSession session =
                LegacyLocalRepositoryManager.overlay(localRepository, legacySupport.getRepositorySession(), repoSystem);

        InstallRequest request = new InstallRequest();

        request.setTrace(RequestTrace.newChild(null, legacySupport.getSession().getCurrentProject()));

        org.eclipse.aether.artifact.Artifact mainArtifact = RepositoryUtils.toArtifact(artifact);
        mainArtifact = mainArtifact.setFile(source);
        request.addArtifact(mainArtifact);

        for (ArtifactMetadata metadata : artifact.getMetadataList()) {
            if (metadata instanceof ProjectArtifactMetadata) {
                org.eclipse.aether.artifact.Artifact pomArtifact = new SubArtifact(mainArtifact, "", "pom");
                pomArtifact = pomArtifact.setFile(((ProjectArtifactMetadata) metadata).getFile());
                request.addArtifact(pomArtifact);
            } else if (metadata instanceof SnapshotArtifactRepositoryMetadata
                    || metadata instanceof ArtifactRepositoryMetadata) {
                // eaten, handled by repo system
            } else {
                request.addMetadata(new MetadataBridge(metadata));
            }
        }

        try {
            repoSystem.install(session, request);
        } catch (InstallationException e) {
            throw new ArtifactInstallationException(e.getMessage(), e);
        }

        /*
         * NOTE: Not used by Maven core, only here to provide backward-compat with plugins like the Install Plugin.
         */

        if (artifact.isSnapshot()) {
            Snapshot snapshot = new Snapshot();
            snapshot.setLocalCopy(true);
            artifact.addMetadata(new SnapshotArtifactRepositoryMetadata(artifact, snapshot));
        }

        Versioning versioning = new Versioning();
        // TODO Should this be changed for MNG-6754 too?
        versioning.updateTimestamp();
        versioning.addVersion(artifact.getBaseVersion());
        if (artifact.isRelease()) {
            versioning.setRelease(artifact.getBaseVersion());
        }
        artifact.addMetadata(new ArtifactRepositoryMetadata(artifact, versioning));
    }
}
