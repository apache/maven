package org.apache.maven.plugin.archetype;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;

import java.util.HashSet;
import java.util.Set;

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
        // archetypeId
        // localRepository
        // remoteRepository
        // parameters
        // ----------------------------------------------------------------------

        // When there is no project how do we get the local repository and remote repos.
        // The local repository will always be present but the remote is in the POM except
        // for the super POM ...

        ArtifactRepository localRepository = (ArtifactRepository) request.getParameter( "localRepository" );

        Set remoteRepositories = new HashSet();

        ArtifactRepository remoteRepository = new ArtifactRepository( "remote", "http://repo1.maven.org" );

        remoteRepositories.add( remoteRepository );

        String archetypeId = (String) request.getParameter( "archetypeId" );

        Archetype archetype = (Archetype) request.getParameter( "archetype" );

        request.getParameters().put( "outputDirectory", System.getProperty( "user.dir" ) );

        archetype.createArchetype( "quickstart", localRepository, remoteRepositories, request.getParameters() );
    }
}
