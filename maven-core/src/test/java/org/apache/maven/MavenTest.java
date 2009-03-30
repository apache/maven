package org.apache.maven;

import java.io.File;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.component.annotations.Requirement;

public class MavenTest
    extends AbstractCoreMavenComponentTest
{
    @Requirement
    private Maven maven;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        maven = lookup( Maven.class );
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }

    // -----------------------------------------------------------------------------------------------
    // 
    // -----------------------------------------------------------------------------------------------

    public void testMaven()
        throws Exception
    {
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenExecutionRequest request = createMavenExecutionRequest( pom );
        MavenExecutionResult result = maven.execute( request );        
    }
}
