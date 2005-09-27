package org.apache.maven.tools.plugin.extractor.ant;

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.tools.model.PluginMetadataParseException;
import org.apache.maven.plugin.tools.model.PluginMetadataParser;
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AntMojoDescriptorExtractor
    extends AbstractScriptedMojoDescriptorExtractor
{

    private static final String METADATA_FILE_EXTENSION = ".plugin.xml";
    private static final String SCRIPT_FILE_EXTENSION = ".xml";

    protected List extractMojoDescriptors( Map scriptFilesKeyedByBasedir, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        List descriptors = new ArrayList();

        PluginMetadataParser parser = new PluginMetadataParser();
        
        for ( Iterator mapIterator = scriptFilesKeyedByBasedir.entrySet().iterator(); mapIterator.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) mapIterator.next();

            String basedir = (String) entry.getKey();
            Set scriptFiles = (Set) entry.getValue();

            for ( Iterator it = scriptFiles.iterator(); it.hasNext(); )
            {
                File scriptMetadataFile = (File) it.next();
                
                String basename = scriptMetadataFile.getName();
                basename = basename.substring( 0, basename.length() - METADATA_FILE_EXTENSION.length() );
                
                File scriptFile = new File( scriptMetadataFile.getParentFile(), basename + SCRIPT_FILE_EXTENSION );
                
                if ( !scriptFile.exists() )
                {
                    throw new InvalidPluginDescriptorException( "Found orphaned plugin metadata file: " + scriptMetadataFile );
                }
                
                String relativePath = null;
                
                if ( basedir.endsWith( "/" ) )
                {
                    basedir = basedir.substring( 0, basedir.length() - 2 );
                }

                relativePath = scriptFile.getPath().substring( basedir.length() );

                relativePath = relativePath.replace( '\\', '/' );
                
                try
                {
                    Set mojoDescriptors = parser.parseMojoDescriptors( scriptMetadataFile );
                    
                    for ( Iterator discoveredMojoIterator = mojoDescriptors.iterator(); discoveredMojoIterator.hasNext(); )
                    {
                        MojoDescriptor descriptor = (MojoDescriptor) discoveredMojoIterator.next();
                        

                        String implementation = relativePath;
                        
                        if ( StringUtils.isNotEmpty( descriptor.getImplementation() ) )
                        {
                            implementation += ":" + descriptor.getImplementation();
                        }
                        
                        descriptor.setImplementation( implementation );

                        descriptor.setLanguage( "ant" );
                        descriptor.setComponentComposer( "map-oriented" );
                        descriptor.setComponentConfigurator( "map-oriented" );
                        
                        descriptors.add( descriptor );
                    }
                }
                catch ( PluginMetadataParseException e )
                {
                    throw new ExtractionException( "Error extracting mojo descriptor from script: " + scriptMetadataFile, e );
                }
            }
        }

        return descriptors;
    }

    protected String getScriptFileExtension()
    {
        return METADATA_FILE_EXTENSION;
    }

}
