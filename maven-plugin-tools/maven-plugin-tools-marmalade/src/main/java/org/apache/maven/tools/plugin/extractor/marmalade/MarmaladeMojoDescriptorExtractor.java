package org.apache.maven.tools.plugin.extractor.marmalade;

import org.apache.maven.plugin.MavenMojoDescriptor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.script.marmalade.MarmaladeMojoExecutionDirectives;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.marmalade.parsing.ScriptParser;
import org.codehaus.marmalade.util.LazyMansAccess;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author jdcasey
 */
public class MarmaladeMojoDescriptorExtractor
    implements MojoDescriptorExtractor
{

    private ScriptParser scriptParser = new ScriptParser();

    public Set execute( String sourceDir, MavenProject project ) throws Exception
    {
        String[] files = PluginUtils.findSources( sourceDir, "**/*.mmld" );

        Set descriptors = new HashSet();

        File dir = new File( sourceDir );
        for ( int i = 0; i < files.length; i++ )
        {
            String file = files[i];

            Map context = new TreeMap();
            context.put( MarmaladeMojoExecutionDirectives.SCRIPT_BASEPATH_INVAR, sourceDir );

            File scriptFile = new File( dir, file );

            context = LazyMansAccess.executeFromFile( scriptFile, context );

            MojoDescriptor descriptor = (MojoDescriptor) context.get( MarmaladeMojoExecutionDirectives.METADATA_OUTVAR );
            
            MavenMojoDescriptor mmDescriptor = new MavenMojoDescriptor( descriptor );
            
            mmDescriptor.setComponentFactory( "marmalade" );
            
            mmDescriptor.setImplementation( file );

            descriptors.add( mmDescriptor );
        }

        return descriptors;
    }

}