package org.apache.maven.listeners;

import java.io.File;
import java.util.Collections;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.dag.CycleDetectedException;

//TODO: use the AbstractCoreMavenComponentTest
public class BuildExtensionListenerTest
    extends PlexusTestCase
{
    public void testBuildExtensionListener()
        throws Exception
    {
        BuildExtensionListener listener = (BuildExtensionListener) lookup( MavenModelEventListener.class, "extensions" );

        Extension extension = new Extension();
        extension.setGroupId("org.apache.maven.wagon" );
        extension.setArtifactId("wagon-webdav" );
        extension.setVersion( "1.0-beta-2" );
        
        Build build = new Build();
        build.addExtension(extension);
        
        Model model = new Model();
        model.setBuild(build);
        
        // Fire the event.
        listener.fire( model );

        try
        {
            lookup( Wagon.class, "dav" );
            fail( "The lookup for the wagon dav extension should not be possible yet." );
        }
        catch( Exception e )
        {
            // We should get an exception.
        }
        
        // Process the data we have collected.
        listener.processModelContainers( newMavenSession() );
        
        // Now we should be able to find the extension.
        lookup( Wagon.class, "dav" );    
    }

    private MavenSession newMavenSession()
        throws CycleDetectedException, DuplicateProjectException
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        MavenProject project = new MavenProject( model );
        ReactorManager rm = new ReactorManager( Collections.singletonList( project ), ReactorManager.FAIL_FAST );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepositoryPath( new File( System.getProperty( "user.home" ), ".m2/repository" ) );        
        MavenSession session = new MavenSession( request );

        return session;
    }

}
