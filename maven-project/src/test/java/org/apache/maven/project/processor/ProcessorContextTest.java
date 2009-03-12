package org.apache.maven.project.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.harness.PomTestWrapper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.model.DomainModel;
import org.codehaus.plexus.PlexusTestCase;

public class ProcessorContextTest extends PlexusTestCase
{
    
    private static String BASE_DIR = "src/test";

    private static String BASE_POM_DIR = BASE_DIR + "/resources-project-builder";
    
    private File testDirectory;
    
    protected void setUp()
        throws Exception
    {
        testDirectory = new File( getBasedir(), BASE_POM_DIR );
    }
    
    public void testPluginDependencyJoin() throws IOException
    {
       PomTestWrapper pom = buildPom( Arrays.asList( "merged-plugin-class-path-order/wo-plugin-mngt/sub/pom.xml", 
                                                     "merged-plugin-class-path-order/wo-plugin-mngt/pom.xml" ) );
     //  System.out.println(pom.getDomainModel().asString());
    }
    
    private PomTestWrapper buildPom( List<String> pomPaths )
        throws IOException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        
        for(String pomPath : pomPaths)
        {
            if(pomPaths.indexOf( pomPath ) == 0)
            {
                domainModels.add( new PomClassicDomainModel( new FileInputStream(new File( testDirectory, pomPath )), true) );
            }
            else
            {
                domainModels.add( new PomClassicDomainModel( new FileInputStream(new File( testDirectory, pomPath )), false) );    
            }
        }

        ProcessorContext.build( domainModels );
        
        return new PomTestWrapper( ProcessorContext.build( domainModels ) );
    }
}
