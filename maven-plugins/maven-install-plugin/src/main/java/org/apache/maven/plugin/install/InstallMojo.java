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
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ModelMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal install
 * @description installs project's main artifact in local repository
 * @parameter name="project"
 * type="org.apache.maven.project.MavenProject"
 * required="true"
 * validator=""
 * expression="#project"
 * description=""
 * @parameter name="installer"
 * type="org.apache.maven.artifact.installer.ArtifactInstaller"
 * required="true"
 * validator=""
 * expression="#component.org.apache.maven.artifact.installer.ArtifactInstaller"
 * description=""
 * @parameter name="localRepository"
 * type="org.apache.maven.artifact.repository.ArtifactRepository"
 * required="true"
 * validator=""
 * expression="#localRepository"
 * description=""
 */
public class InstallMojo
    extends AbstractPlugin
{
    private MavenProject project;

    private ArtifactInstaller installer;

    private ArtifactRepository localRepository;

    public void execute()
        throws PluginExecutionException
    {
        Artifact artifact = new DefaultArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                                 project.getPackaging() );

        if ( !"pom".equals( project.getPackaging() ) )
        {
            File pom = new File( project.getFile().getParentFile(), "pom.xml" );
            ArtifactMetadata metadata = new ModelMetadata( artifact, pom );
            artifact.addMetadata( metadata );
        }

        try
        {
            installer.install( project.getBuild().getDirectory(), artifact, localRepository );
        }
        catch ( ArtifactInstallationException e )
        {
            // TODO: install exception that does not give a trace
            throw new PluginExecutionException( "Error installing artifact", e );
        }
    }
}
