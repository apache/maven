package org.apache.maven.project.processor;

import java.util.ArrayList;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;

import junit.framework.TestCase;

public class BuildProcessorTest extends TestCase
{
    public void testChild_FinalName()
    {
        Model child = new Model();
        child.setBuild( new Build() );
        child.getBuild().setFinalName( "name" );
        Model target = new Model();
        
        BuildProcessor proc = new BuildProcessor(new ArrayList());
        proc.process( null, child, target, false );
        
        assertEquals("name", target.getBuild().getFinalName());
        
        child.getBuild().setFinalName( "name2" );
        assertEquals("name", target.getBuild().getFinalName());       
    }
    
    public void testParent_FinalName()
    {
        Model child = new Model();
        Model parent = new Model();
        parent.setBuild( new Build() );
        parent.getBuild().setFinalName( "name" );
        Model target = new Model();
        
        BuildProcessor proc = new BuildProcessor(new ArrayList());
        proc.process( parent, child, target, false );
        
        assertEquals("name", target.getBuild().getFinalName());
        
        //Immutable
        parent.getBuild().setFinalName( "name2" );
        assertEquals("name", target.getBuild().getFinalName());
        
    }    
    
    public void testParent_Filters()
    {
        Model child = new Model();
        
        Model parent = new Model();
        parent.setBuild( new Build() );

        parent.getBuild().getFilters().add( "filter1" );
        Model target = new Model();
        
        BuildProcessor proc = new BuildProcessor(new ArrayList());
        proc.process( parent, child, target, false );
        
        assertEquals(1, target.getBuild().getFilters().size());
        assertEquals("filter1", target.getBuild().getFilters().get( 0 ));
               
    }
    
    public void testChild_Filters()
    {
        Model child = new Model();
        child.setBuild( new Build() );
        child.getBuild().getFilters().add( "filter1" );
        Model target = new Model();
        
        BuildProcessor proc = new BuildProcessor(new ArrayList());
        proc.process( null, child, target, false );
        
        assertEquals(1, target.getBuild().getFilters().size());
        assertEquals("filter1", target.getBuild().getFilters().get( 0 ));
               
    }   
    public void testJoin_Filters()
    {
        Model child = new Model();
        child.setBuild( new Build() );
        child.getBuild().getFilters().add( "filter1" );
        Model target = new Model();
 
        Model parent = new Model();
        parent.setBuild( new Build() );

        parent.getBuild().getFilters().add( "filter2" );        
        
        BuildProcessor proc = new BuildProcessor(new ArrayList());
        proc.process( parent, child, target, false );
        
        assertEquals(2, target.getBuild().getFilters().size());
        
        //ORDER
        assertEquals("filter1", target.getBuild().getFilters().get( 0 ));
        assertEquals("filter2", target.getBuild().getFilters().get( 1 ));      
    }   
    
    public void testDoNotInheritParentIfChildExists_Resources()
    {
        Resource r = new Resource();
        r.setDirectory( "dir" );
        
        Resource r1 = new Resource();
        r1.setDirectory( "dir1" );
        
        Model child = new Model();
        child.setBuild( new Build() );
        child.getBuild().getResources().add( r );
        
        Model target = new Model();
 
        Model parent = new Model();
        parent.setBuild( new Build() );

        parent.getBuild().getResources().add( r1 );       
        
        BuildProcessor proc = new BuildProcessor(new ArrayList());
        proc.process( parent, child, target, false );
        
        assertEquals(1, target.getBuild().getResources().size());
        assertEquals("dir", target.getBuild().getResources().get( 0 ).getDirectory());
               
    }       
}
