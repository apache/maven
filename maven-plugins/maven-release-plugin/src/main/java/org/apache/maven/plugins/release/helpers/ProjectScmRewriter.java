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
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ProjectScmRewriter
{

    private Map originalScmInformation = new HashMap();

    public void rewriteScmInfo( MavenProject project, String tagLabel )
        throws MojoExecutionException
    {
        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        if ( originalScmInformation.containsKey( projectId ) )
        {
            throw new IllegalArgumentException( "Project: " + projectId +
                " already has it's original SCM info cached. Each project should only be resolved once." );
        }

        if ( project.getScm() == null )
        {
            throw new MojoExecutionException(
                "Project: " + projectId + " does not have a SCM section! Cannot proceed with release." );
        }

        Model model = project.getOriginalModel();

        Scm scm = model.getScm();
        // If SCM is null in original model, it is inherited, no mods needed
        if ( scm != null )
        {
            String tag = scm.getTag();

            String connection = scm.getConnection();

            String developerConnection = scm.getDeveloperConnection();

            String url = scm.getUrl();

            ScmInfo info = new ScmInfo( tag, connection, developerConnection, url );

            originalScmInformation.put( projectId, info );

            rewriteScmConnection( scm, tagLabel );
        }
    }

    public void restoreScmInfo( MavenProject project )
    {
        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        ScmInfo original = (ScmInfo) originalScmInformation.get( projectId );

        if ( original == null )
        {
            throw new IllegalArgumentException(
                "Project \'" + projectId + "\' has not had its SCM info cached. Cannot restore uncached SCM info." );
        }

        original.modify( project );
    }

    // TODO: Add other SCM types for rewriting, and allow other layouts
    private void rewriteScmConnection( Scm scm, String tag )
    {
        if ( scm != null )
        {
            String scmConnection = scm.getConnection();
            if ( scmConnection != null && scmConnection.startsWith( "scm:svn" ) )
            {
                scm.setConnection( convertSvnConnectionString( scmConnection, tag ) );
                scm.setDeveloperConnection( convertSvnConnectionString( scm.getDeveloperConnection(), tag ) );
                scm.setUrl( convertSvnConnectionString( scm.getUrl(), tag ) );
            }
        }
    }

    private String convertSvnConnectionString( String scmConnection, String tag )
    {
        if ( scmConnection.indexOf( "/trunk" ) >= 0 )
        {
            scmConnection = StringUtils.replace( scmConnection, "/trunk", "/tags/" + tag );
        }
        else
        {
            int begin = scmConnection.indexOf( "/branches/" );
            if ( begin >= 0 )
            {
                int end = scmConnection.indexOf( '/', begin + "/branches/".length() );
                scmConnection = scmConnection.substring( 0, begin ) + "/tags/" + tag;
                if ( end >= 0 && end < scmConnection.length() - 1 )
                {
                    scmConnection += scmConnection.substring( end );
                }
            }
        }
        return scmConnection;
    }

    private static class ScmInfo
    {
        private String tag;

        private String connection;

        private String developerConnection;

        private String url;

        ScmInfo( String tag, String connection, String developerConnection, String url )
        {
            this.tag = tag;
            this.connection = connection;
            this.developerConnection = developerConnection;
            this.url = url;
        }

        void modify( MavenProject project )
        {
            Model model = project.getOriginalModel();

            Scm scm = model.getScm();
            if ( scm != null )
            {
                scm.setTag( tag );

                scm.setConnection( connection );

                scm.setDeveloperConnection( developerConnection );

                scm.setUrl( url );
            }
        }
    }

}
