package org.apache.maven.tools.plugin.extractor;

import org.apache.maven.project.MavenProject;
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
    implements MojoDescriptorExtractor
{
    public Set execute( MavenProject project )
        throws Exception
    {
        Map scriptFilesKeyedByBasedir = gatherScriptSourcesByBasedir( project.getScriptSourceRoots(),
                                                                      getScriptFileExtension() );

        Set mojoDescriptors = extractMojoDescriptors( scriptFilesKeyedByBasedir );

        return mojoDescriptors;
    }

    protected abstract Set extractMojoDescriptors( Map scriptFilesKeyedByBasedir )
        throws Exception;

    protected abstract String getScriptFileExtension();

    protected Map gatherScriptSourcesByBasedir( List directories, String scriptFileExtension )
    {
        Map sourcesByBasedir = new TreeMap();

        for ( Iterator it = directories.iterator(); it.hasNext(); )
        {
            Set sources = new HashSet();

            String resourceDir = (String) it.next();
            File dir = new File( resourceDir );

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
                    File scriptFile = new File( dir, relativePath );

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