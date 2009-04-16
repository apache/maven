package org.apache.maven.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.project.builder.ProjectUri;
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
    
    private List<Extension> buildExtensions = new ArrayList<Extension>();
     
    public void fire(Model model)
    {
    	buildExtensions.addAll(new ArrayList<Extension>(model.getBuild().getExtensions()));
    }
           
    public List<String> getUris()
    {
        return Arrays.asList( ProjectUri.Build.Extensions.Extension.xUri );
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
    	if(!inBuild)
    	{
    		return;
    	}
    	
        for ( Extension be : buildExtensions )
        {
            PluginResolutionRequest request = new PluginResolutionRequest()
                .setPluginMetadata( new PluginMetadata( be.getGroupId(), be.getArtifactId(), be.getVersion() ) )
                .addLocalRepository( session.getRequest().getLocalRepositoryPath() )
                .setRemoteRepositories( convertToMercuryRepositories( session.getRequest().getRemoteRepositories() ) );

            PluginResolutionResult result = null;

            try
            {
                result = pluginManager.resolve( request );

                ClassRealm realm = pluginManager.createClassRealm( result.getArtifacts() );

                realm.display();
            
                List<ComponentDescriptor<?>> components = pluginManager.discoverComponents( realm );            
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
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
