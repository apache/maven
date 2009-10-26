package org.apache.maven.repository;

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

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Describes an event to be consumed by {@link LocalRepositoryMaintainer}.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultLocalRepositoryMaintainerEvent
    implements LocalRepositoryMaintainerEvent
{
    private ArtifactRepository localRepository;

    private Artifact artifact;

    private File file;

    public DefaultLocalRepositoryMaintainerEvent( ArtifactRepository localRepository, Artifact artifact, File file )
    {
        this.localRepository = localRepository;
        this.artifact = artifact;
        this.file = ( file != null ) ? file : artifact.getFile();
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    public String getClassifier()
    {
        return artifact.hasClassifier() ? artifact.getClassifier() : "";
    }

    public String getVersion()
    {
        return artifact.getVersion();
    }

    public String getType()
    {
        return artifact.getType();
    }

    public File getFile()
    {
        return file;
    }

}
