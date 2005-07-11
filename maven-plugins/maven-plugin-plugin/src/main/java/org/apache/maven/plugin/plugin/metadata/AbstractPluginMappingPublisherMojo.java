package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.mapping.MappedPlugin;
import org.apache.maven.plugin.mapping.PluginMap;
import org.apache.maven.plugin.mapping.io.xpp3.PluginMappingXpp3Reader;
import org.apache.maven.plugin.mapping.io.xpp3.PluginMappingXpp3Writer;
import org.apache.maven.plugin.mapping.metadata.PluginMappingMetadata;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

public abstract class AbstractPluginMappingPublisherMojo
    extends AbstractMetadataPublisherMojo
{

    protected RepositoryMetadata createMetadataInstance()
    {
        return new PluginMappingMetadata( getGroupId() );
    }

    protected void updateMetadata( RepositoryMetadata metadata ) throws MojoExecutionException
    {
        PluginMappingXpp3Reader mappingReader = new PluginMappingXpp3Reader();
        
        PluginMap pluginMap = null;
        
        Reader reader = null;
        
        File metadataFile = metadata.getFile();
        
        if ( metadataFile != null )
        {
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
        else
        {
            pluginMap = new PluginMap();
            
            pluginMap.setGroupId( getGroupId() );
        }
        
        MappedPlugin mappedPlugin = new MappedPlugin();
        
        mappedPlugin.setArtifactId( getArtifactId() );
        
        mappedPlugin.setPrefix( getGoalPrefix() );
        
        boolean prefixAlreadyMapped = false;
        
        for ( Iterator it = pluginMap.getPlugins().iterator(); it.hasNext(); )
        {
            MappedPlugin preExisting = (MappedPlugin) it.next();
            
            if ( preExisting.getPrefix().equals( getGoalPrefix() ) )
            {
                prefixAlreadyMapped = true;
                
                if ( !preExisting.getArtifactId().equals( getArtifactId() ) )
                {
                    // TODO: In this case, should we rather just replace the existing plugin mapping??
                    
                    throw new MojoExecutionException( "Cannot map plugin to it's prefix in plugins.xml metadata; the prefix: \'" + getGoalPrefix() + "\' is already mapped to: " + preExisting.getArtifactId() ); 
                }
                else
                {
                    getLog().info( "NOT updating plugins.xml metadata; this plugin is already mapped." ); 
                }
            }
        }
        
        if ( !prefixAlreadyMapped )
        {
            pluginMap.addPlugin( mappedPlugin );
            
            Writer writer = null;
            try
            {
                metadataFile = getMetadataFile( metadata.getRepositoryPath() );
                
                writer = new FileWriter( metadataFile );
                
                PluginMappingXpp3Writer mappingWriter = new PluginMappingXpp3Writer();
                
                mappingWriter.write( writer, pluginMap );
                
                metadata.setFile( metadataFile );
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

}
