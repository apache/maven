package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.mapping.MappedPlugin;
import org.apache.maven.plugin.mapping.PluginMap;
import org.apache.maven.plugin.mapping.io.xpp3.PluginMappingXpp3Reader;
import org.apache.maven.plugin.mapping.io.xpp3.PluginMappingXpp3Writer;
import org.apache.maven.plugin.mapping.metadata.PluginMappingMetadata;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscovererManager;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.discovery.DefaultComponentDiscoverer;
import org.codehaus.plexus.component.discovery.PlexusXmlComponentDiscoverer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @phase package
 * @goal generateUpdatedMapping
 */
public class GenerateUpdatedMappingMojo
    extends AbstractMojo
{

    /**
     * @parameter
     */
    private String goalPrefix;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}/repository-metadata"
     * @required
     * @readonly
     */
    private String metadataDirectory;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private String classesDirectory;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager}"
     * @required
     * @readonly
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    public void execute()
        throws MojoExecutionException
    {
        ArtifactRepository distributionRepository = project.getDistributionManagementArtifactRepository();

        String distributionRepositoryId = distributionRepository.getId();

        List remoteArtifactRepositories = project.getRemoteArtifactRepositories();

        ArtifactRepository readRemoteRepository = null;

        for ( Iterator it = remoteArtifactRepositories.iterator(); it.hasNext(); )
        {
            ArtifactRepository currentRepository = (ArtifactRepository) it.next();

            if ( distributionRepositoryId.equals( currentRepository.getId() ) )
            {
                readRemoteRepository = currentRepository;

                break;
            }
        }

        PluginMappingXpp3Reader mappingReader = new PluginMappingXpp3Reader();

        PluginMap pluginMap = null;

        RepositoryMetadata metadata = new PluginMappingMetadata( project.getGroupId() );

        try
        {
            repositoryMetadataManager.resolve( metadata, readRemoteRepository, localRepository );

            Reader reader = null;

            File metadataFile = metadata.getFile();

            try
            {
                reader = new FileReader( metadataFile );

                pluginMap = mappingReader.read( reader );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot read plugin-mapping metadata from file: " + metadataFile, e );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException( "Cannot parse plugin-mapping metadata from file: " + metadataFile, e );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }
        catch ( RepositoryMetadataManagementException e )
        {
            Throwable cause = e.getCause();

            if ( cause != null && ( cause instanceof ResourceDoesNotExistException ) )
            {
                getLog().info( "Cannot find " + metadata + " on remote repository. Creating a new one." );
                getLog().debug( "Metadata " + metadata + " cannot be resolved.", e );

                pluginMap = new PluginMap();
                pluginMap.setGroupId( project.getGroupId() );
            }
            else
            {
                throw new MojoExecutionException( "Failed to resolve " + metadata, e );
            }
        }

        for ( Iterator it = pluginMap.getPlugins().iterator(); it.hasNext(); )
        {
            MappedPlugin preExisting = (MappedPlugin) it.next();

            if ( preExisting.getArtifactId().equals( project.getArtifactId() ) )
            {
                getLog().info( "Updating pre-existing plugin-mapping metadata." );

                pluginMap.removePlugin( preExisting );

                break;
            }
        }

        MappedPlugin mappedPlugin = new MappedPlugin();

        mappedPlugin.setArtifactId( project.getArtifactId() );

        mappedPlugin.setPrefix( getGoalPrefix() );

        mappedPlugin.setPackagingHandlers( extractPackagingHandlers() );

        pluginMap.addPlugin( mappedPlugin );

        Writer writer = null;
        try
        {
            File updatedMetadataFile = new File( metadataDirectory, metadata.getRepositoryPath() ).getAbsoluteFile();

            File dir = updatedMetadataFile.getParentFile();

            if ( !dir.exists() )
            {
                dir.mkdirs();
            }

            writer = new FileWriter( updatedMetadataFile );

            PluginMappingXpp3Writer mappingWriter = new PluginMappingXpp3Writer();

            mappingWriter.write( writer, pluginMap );

            metadata.setFile( updatedMetadataFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing repository metadata to build directory.", e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private List extractPackagingHandlers()
        throws MojoExecutionException
    {
        List packagingHandlers = new ArrayList();

        Context ctx = new DefaultContext();

        ClassWorld discoveryWorld = new ClassWorld();

        try
        {
            ClassRealm discoveryRealm = discoveryWorld.newRealm( "packageHandler-discovery" );

            File classDir = new File( classesDirectory ).getAbsoluteFile();

            discoveryRealm.addConstituent( classDir.toURL() );

            packagingHandlers
                .addAll( discoverLifecycleMappings( ctx, discoveryRealm, new DefaultComponentDiscoverer() ) );

            packagingHandlers.addAll( discoverLifecycleMappings( ctx, discoveryRealm,
                                                                 new PlexusXmlComponentDiscoverer() ) );
        }
        catch ( DuplicateRealmException e )
        {
            throw new MojoExecutionException( "Error constructing class-realm for lifecycle-mapping detection.", e );
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Error constructing class-realm for lifecycle-mapping detection.", e );
        }
        catch ( PlexusConfigurationException e )
        {
            throw new MojoExecutionException( "Error detecting lifecycle-mappings.", e );
        }

        return packagingHandlers;
    }

    private List discoverLifecycleMappings( Context ctx, ClassRealm discoveryRealm, ComponentDiscoverer discoverer )
        throws PlexusConfigurationException
    {
        discoverer.setManager( new DummyComponentDiscovererManager() );
        
        List packagingHandlers = new ArrayList();

        List componentSetDescriptors = discoverer.findComponents( ctx, discoveryRealm );

        if ( componentSetDescriptors != null )
        {
            for ( Iterator it = componentSetDescriptors.iterator(); it.hasNext(); )
            {
                ComponentSetDescriptor setDescriptor = (ComponentSetDescriptor) it.next();

                List components = setDescriptor.getComponents();

                if ( components != null )
                {
                    for ( Iterator componentIterator = components.iterator(); componentIterator.hasNext(); )
                    {
                        ComponentDescriptor descriptor = (ComponentDescriptor) componentIterator.next();

                        if ( LifecycleMapping.ROLE.equals( descriptor.getRole() ) )
                        {
                            packagingHandlers.add( descriptor.getRoleHint() );
                        }
                    }
                }
            }
        }

        return packagingHandlers;
    }

    private String getGoalPrefix()
    {
        if ( goalPrefix == null )
        {
            goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        }

        return goalPrefix;
    }

    public static class DummyComponentDiscovererManager implements ComponentDiscovererManager
    {

        DummyComponentDiscovererManager()
        {
        }
        
        public List getComponentDiscoverers()
        {
            return null;
        }

        public void registerComponentDiscoveryListener( ComponentDiscoveryListener listener )
        {
        }

        public void removeComponentDiscoveryListener( ComponentDiscoveryListener listener )
        {
        }

        public void fireComponentDiscoveryEvent( ComponentDiscoveryEvent event )
        {
        }

        public void initialize()
        {
        }

        public List getListenerDescriptors()
        {
            return null;
        }
        
    }
}
