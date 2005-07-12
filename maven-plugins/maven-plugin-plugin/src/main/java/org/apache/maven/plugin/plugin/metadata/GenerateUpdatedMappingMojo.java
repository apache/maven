package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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
    private String outputDirectory;

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

    public void execute() throws MojoExecutionException
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
        
        boolean prefixAlreadyMapped = false;
        
        for ( Iterator it = pluginMap.getPlugins().iterator(); it.hasNext(); )
        {
            MappedPlugin preExisting = (MappedPlugin) it.next();
            
            if ( preExisting.getPrefix().equals( getGoalPrefix() ) )
            {
                prefixAlreadyMapped = true;
                
                if ( !preExisting.getArtifactId().equals( project.getArtifactId() ) )
                {
                    // TODO: In this case, should we rather just replace the existing plugin mapping??
                    
                    throw new MojoExecutionException( "Cannot map plugin to it's prefix in plugins.xml metadata; the prefix: \'" + getGoalPrefix() + "\' is already mapped to: " + preExisting.getArtifactId() ); 
                }
                else
                {
                    getLog().info( "NOT updating plugins.xml metadata; this plugin is already mapped." ); 
                }
                
                break;
            }
        }
        
        if ( !prefixAlreadyMapped )
        {
            MappedPlugin mappedPlugin = new MappedPlugin();
            
            mappedPlugin.setArtifactId( project.getArtifactId() );
            
            mappedPlugin.setPrefix( getGoalPrefix() );
            
            pluginMap.addPlugin( mappedPlugin );
            
            Writer writer = null;
            try
            {
                File updatedMetadataFile = new File( outputDirectory, metadata.getRepositoryPath() ).getAbsoluteFile();

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
    }
    
    private String getGoalPrefix()
    {
        if ( goalPrefix == null )
        {
            goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );
        }
        
        return goalPrefix;
    }

}
