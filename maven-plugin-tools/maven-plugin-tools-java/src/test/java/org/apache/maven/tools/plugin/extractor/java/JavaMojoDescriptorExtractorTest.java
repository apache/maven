package org.apache.maven.tools.plugin.extractor.java;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class JavaMojoDescriptorExtractorTest
    extends TestCase
{
    
    public void testShouldFindTwoMojoDescriptorsInTestSourceDirectory() throws Exception
    {
        JavaMojoDescriptorExtractor extractor = new JavaMojoDescriptorExtractor();
        
        File sourceFile = fileOf("dir-flag.txt");
        System.out.println("found source file: " + sourceFile);
        
        File dir = sourceFile.getParentFile();
        
        Model model = new Model();
        model.setArtifactId("maven-unitTesting-plugin");
        
        MavenProject project = new MavenProject(model);
        
        Set results = extractor.execute(dir.getAbsolutePath(), project);
        assertEquals(2, results.size());
    }
    
    private File fileOf(String classpathResource)
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource(classpathResource);
        
        File result = null;
        if(resource != null)
        {
            result = new File(resource.getPath());
        }
        
        return result;
    }

}
