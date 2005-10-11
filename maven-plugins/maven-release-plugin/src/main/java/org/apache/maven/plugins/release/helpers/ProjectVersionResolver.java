package org.apache.maven.plugins.release.helpers;

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

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProjectVersionResolver
{
    private static final String SNAPSHOT_CLASSIFIER = "-SNAPSHOT";

    private Map resolvedVersions = new HashMap();

    private final Log log;

    private final InputHandler inputHandler;

    private final boolean interactive;

    public ProjectVersionResolver( Log log, InputHandler inputHandler, boolean interactive )
    {
        this.log = log;
        this.inputHandler = inputHandler;
        this.interactive = interactive;
    }

    public void resolveVersion( Model model, String projectId )
        throws MojoExecutionException
    {
        if ( resolvedVersions.containsKey( projectId ) )
        {
            throw new IllegalArgumentException(
                "Project: " + projectId + " is already resolved. Each project should only be resolved once." );
        }

        //Rewrite project version
        String projectVersion = model.getVersion();

        projectVersion = projectVersion.substring( 0, projectVersion.length() - SNAPSHOT_CLASSIFIER.length() );

        if ( interactive )
        {
            try
            {
                log.info( "What is the release version for \'" + projectId + "\'? [" + projectVersion + "]" );

                String inputVersion = inputHandler.readLine();

                if ( !StringUtils.isEmpty( inputVersion ) )
                {
                    projectVersion = inputVersion;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Can't read release version from user input.", e );
            }
        }

        model.setVersion( projectVersion );

        resolvedVersions.put( projectId, projectVersion );
    }

    public String getResolvedVersion( String groupId, String artifactId )
    {
        String projectId = ArtifactUtils.versionlessKey( groupId, artifactId );

        return (String) resolvedVersions.get( projectId );
    }

    public void incrementVersion( Model model, String projectId )
        throws MojoExecutionException
    {
        String projectVersion = model.getVersion();

        if ( ArtifactUtils.isSnapshot( projectVersion ) )
        {
            throw new MojoExecutionException( "The project " + projectId + " is a snapshot (" + projectVersion +
                "). It appears that the release version has not been committed." );
        }

        // TODO: we will need to incorporate versioning strategies here because it is unlikely
        // that everyone will be able to agree on a standard. This is extremely limited right
        // now and really only works for the way maven is versioned.

        // releaseVersion = 1.0-beta-4
        // snapshotVersion = 1.0-beta-5-SNAPSHOT
        // or
        // releaseVersion = 1.0.4
        // snapshotVersion = 1.0.5-SNAPSHOT

        String staticVersionPart;
        String nextVersionString;

        int rcIdx = projectVersion.toLowerCase().lastIndexOf( "-rc" );
        int dashIdx = projectVersion.lastIndexOf( "-" );
        int dotIdx = projectVersion.lastIndexOf( "." );

        if ( rcIdx >= dashIdx )
        {
            staticVersionPart = projectVersion.substring( 0, rcIdx + 3 );
            nextVersionString = projectVersion.substring( rcIdx + 3 );
        }
        else if ( dashIdx > 0 )
        {
            staticVersionPart = projectVersion.substring( 0, dashIdx + 1 );
            nextVersionString = projectVersion.substring( dashIdx + 1 );
        }
        else if ( dotIdx > 0 )
        {
            staticVersionPart = projectVersion.substring( 0, dotIdx + 1 );
            nextVersionString = projectVersion.substring( dotIdx + 1 );
        }
        else
        {
            staticVersionPart = "";
            nextVersionString = projectVersion;
        }

        try
        {
            nextVersionString = Integer.toString( Integer.parseInt( nextVersionString ) + 1 );

            projectVersion = staticVersionPart + nextVersionString + SNAPSHOT_CLASSIFIER;
        }
        catch ( NumberFormatException e )
        {
            projectVersion = "";
        }

        if ( interactive )
        {
            try
            {
                log.info( "What is the new development version for \'" + projectId + "\'? [" + projectVersion + "]" );

                String inputVersion = inputHandler.readLine();

                if ( !StringUtils.isEmpty( inputVersion ) )
                {
                    projectVersion = inputVersion;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Can't read next development version from user input.", e );
            }
        }
        else if ( "".equals( projectVersion ) )
        {
            throw new MojoExecutionException( "Cannot determine incremented development version for: " + projectId );
        }

        model.setVersion( projectVersion );

        resolvedVersions.put( projectId, projectVersion );
    }

}
