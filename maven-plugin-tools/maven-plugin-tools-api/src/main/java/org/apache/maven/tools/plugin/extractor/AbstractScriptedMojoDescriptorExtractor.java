package org.apache.maven.tools.plugin.extractor;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
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

    public Set execute( MavenProject project ) throws Exception
    {
        Build buildSection = project.getBuild();

        List resources = null;
        if ( buildSection != null )
        {
            resources = buildSection.getResources();
        }

        Map scriptFilesKeyedByBasedir = gatherScriptSourcesByBasedir( resources, getScriptFileExtension() );

        Set mojoDescriptors = extractMojoDescriptors( scriptFilesKeyedByBasedir );

        return mojoDescriptors;
    }

    protected abstract Set extractMojoDescriptors( Map scriptFilesKeyedByBasedir ) throws Exception;

    protected abstract String getScriptFileExtension();

    protected Map gatherScriptSourcesByBasedir( List resources, String scriptFileExtension )
    {
        Map sourcesByBasedir = new TreeMap();

        if ( resources != null )
        {
            for ( Iterator it = resources.iterator(); it.hasNext(); )
            {
                Set sources = new HashSet();

                Resource resource = (Resource) it.next();

                String resourceDir = resource.getDirectory();
                File dir = new File( resourceDir );

                if ( dir.exists() )
                {
                    DirectoryScanner scanner = new DirectoryScanner();

                    scanner.setBasedir( dir );

                    List includes = resource.getIncludes();

                    if ( includes != null && !includes.isEmpty() )
                    {
                        scanner.setIncludes( (String[]) includes.toArray( new String[includes.size()] ) );
                    }

                    List excludes = resource.getExcludes();

                    if ( excludes != null && !excludes.isEmpty() )
                    {
                        scanner.setExcludes( (String[]) excludes.toArray( new String[excludes.size()] ) );
                    }

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
        }

        return sourcesByBasedir;
    }

}