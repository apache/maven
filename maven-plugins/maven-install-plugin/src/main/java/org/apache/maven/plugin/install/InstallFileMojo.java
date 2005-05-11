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
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Installs a file in local repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal install-file
 */
public class InstallFileMojo
    extends AbstractInstallMojo
{
    /**
     * @parameter expression="${groupId}"
     * @required
     * @readonly
     */
    protected String groupId;

    /**
     * @parameter expression="${artifactId}"
     * @required
     * @readonly
     */
    protected String artifactId;

    /**
     * @parameter expression="${version}"
     * @required
     * @readonly
     */
    protected String version;

    /**
     * @parameter expression="${packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${file}"
     * @required
     * @readonly
     */
    private File file;

    public void execute()
        throws MojoExecutionException
    {
        Artifact artifact = new DefaultArtifact( groupId, artifactId, version, packaging );

        try
        {
            installer.install( file, artifact, localRepository );
        }
        catch ( ArtifactInstallationException e )
        {
            // TODO: install exception that does not give a trace
            throw new MojoExecutionException( "Error installing artifact", e );
        }
    }
}
