package org.apache.maven.project.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.harness.PomTestWrapper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
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
    
    public void testProfileModules() throws IOException
    {
        Model model = new Model();
        Profile profile = new Profile();
        profile.setModules( Arrays.asList( "m1", "m2" ) );
        
        PomClassicDomainModel m = ProcessorContext.mergeProfilesIntoModel( Arrays.asList( profile ), model, false );
        
        assertEquals(2, m.getModel().getModules().size());
    }
    
    public void testPackagingInheritance() throws IOException
    {
    	Model parent = new Model();
    	parent.setPackaging( "jar" );
    	Model child = new Model();
    	child.setPackaging( "pom" );
    	
        DomainModel pdmParent = new PomClassicDomainModel(parent);
        
        DomainModel pdmChild = new PomClassicDomainModel(child, true);    	
        ProcessorContext.build( Arrays.asList( pdmChild, pdmParent ), new ArrayList<InterpolatorProperty>() );    
        
        assertEquals("pom", child.getPackaging());
    }
    /*
    public void testProfilePluginManagement() throws IOException
    {
        Model model = new Model();
        
        Profile profile = new Profile();
        PluginManagement pm = new PluginManagement();
        Plugin p = new Plugin();
        p.setArtifactId( "aid" );
        pm.addPlugin( p );
        BuildBase b = new BuildBase();
        b.setPluginManagement( pm );
        profile.setBuild( b);
        
        
        PomClassicDomainModel m = ProcessorContext.mergeProfilesIntoModel( Arrays.asList( profile ), model, false );
        
        assertEquals(1, m.getModel().getBuild().getPluginManagement().getPlugins().size());
    }    
    */
    public void testInheritancePluginManagement() throws IOException
    {
        Model model = new Model();
        model.setBuild( createPluginManagement("aid") );
        DomainModel pdm = new PomClassicDomainModel(model);
        
        DomainModel child = new PomClassicDomainModel(new Model(), true);
     
        PomClassicDomainModel m =
            ProcessorContext.build( Arrays.asList( child, pdm ), new ArrayList<InterpolatorProperty>() );
        
        assertEquals(1, m.getModel().getBuild().getPluginManagement().getPlugins().size());
    }       
    
    
    private static Build createPluginManagement(String id)
    {
        
        PluginManagement pm = new PluginManagement();
        Plugin p = new Plugin();
        p.setArtifactId( id );
        pm.addPlugin( p );
        Build b = new Build();
        b.setPluginManagement( pm );
        return b;
    }
    /*
    public void testInheritancePluginManagement2() throws IOException
    {
        Model model = new Model();
        model.setBuild( createPluginManagement("aid") );
        
        DomainModel pdm = new PomClassicDomainModel(model);
        
        Model model2 = new Model();
        model2.setBuild( createPluginManagement("aid2") );   
        
        DomainModel pdm2 = new PomClassicDomainModel(model);        
        
        DomainModel child = new PomClassicDomainModel(new Model(), true);
     
        PomClassicDomainModel m = ProcessorContext.build( Arrays.asList(child, pdm, pdm2)) ;
        
        assertEquals(2, m.getModel().getBuild().getPluginManagement().getPlugins().size());
    }    
    */  
    
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

        ProcessorContext.build( domainModels, new ArrayList<InterpolatorProperty>() );
        
        return new PomTestWrapper( ProcessorContext.build( domainModels, new ArrayList<InterpolatorProperty>() ) );
    }
}
