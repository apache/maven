package org.apache.maven.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelContainerFactory;
import org.apache.maven.shared.model.ModelEventListener;
import org.apache.maven.shared.model.ModelProperty;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.sonatype.plexus.plugin.manager.PlexusPluginManager;
import org.sonatype.plexus.plugin.manager.PluginMetadata;
import org.sonatype.plexus.plugin.manager.PluginResolutionRequest;
import org.sonatype.plexus.plugin.manager.PluginResolutionResult;

// I need access to the local repository
// i need the remote repositories
// i need filters to keep stuff out of the realm that exists

@Component(role = MavenModelEventListener.class, hint="extensions", instantiationStrategy="per-lookup" )
public class BuildExtensionListener
    implements MavenModelEventListener
{
    @Configuration(value = "true")
    private boolean inBuild = true;

    @Requirement
    PlexusPluginManager pluginManager;
    
    private List<BuildExtension> buildExtensions = new ArrayList<BuildExtension>();
    
    public void fire( List<ModelContainer> modelContainers )
    {        
        if ( !inBuild )
        {
            return;
        }

        for ( ModelContainer mc : modelContainers )
        {
            if ( hasExtension( mc ) )
            {
                buildExtensions.add( new BuildExtension( mc.getProperties() ) );
            }
        }
    }
           
    public List<String> getUris()
    {
        return Arrays.asList( ProjectUri.Build.Extensions.Extension.xUri );
    }

    public Collection<ModelContainerFactory> getModelContainerFactories()
    {
        return Arrays.asList( (ModelContainerFactory) new ArtifactModelContainerFactory() );
    }

    private static boolean hasExtension( ModelContainer container )
    {
        for ( ModelProperty mp : container.getProperties() )
        {
            if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.xUri ) )
            {
                return true;
            }
        }
        return false;
    }

    private static class BuildExtension
    {
        private String groupId;

        private String artifactId;

        private String version;
        
        public BuildExtension( String groupId, String artifactId, String version )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        BuildExtension( List<ModelProperty> modelProperties )
        {
            for ( ModelProperty mp : modelProperties )
            {
                if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.groupId ) )
                {
                    groupId = mp.getValue();
                }
                else if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.artifactId ) )
                {
                    artifactId = mp.getValue();
                }
                else if ( mp.getUri().equals( ProjectUri.Build.Extensions.Extension.version ) )
                {
                    version = mp.getValue();
                }
            }
        }
    }   
    
    // Processing the information that was collected.
    
    public void processModelContainers( MavenSession session )
    {       
        for ( BuildExtension be : buildExtensions )
        {
            PluginResolutionRequest request = new PluginResolutionRequest()
                .setPluginMetadata( new PluginMetadata( be.groupId, be.artifactId, be.version ) )
                .addLocalRepository( session.getRequest().getLocalRepositoryPath() )
                .setRemoteRepositories( convertToMercuryRepositories( session.getRequest().getRemoteRepositories() ) );

            PluginResolutionResult result = null;

            try
            {
                result = pluginManager.resolve( request );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            ClassRealm realm = pluginManager.createClassRealm( result.getArtifacts() );

            realm.display();
            
            List<ComponentDescriptor<?>> components = pluginManager.discoverComponents( realm );            
        }
    } 
    
    List<String> convertToMercuryRepositories( List<ArtifactRepository> repositories )
    {
        List<String> repos = new ArrayList<String>();

        if ( repositories != null )
        {
            for ( ArtifactRepository r : repositories )
            {
                repos.add( r.getUrl() );
            }
        }
        else
        {
            // I'm doing this because I am about to rip the artifact clusterfuck out and
            // replace it with mercury and I don't want to pull in 5 component to make a
            // remote repository. This will do until alpha-2.
            repos.add( "http://repo1.maven.org/maven2" );
        }
        
        return repos;        
    }            
}
