package org.apache.maven.project.processor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;

import junit.framework.TestCase;

public class PrerequisitesProcessorTest
    extends TestCase
{

    public void testMaven()
    {
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setMaven( "2.1" );

        Model child = new Model();
        child.setPrerequisites( prerequisites );

        Model target = new Model();

        PrerequisitesProcessor proc = new PrerequisitesProcessor();
        proc.process( null, child, target, true );

        assertEquals( "2.1", target.getPrerequisites().getMaven() );

        // Immutable
        prerequisites.setMaven( "2.2" );
        assertEquals( "2.1", target.getPrerequisites().getMaven() );

    }

    public void testMavenParent()
    {
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setMaven( "2.1" );

        Model parent = new Model();
        parent.setPrerequisites( prerequisites );

        Model target = new Model();

        PrerequisitesProcessor proc = new PrerequisitesProcessor();
        proc.process( parent, new Model(), target, false );

        assertEquals( null, target.getPrerequisites() );

    }

}
