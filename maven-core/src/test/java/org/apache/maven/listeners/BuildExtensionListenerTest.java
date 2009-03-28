package org.apache.maven.listeners;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerAction;
import org.apache.maven.shared.model.ModelProperty;
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

        // Create the model properties and the model container to feed to the event firing
        List<ModelProperty> modelProperties = new ArrayList<ModelProperty>();
        modelProperties.add( new ModelProperty( ProjectUri.Build.Extensions.Extension.xUri, null ) );
        modelProperties.add( new ModelProperty( ProjectUri.Build.Extensions.Extension.groupId, "org.apache.maven.wagon" ) );
        modelProperties.add( new ModelProperty( ProjectUri.Build.Extensions.Extension.artifactId, "wagon-webdav" ) );
        modelProperties.add( new ModelProperty( ProjectUri.Build.Extensions.Extension.version, "1.0-beta-2" ) );
        ModelContainer container = new TestModelContainer( modelProperties );

        // Fire the event.
        listener.fire( Arrays.asList( container ) );

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
    //    lookup( Wagon.class, "dav" );        
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
        MavenSession session = new MavenSession( getContainer(), request );

        return session;
    }

    public class TestModelContainer
        implements ModelContainer
    {
        List<ModelProperty> modelProperties;

        public TestModelContainer( List<ModelProperty> properties )
        {
            this.modelProperties = properties;
        }

        public List<ModelProperty> getProperties()
        {
            return new ArrayList<ModelProperty>( modelProperties );
        }

        public ModelContainerAction containerAction( ModelContainer modelContainer )
        {
            return null;
        }

        public ModelContainer createNewInstance( List<ModelProperty> modelProperties )
        {
            return null;
        }
    }
}
