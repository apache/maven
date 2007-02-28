package org.apache.maven.embedder;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractEmbedderExecutionTestCase
    extends AbstractEmbedderTestCase
{
    protected MavenExecutionRequest request( File basedir,
                                             List goals )
    {
        return new DefaultMavenExecutionRequest()
            .setBaseDirectory( basedir )
            .setGoals( goals );
    }

    protected File runWithProject( String goal )
        throws Exception
    {
        return runWithProject( new String[]{goal} );
    }

    protected File runWithProject( String[] goals )
        throws Exception
    {
        return runWithProject( Arrays.asList( goals ) );
    }

    protected File runWithProject( List goals )
        throws Exception
    {
        /*
        if ( request.getBaseDirectory() == null || !new File( request.getBaseDirectory() ).exists() )
        {
            throw new IllegalStateException( "You must specify a valid base directory in your execution request for this test." );
        }
        */

        File testDirectory = new File( getBasedir(), "src/test/embedder-test-project" );

        File targetDirectory = new File( getBasedir(), "target/" + getId() );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = request( targetDirectory, goals );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

        return targetDirectory;
    }

    protected abstract String getId();

    protected void assertNoExceptions( MavenExecutionResult result )
    {
        if ( !result.hasExceptions() )
        {
            return;
        }

        for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
        {
            Exception exception = (Exception) i.next();

            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }

    protected void assertFileExists( File file )
    {
        if ( !file.exists() )
        {
            fail( "The specified file '" + file + "' does not exist." );
        }
    }
}
