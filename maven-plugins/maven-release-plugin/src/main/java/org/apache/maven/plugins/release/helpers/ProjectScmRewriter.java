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

        Model model = project.getModel();

        Scm scm = model.getScm();

        if ( scm == null )
        {
            throw new MojoExecutionException(
                "Project: " + projectId + " does not have a SCM section! Cannot proceed with release." );
        }

        String tag = model.getScm().getTag();

        String connection = model.getScm().getConnection();

        String developerConnection = model.getScm().getDeveloperConnection();

        ScmInfo info = new ScmInfo( tag, connection, developerConnection );

        originalScmInformation.put( projectId, info );

        scm.setTag( tagLabel );

        scm.setConnection( rewriteScmConnection( connection, tagLabel ) );

        scm.setDeveloperConnection( rewriteScmConnection( developerConnection, tagLabel ) );
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

    // TODO: Add other SCM types for rewriting...
    private String rewriteScmConnection( String scmConnection, String tag )
    {
        if ( scmConnection != null )
        {
            if ( scmConnection.startsWith( "svn" ) )
            {
                if ( scmConnection.endsWith( "trunk/" ) )
                {
                    scmConnection = scmConnection.substring( 0, scmConnection.length() - "trunk/".length() );
                }
                if ( scmConnection.endsWith( "branches/" ) )
                {
                    scmConnection = scmConnection.substring( 0, scmConnection.length() - "branches/".length() );
                }
                scmConnection += "tags/" + tag;
            }
        }

        return scmConnection;
    }

    private static class ScmInfo
    {
        private String tag;

        private String connection;

        private String developerConnection;

        ScmInfo( String tag, String connection, String developerConnection )
        {
            this.tag = tag;
            this.connection = connection;
            this.developerConnection = developerConnection;
        }

        void modify( MavenProject project )
        {
            Model model = project.getModel();

            if ( model.getScm() != null )
            {
                model.getScm().setTag( tag );

                model.getScm().setConnection( connection );

                model.getScm().setDeveloperConnection( developerConnection );
            }
        }
    }

}
