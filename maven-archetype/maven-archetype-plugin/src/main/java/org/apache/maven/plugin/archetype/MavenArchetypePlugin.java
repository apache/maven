package org.apache.maven.plugin.archetype;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @goal create
 *
 * @description Builds archetype containers.
 *
 * @parameter
 *   name="archetype"
 *   type="org.apache.maven.archetype.Archetype"
 *   required="true"
 *   validator=""
 *   expression="#component.org.apache.maven.archetype.Archetype"
 *   description=""
 *
 * @parameter
 *   name="localRepository"
 *   type="org.apache.maven.artifact.ArtifactRepository"
 *   required="true"
 *   validator=""
 *   expression="#localRepository"
 *   description=""
 *
 * @parameter
 *   name="archetypeGroupId"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#archetypeGroupId"
 *   default="maven"
 *   description=""
 *
 * @parameter
 *   name="archetypeArtifactId"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#archetypeArtifactId"
 *   default="maven"
 *   description=""
 *
 * @parameter
 *   name="archetypeVersion"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#archetypeVersion"
 *   default="maven"
 *   description=""
 *
 * @parameter
 *   name="groupId"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#groupId"
 *   default="maven"
 *   description=""
 *
 * @parameter
 *   name="artifactId"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#artifactId"
 *   default="quickstart"
 *   description=""
 *
 * @parameter
 *   name="version"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#version"
 *   default="1.0"
 *   description=""
 *
 * @parameter
 *   name="package"
 *   type="String"
 *   required="true"
 *   validator=""
 *   expression="#package"
 *   default="org.apache.maven.quickstart"
 *   description=""
 */
public class MavenArchetypePlugin
    extends AbstractPlugin
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        // archetypeGroupId
        // archetypeArtifactId
        // archetypeVersion
        //
        // localRepository
        // remoteRepository
        // parameters
        // ----------------------------------------------------------------------

        // When there is no project how do we get the local repository and remote repos.
        // The local repository will always be present but the remote is in the POM except
        // for the super POM ...

        ArtifactRepository localRepository = (ArtifactRepository) request.getParameter( "localRepository" );

        List remoteRepositories = new ArrayList();

        ArtifactRepository remoteRepository = new ArtifactRepository( "remote", "http://repo1.maven.org" );

        remoteRepositories.add( remoteRepository );

        String archetypeGroupId = (String) request.getParameter( "archetypeGroupId" );

        String archetypeArtifactId = (String) request.getParameter( "archetypeArtifactId" );

        String archetypeVersion = (String) request.getParameter( "archetypeVersion" );

        Archetype archetype = (Archetype) request.getParameter( "archetype" );

        request.getParameters().put( "outputDirectory", System.getProperty( "user.dir" ) );

        archetype.createArchetype( archetypeGroupId, archetypeArtifactId, archetypeVersion,
                                   localRepository, remoteRepositories, request.getParameters() );
    }
}
