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
    
    public void testDependencyOrder()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid1" );    
        Model child = new Model();
        child.addDependency( dependency1 );
        
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid" );    
        Model parent = new Model();
        parent.addDependency( dependency ); 
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );
        
        assertEquals(2, target.getDependencies().size());
        assertEquals("aid1", target.getDependencies().get( 0 ).getArtifactId());
        assertEquals("aid", target.getDependencies().get( 1 ).getArtifactId());
    }  
    
    public void testJoin_NullVersion()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" ); 
 
        
        Model child = new Model();
        child.addDependency( dependency1 );
        
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" ); 
        dependency.setSystemPath("sp");
        
        Model parent = new Model();
        parent.addDependency( dependency ); 
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );
        
        assertEquals(1, target.getDependencies().size());
        assertEquals("sp", target.getDependencies().get( 0 ).getSystemPath());
    }   
    
    public void testJoin_DefaultType()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" ); 
        dependency1.setVersion( "1.0" ); 
        dependency1.setType( "jar" );
        Model child = new Model();
        child.addDependency( dependency1 );
        
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setVersion( "1.0" ); 
        dependency.setSystemPath("sp");
        
        Model parent = new Model();
        parent.addDependency( dependency ); 
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );
        
        assertEquals(1, target.getDependencies().size());
        assertEquals("sp", target.getDependencies().get( 0 ).getSystemPath());
    } 
    
    public void testJoin_DifferentClassifiers()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" ); 
        dependency1.setVersion( "1.0" ); 
        dependency1.setClassifier( "c1" );
        
        Model child = new Model();
        child.addDependency( dependency1 );
        
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setVersion( "1.0" ); 
        dependency1.setClassifier( "c2" );
        
        Model parent = new Model();
        parent.addDependency( dependency ); 
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );
        
        assertEquals(2, target.getDependencies().size());
    } 
    
    public void testJoin_DifferentVersions()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" ); 
        dependency1.setVersion( "1.1" ); 
        
        Model child = new Model();
        child.addDependency( dependency1 );
        
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setVersion( "1.0" ); 
        
        Model parent = new Model();
        parent.addDependency( dependency ); 
        
        Model target = new Model();
        
        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );
        
        assertEquals(2, target.getDependencies().size());
    }       
}
