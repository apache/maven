package org.apache.maven.tools.plugin.extractor;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.tools.plugin.PluginToolsException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author jdcasey
 */
public abstract class AbstractScriptedMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor
{
    public Set execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws PluginToolsException
    {
        Map scriptFilesKeyedByBasedir = gatherScriptSourcesByBasedir( project.getScriptSourceRoots(),
                                                                      getScriptFileExtension() );

        Set mojoDescriptors = extractMojoDescriptors( scriptFilesKeyedByBasedir, pluginDescriptor );

        return mojoDescriptors;
    }

    protected abstract Set extractMojoDescriptors( Map scriptFilesKeyedByBasedir, PluginDescriptor pluginDescriptor )
        throws PluginToolsException;

    protected abstract String getScriptFileExtension();

    protected Map gatherScriptSourcesByBasedir( List directories, String scriptFileExtension )
    {
        Map sourcesByBasedir = new TreeMap();

        for ( Iterator it = directories.iterator(); it.hasNext(); )
        {
            Set sources = new HashSet();

            String resourceDir = (String) it.next();
            File dir = new File( resourceDir ).getAbsoluteFile();
            
            resourceDir = dir.getPath();

            if ( dir.exists() )
            {
                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir( dir );
                scanner.addDefaultExcludes();
                scanner.scan();

                String[] relativePaths = scanner.getIncludedFiles();

                for ( int i = 0; i < relativePaths.length; i++ )
                {
                    String relativePath = relativePaths[i];
                    File scriptFile = new File( dir, relativePath ).getAbsoluteFile();

                    if ( scriptFile.isFile() && relativePath.endsWith( scriptFileExtension ) )
                    {
                        sources.add( scriptFile );
                    }
                }

                sourcesByBasedir.put( resourceDir, sources );
            }
        }

        return sourcesByBasedir;
    }

}