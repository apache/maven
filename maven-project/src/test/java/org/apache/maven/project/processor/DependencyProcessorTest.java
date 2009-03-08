package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;

import junit.framework.TestCase;

public class DependencyProcessorTest extends TestCase
{
    public void testCopyChild()
    {
        DependencyProcessor processor = new DependencyProcessor();
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Dependency child = new Dependency();
        child.setArtifactId( "aid" );
        
        processor.process( null, child, dependencies, false );
        assertEquals(1, dependencies.size());
        
        //Immutable
        child.setArtifactId( "aid2" );
        assertEquals("aid", dependencies.get( 0 ).getArtifactId());
    }
    
    public void testCopyParent()
    {
        DependencyProcessor processor = new DependencyProcessor();
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Dependency parent = new Dependency();
        parent.setArtifactId( "aid" );
        
        processor.process( parent, null, dependencies, false );
        assertEquals(1, dependencies.size());
        
        //Immutable
        parent.setArtifactId( "aid2" );
        assertEquals("aid", dependencies.get( 0 ).getArtifactId());
    }   
    
    public void testJoinChildOverridesParent()
    {
        DependencyProcessor processor = new DependencyProcessor();
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Dependency child = new Dependency();
        child.setArtifactId( "aid" );
        
        Dependency parent = new Dependency();
        parent.setArtifactId( "aid2" );
        
        processor.process( parent, child, dependencies, false );
        assertEquals(1, dependencies.size());
        
        assertEquals("aid", dependencies.get( 0 ).getArtifactId());
        
        //Immutable
        child.setArtifactId( "aid3" );
        assertEquals("aid", dependencies.get( 0 ).getArtifactId());
    } 
    
    public void testJoinElements()
    {
        DependencyProcessor processor = new DependencyProcessor();
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Dependency child = new Dependency();
        child.setArtifactId( "aid" );
        
        Dependency parent = new Dependency();
        parent.setGroupId( "gid" );
        
        processor.process( parent, child, dependencies, false );
        assertEquals(1, dependencies.size());
        
        assertEquals("aid", dependencies.get( 0 ).getArtifactId());
        assertEquals("gid", dependencies.get( 0 ).getGroupId());
        
    }     
}
