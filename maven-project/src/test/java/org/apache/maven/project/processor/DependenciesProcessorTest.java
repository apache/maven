package org.apache.maven.project.processor;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import junit.framework.TestCase;

public class DependenciesProcessorTest extends TestCase
{
    public void testCopyChild()
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid" );
        
        Model child = new Model();
        child.addDependency( dependency );
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( null, child, target, false );
        
        assertEquals(1, target.getDependencies().size());
        assertEquals("aid", target.getDependencies().get( 0 ).getArtifactId());
    }
    
    public void testParentCopy()
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid" );
        
        Model child = new Model();
 
        Model parent = new Model();
        parent.addDependency( dependency ); 
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );
        
        assertEquals(1, target.getDependencies().size());
        assertEquals("aid", target.getDependencies().get( 0 ).getArtifactId());
    }    
}
