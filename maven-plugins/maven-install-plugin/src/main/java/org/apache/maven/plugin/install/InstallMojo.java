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
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ReleaseArtifactMetadata;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Installs project's main artifact in local repository.
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal install
 * @phase install
 */
public class InstallMojo
    extends AbstractInstallMojo
{
    /**
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private String buildDirectory;

    /**
     * @parameter alias="archiveName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${updateReleaseInfo}"
     */
    private boolean updateReleaseInfo = false;
    
    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

    public void execute()
        throws MojoExecutionException
    {
        boolean isPomArtifact = "pom".equals( packaging );
        
        File pom = new File( basedir, "pom.xml" );
        if ( !isPomArtifact )
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pom );
            artifact.addMetadata( metadata );
        }

        if ( updateReleaseInfo )
        {
            ReleaseArtifactMetadata metadata = new ReleaseArtifactMetadata( artifact );
            metadata.setVersion( artifact.getVersion() );
            artifact.addMetadata( metadata );
        }
        
        try
        {
            if ( isPomArtifact )
            {
                installer.install( pom, artifact, localRepository );
            }
            else
            {
                // TODO: would be something nice to get back from the project to get the full filename (the OGNL feedback thing)
                installer.install( buildDirectory, finalName, artifact, localRepository );
            }

            for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
            {
                Artifact attached = (Artifact) i.next();
                installer.install( attached.getFile(), attached, localRepository );
            }
        }
        catch ( ArtifactInstallationException e )
        {
            // TODO: install exception that does not give a trace
            throw new MojoExecutionException( "Error installing artifact", e );
        }
    }
}
