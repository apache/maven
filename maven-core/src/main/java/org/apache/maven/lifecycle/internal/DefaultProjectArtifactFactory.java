package org.apache.maven.lifecycle.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;

/**
 * Default component responsible for creation of MavenProject#dependencyArtifacts instances.
 */
@SuppressWarnings( "deprecation" )
@Named
public class DefaultProjectArtifactFactory
    implements ProjectArtifactFactory
{
    private final ArtifactFactory artifactFactory;

    @Inject
    public DefaultProjectArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    @Override
    public Set<Artifact> createArtifacts( MavenProject project )
        throws InvalidDependencyVersionException
    {
        return MavenMetadataSource.createArtifacts( artifactFactory, project.getDependencies(), null, null, project );
    }

}
