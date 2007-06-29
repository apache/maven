package org.apache.maven.integrationtests;

import org.apache.maven.it.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;

/**
 * @author Jason van Zyl
 * @author Kenney Westerhof
 */
public abstract class AbstractMavenIntegrationTestCase
    extends TestCase
{
    protected void runTest()
        throws Throwable
    {
        if ( "true".equals( System.getProperty( "useEmptyLocalRepository", "false" ) ) )
        {
            setupLocalRepo();
        }

        // save System.out since running the test will replace it
        PrintStream out = System.out;

        out.print( getITName() + "(" + getName() + ").." );

        try
        {
            super.runTest();
            out.println( " Ok" );
        }
        catch ( Throwable t )
        {
            out.println( " Failure" );
            throw t;
        }
    }

    private String getITName()
    {
        String simpleName = getClass().getName();
        int idx = simpleName.lastIndexOf( '.' );
        simpleName = idx >= 0 ? simpleName.substring( idx + 1 ) : simpleName;
        simpleName = simpleName.startsWith( "MavenIT" ) ? simpleName.substring( "MavenIT".length() ) : simpleName;
        simpleName = simpleName.endsWith( "Test" ) ? simpleName.substring( 0, simpleName.length() - 4 ) : simpleName;
        return simpleName;
    }

    protected File setupLocalRepo()
        throws IOException
    {
        String tempDirPath = System.getProperty( "maven.test.tmpdir", System.getProperty( "java.io.tmpdir" ) );
        File localRepo = new File( tempDirPath, "local-repository/" + getITName() );
        if ( localRepo.isDirectory() )
        {
            FileUtils.deleteDirectory( localRepo );
        }

        System.setProperty( "maven.repo.local", localRepo.getAbsolutePath() );

        return localRepo;
    }
}
