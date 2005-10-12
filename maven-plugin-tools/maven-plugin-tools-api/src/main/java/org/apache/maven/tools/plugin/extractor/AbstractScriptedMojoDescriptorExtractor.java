package org.apache.maven.tools.plugin.extractor;

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
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
    public List execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        Map scriptFilesKeyedByBasedir =
            gatherScriptSourcesByBasedir( project.getScriptSourceRoots(), getScriptFileExtension() );

        List mojoDescriptors = extractMojoDescriptors( scriptFilesKeyedByBasedir, pluginDescriptor );

        copyScriptsToOutputDirectory( scriptFilesKeyedByBasedir, project.getBuild().getOutputDirectory() );

        return mojoDescriptors;
    }

    private void copyScriptsToOutputDirectory( Map scriptFilesKeyedByBasedir, String outputDirectory )
        throws ExtractionException
    {
        File outputDir = new File( outputDirectory );

        if ( !outputDir.exists() )
        {
            outputDir.mkdirs();
        }

        for ( Iterator it = scriptFilesKeyedByBasedir.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            File sourceDir = new File( (String) entry.getKey() );

            Set scripts = (Set) entry.getValue();

            for ( Iterator scriptIterator = scripts.iterator(); scriptIterator.hasNext(); )
            {
                File scriptFile = (File) scriptIterator.next();

                String relativePath = scriptFile.getPath().substring( sourceDir.getPath().length() );

                if ( relativePath.charAt( 0 ) == File.separatorChar )
                {
                    relativePath = relativePath.substring( 1 );
                }

                File outputFile = new File( outputDir, relativePath ).getAbsoluteFile();

                if ( !outputFile.getParentFile().exists() )
                {
                    outputFile.getParentFile().mkdirs();
                }

                try
                {
                    FileUtils.copyFile( scriptFile, outputFile );
                }
                catch ( IOException e )
                {
                    throw new ExtractionException(
                        "Cannot copy script file: " + scriptFile + " to output: " + outputFile, e );
                }
            }
        }
    }

    protected abstract List extractMojoDescriptors( Map scriptFilesKeyedByBasedir, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException;

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