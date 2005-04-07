package org.apache.maven.plugin.archetype;

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

import org.apache.maven.archetype.Archetype;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.io.File;
import java.util.List;

/**
 * @goal create
 * @description Builds archetype containers.
 * @parameter name="archetype"
 * type="org.apache.maven.archetype.Archetype"
 * required="true"
 * validator=""
 * expression="#component.org.apache.maven.archetype.Archetype"
 * description=""
 * @parameter name="localRepository"
 * type="org.apache.maven.artifact.ArtifactRepository"
 * required="true"
 * validator=""
 * expression="#localRepository"
 * description=""
 * @parameter name="archetypeGroupId"
 * type="String"
 * required="true"
 * validator=""
 * expression="#archetypeGroupId"
 * default="org.apache.maven.archetypes"
 * description=""
 * @parameter name="archetypeArtifactId"
 * type="String"
 * required="true"
 * validator=""
 * expression="#archetypeArtifactId"
 * default="maven-archetype-quickstart"
 * description=""
 * @parameter name="archetypeVersion"
 * type="String"
 * required="true"
 * validator=""
 * expression="#archetypeVersion"
 * default="1.0-alpha-1-SNAPSHOT"
 * description=""
 * @parameter name="groupId"
 * type="String"
 * required="true"
 * validator=""
 * expression="#groupId"
 * description=""
 * @parameter name="artifactId"
 * type="String"
 * required="true"
 * validator=""
 * expression="#artifactId"
 * description=""
 * @parameter name="version"
 * type="String"
 * required="true"
 * validator=""
 * expression="#version"
 * default="1.0-SNAPSHOT"
 * description=""
 * @parameter name="package"
 * type="String"
 * required="true"
 * validator=""
 * expression="#package"
 * default="com.mycompany.app"
 * description=""
 * @parameter name="remoteRepositories"
 * type="java.util.List"
 * required="true"
 * validator=""
 * expression="#project.remoteArtifactRepositories"
 * description=""
 */
public class MavenArchetypePlugin
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // TODO: prompt for missing values
        // TODO: use new plugin type
        // TODO: configurable license

        // ----------------------------------------------------------------------
        // archetypeGroupId
        // archetypeArtifactId
        // archetypeVersion
        //
        // localRepository
        // remoteRepository
        // parameters
        // ----------------------------------------------------------------------

        ArtifactRepository localRepository = (ArtifactRepository) request.getParameter( "localRepository" );

        // From the super POM
        List remoteRepositories = (List) request.getParameter( "remoteRepositories" );

        String archetypeGroupId = (String) request.getParameter( "archetypeGroupId" );

        String archetypeArtifactId = (String) request.getParameter( "archetypeArtifactId" );

        String archetypeVersion = (String) request.getParameter( "archetypeVersion" );

        Archetype archetype = (Archetype) request.getParameter( "archetype" );

        String basedir = System.getProperty( "user.dir" );

        // TODO: allow this to be configured
        File outputDirectory = new File( basedir, (String) request.getParameter( "artifactId" ) );
        request.getParameters().put( "outputDirectory", outputDirectory.getAbsolutePath() );

        if ( outputDirectory.exists() )
        {
            throw new PluginExecutionException(
                outputDirectory.getName() + " already exists - please run from a clean directory" );
        }

        archetype.createArchetype( archetypeGroupId, archetypeArtifactId, archetypeVersion, localRepository,
                                   remoteRepositories, request.getParameters() );
    }
}
