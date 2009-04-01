package org.apache.maven.project.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import junit.framework.TestCase;

public class ProcessorContextTest extends TestCase
{
    
    public void testProfileModules() throws IOException
    {
        Model model = new Model();
        Profile profile = new Profile();
        profile.setModules( Arrays.asList( "m1", "m2" ) );
        
        PomClassicDomainModel m = ProcessorContext.mergeProfilesIntoModel( Arrays.asList( profile ), new PomClassicDomainModel(model) );
        
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
    
}
