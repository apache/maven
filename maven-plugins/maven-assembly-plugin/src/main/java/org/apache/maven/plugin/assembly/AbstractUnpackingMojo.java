package org.apache.maven.plugin.assembly;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Base routines for assembly and unpack goals.
 *
 * @version $Id$
 */
public abstract class AbstractUnpackingMojo
    extends AbstractMojo
{
    protected static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * The output directory of the assembled distribution file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * The filename of the assembled distribution file.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /**
     * Directory to unpack JARs into if needed
     *
     * @parameter expression="${project.build.directory}/assembly/work"
     * @required
     */
    protected File workDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */

    protected ArchiverManager archiverManager;

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * Creates a Map of artifacts within the reactor using the groupId:artifactId:version as key
     *
     * @return a HashMap of all artifacts available in the reactor
     */
    protected Map getMappedReactorArtifacts()
    {
        Map mappedReactorArtifacts = new HashMap();

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) i.next();

            String key = reactorProject.getGroupId() + ":" + reactorProject.getArtifactId() + ":"
                + reactorProject.getVersion();

            mappedReactorArtifacts.put( key, reactorProject.getArtifact() );
        }

        return mappedReactorArtifacts;
    }

    /**
     * Retrieves all artifact dependencies within the reactor
     *
     * @return A HashSet of artifacts
     */
    protected Set getDependencies()
    {
        Map reactorArtifacts = getMappedReactorArtifacts();

        Map dependencies = new HashMap();

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject reactorProject = (MavenProject) i.next();

            for ( Iterator j = reactorProject.getArtifacts().iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();

                if ( !reactorArtifacts.containsKey( key ) && !dependencies.containsKey( key ) )
                {
                    dependencies.put( key, artifact );
                }
            }
        }

        return new HashSet( dependencies.values() );
    }

    /**
     * Unpacks the archive file.
     *
     * @param file File to be unpacked.
     * @param location Location where to put the unpacked files.
     */
    protected void unpack( File file, File location )
        throws MojoExecutionException, NoSuchArchiverException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() ).toLowerCase();

        try
        {
            UnArchiver unArchiver;

            unArchiver = this.archiverManager.getUnArchiver( archiveExt );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

}
