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

/**
 * This listener has two parts: the collection of the extension elements which happens during POM construction,
 * and the processing of the build extensions once the construction is finished and the build plan is being
 * created. The extensions that are found can be contributed to a Mercury session where a set of artifacts
 * are retrieved and any number of extensions may be required. We don't want to load them as they are discovered
 * because that prevents any sort of analysis so we collect, analyze and process.
 * 
 * @author Jason van Zyl
 *
 */
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
    
    /**
     * Take the extension elements that were found during the POM construction process and now
     * retrieve all the artifacts necessary, load them in a realm, and discovery the components
     * that are in the realm. Any components that are discovered will be available to lookups
     * in the container from any location and the right classloader will be used to execute
     * any components discovered in the extension realm.
     * 
     * @param session Maven session used as the execution context for the current Maven project.
     */
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
            // This will only ever be use for the test that I have. So yes this will break
            // folks behind proxies until alpha-2. Such is life.
            repos.add( "http://repo1.maven.org/maven2" );
        }
        
        return repos;        
    }            
}
