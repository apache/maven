package org.apache.maven.plugin.install;

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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractInstallMojo
    extends AbstractPlugin
{

    protected boolean isPom()
    {
        return false;
    }

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        MavenProject project = (MavenProject) request.getParameter( "project" );

        ArtifactInstaller artifactInstaller = (ArtifactInstaller) request.getParameter( "installer" );

        ArtifactRepository localRepository = (ArtifactRepository) request.getParameter( "localRepository" );

        // Install the POM
        Artifact pomArtifact = new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                                                    project.getVersion(), "pom" );

        File pom = new File( project.getFile().getParentFile(), "pom.xml" );

        artifactInstaller.install( pom, pomArtifact, localRepository );

        //Install artifact
        if ( !isPom() )
        {
            Artifact artifact = new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                                                     project.getVersion(), project.getPackaging() );

            artifactInstaller.install( project.getBuild().getDirectory(), artifact, localRepository );
        }
    }
}
