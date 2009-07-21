package org.apache.maven;

import java.io.File;

import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.component.annotations.Requirement;

public class MavenTest
    extends AbstractCoreMavenComponentTestCase
{
    @Requirement
    private Maven maven;

    @Requirement
    private ExceptionHandler exceptionHandler;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        maven = lookup( Maven.class );
        exceptionHandler = lookup( ExceptionHandler.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        maven = null;
        exceptionHandler = null;
        super.tearDown();
    }

    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }

    public void testLifecycleExecutionUsingADefaultLifecyclePhase()
        throws Exception
    {
        /*
        File pom = getProject( "project-with-additional-lifecycle-elements" );
        MavenExecutionRequest request = createMavenExecutionRequest( pom );
        MavenExecutionResult result = maven.execute( request );
        if ( result.hasExceptions() )
        {
            ExceptionSummary es = exceptionHandler.handleException( result.getExceptions().get( 0 ) );
            System.out.println( es.getMessage() );
            es.getException().printStackTrace();
            fail( "Maven did not execute correctly." );
        }
        */
    }
}
