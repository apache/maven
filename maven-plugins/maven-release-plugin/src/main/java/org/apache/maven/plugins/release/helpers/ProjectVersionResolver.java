package org.apache.maven.plugins.release.helpers;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.inputhandler.InputHandler;
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

    public void resolveVersion( MavenProject project )
        throws MojoExecutionException
    {
        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        if ( resolvedVersions.containsKey( projectId ) )
        {
            throw new IllegalArgumentException( "Project: " + projectId
                + " is already resolved. Each project should only be resolved once." );
        }

        //Rewrite project version
        String projectVersion = project.getVersion();

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
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Can't read release version from user input.", e );
            }
        }

        project.setVersion( projectVersion );

        resolvedVersions.put( projectId, projectVersion );
    }

    public String getResolvedVersion( String groupId, String artifactId )
    {
        String projectId = ArtifactUtils.versionlessKey( groupId, artifactId );

        return (String) resolvedVersions.get( projectId );
    }

    public void incrementVersion( MavenProject project )
        throws MojoExecutionException
    {
        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        String projectVersion = (String) resolvedVersions.get( projectId );

        if ( projectVersion == null )
        {
            throw new IllegalArgumentException( "Project \'" + projectId
                + "\' has not been resolved. Cannot increment an unresolved version." );
        }

        // TODO: we will need to incorporate versioning strategies here because it is unlikely
        // that everyone will be able to agree on a standard. This is extremely limited right
        // now and really only works for the way maven is versioned.

        // releaseVersion = 1.0-beta-4
        // snapshotVersion = 1.0-beta-5-SNAPSHOT

        String nextVersionString = projectVersion.substring( projectVersion.lastIndexOf( "-" ) + 1 );

        try
        {
            nextVersionString = Integer.toString( Integer.parseInt( nextVersionString ) + 1 );

            projectVersion = projectVersion.substring( 0, projectVersion.lastIndexOf( "-" ) + 1 ) + nextVersionString
                + SNAPSHOT_CLASSIFIER;
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

                project.setVersion( projectVersion );

                resolvedVersions.put( projectId, projectVersion );
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
    }

}
