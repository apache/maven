package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import junit.framework.TestCase;

public class DependenciesProcessorTest
    extends TestCase
{
    public void testCopyChild()
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid" );

        List<Dependency> child = Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( null, child, target, false );

        assertEquals( 1, target.getDependencies().size() );
        assertEquals( "aid", target.getDependencies().get( 0 ).getArtifactId() );
    }

    public void testParentCopy()
    {
        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid" );

        List<Dependency> child = new ArrayList<Dependency>();

        List<Dependency> parent = Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );

        assertEquals( 1, target.getDependencies().size() );
        assertEquals( "aid", target.getDependencies().get( 0 ).getArtifactId() );
    }

    public void testDependencyOrder()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid1" );
        List<Dependency> child = Arrays.asList( dependency1 );

        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid" );
        List<Dependency> parent = Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );

        assertEquals( 2, target.getDependencies().size() );
        assertEquals( "aid1", target.getDependencies().get( 0 ).getArtifactId() );
        assertEquals( "aid", target.getDependencies().get( 1 ).getArtifactId() );
    }

    public void testJoin_NullVersion()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" );

        List<Dependency> child = Arrays.asList( dependency1 );

        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setSystemPath( "sp" );

        List<Dependency> parent= Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );

        assertEquals( 1, target.getDependencies().size() );
        assertEquals( "sp", target.getDependencies().get( 0 ).getSystemPath() );
    }

    public void testJoin_DefaultType()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" );
        dependency1.setVersion( "1.0" );
        dependency1.setType( "jar" );
        List<Dependency> child = Arrays.asList( dependency1 );

        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setVersion( "1.0" );
        dependency.setSystemPath( "sp" );

        List<Dependency> parent = Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );

        assertEquals( 1, target.getDependencies().size() );
        assertEquals( "sp", target.getDependencies().get( 0 ).getSystemPath() );
    }

    public void testJoin_DifferentClassifiers()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" );
        dependency1.setVersion( "1.0" );
        dependency1.setClassifier( "c1" );

        List<Dependency> child = Arrays.asList( dependency1 );

        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setVersion( "1.0" );
        dependency1.setClassifier( "c2" );

        List<Dependency> parent = Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );

        assertEquals( 2, target.getDependencies().size() );
    }

    public void testJoin_DifferentVersions()
    {
        Dependency dependency1 = new Dependency();
        dependency1.setArtifactId( "aid-c" );
        dependency1.setGroupId( "gid-c" );
        dependency1.setVersion( "1.1" );

        List<Dependency> child = Arrays.asList( dependency1 );

        Dependency dependency = new Dependency();
        dependency.setArtifactId( "aid-c" );
        dependency.setGroupId( "gid-c" );
        dependency.setVersion( "1.0" );

        List<Dependency> parent = Arrays.asList( dependency );

        Model target = new Model();

        DependenciesProcessor processor = new DependenciesProcessor();
        processor.process( parent, child, target, false );

        assertEquals( 2, target.getDependencies().size() );
    }
}
