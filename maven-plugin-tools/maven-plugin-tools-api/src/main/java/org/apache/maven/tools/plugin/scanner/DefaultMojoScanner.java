package org.apache.maven.tools.plugin.scanner;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author jdcasey
 */
public class DefaultMojoScanner
    implements MojoScanner
{

    private Map mojoDescriptorExtractors;

    public DefaultMojoScanner( Map extractors )
    {
        this.mojoDescriptorExtractors = extractors;
    }

    public DefaultMojoScanner()
    {
    }

    public Set execute( MavenProject project ) throws Exception
    {
        Set descriptors = new HashSet();

        for ( Iterator it = mojoDescriptorExtractors.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            String language = (String) entry.getKey();
            MojoDescriptorExtractor extractor = (MojoDescriptorExtractor) entry.getValue();

            String sourceDir = null;

            File basedir = project.getBasedir();
            
            Build buildSection = project.getBuild();
            if ( buildSection != null )
            {
                sourceDir = buildSection.getSourceDirectory();
            }

            if ( sourceDir == null )
            {
                sourceDir = "src/main/java";
            }
            
            File src = new File(basedir, sourceDir);

            sourceDir = src.getAbsolutePath();
            
            Set extractorDescriptors = extractor.execute( sourceDir, project );
            
            System.out.println("Extractor for language: " + language + " found " + extractorDescriptors.size() + " mojo descriptors.");

            descriptors.addAll( extractorDescriptors );
        }

        return descriptors;
    }

}