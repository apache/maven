package org.apache.maven.artifact.installer;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;

/**
 * @author <a href="michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public interface ArtifactInstaller
{
    String ROLE = ArtifactInstaller.class.getName();

    /**
     * Install an artifact from a particular directory. The artifact handler is used to determine the filename
     * of the source file.
     *
     * @param basedir         the directory where the artifact is stored
     * @param artifact        the artifact definition
     * @param localRepository the local repository to install into
     * @throws ArtifactInstallationException if an error occurred installing the artifact
     */
    void install( String basedir, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException;


    /**
     * Install an artifact from a particular file.
     *
     * @param source          the file to install
     * @param artifact        the artifact definition
     * @param localRepository the local repository to install into
     * @throws ArtifactInstallationException if an error occurred installing the artifact
     */
    void install( File source, Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException;
}
