package org.apache.maven.plugin.verifier;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

public class VerifierMojoTest
    extends TestCase
{
    public void testPrefixWithBaseDir()
    {
        VerifierMojo mojo = new VerifierMojo();
        mojo.setBaseDir( "c:/some/path" );

        File result = mojo.getAbsoluteFileToCheck( new File( "target/dummy.txt" ) );

        File expectedResult = new File( "c:/some/path/target/dummy.txt" );
        assertEquals( expectedResult.getPath(), result.getPath() );
    }

    public void testDoNotPrefixWhenAbsolutePath()
    {
        VerifierMojo mojo = new VerifierMojo();
        mojo.setBaseDir( new File( "/some/path" ).getAbsolutePath() );

        File absoluteFile = new File( "/project/target/dummy.txt" ).getAbsoluteFile();
        File result = mojo.getAbsoluteFileToCheck( absoluteFile );

        File expectedResult = absoluteFile;
        assertEquals( expectedResult.getPath(), result.getPath() );
    }

    public void testCheckFileThatDoesNotExist()
        throws Exception
    {
        VerifierMojo mojo = new VerifierMojo();
        File file = new File( getClass().getResource( "/FileDoesNotExist.xml" ).getFile() );
        mojo.setBaseDir( "c:/some/path" );
        mojo.setVerificationFile( file );
        mojo.setFailOnError( true );
        mojo.setVerificationResultPrinter( new VerificationResultPrinter()
        {
            public void print( VerificationResult result )
            {
                assertEquals( 1, result.getExistenceFailures().size() );
                assertEquals( 0, result.getNonExistenceFailures().size() );
                assertEquals( 0, result.getContentFailures().size() );
            }
        } );

        try
        {
            mojo.execute();
            fail( "Should have thrown an exception" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
    }

    public void testCheckFileThatExists()
        throws Exception
    {
        VerifierMojo mojo = new VerifierMojo();
        File file = new File( getClass().getResource( "/FileExists.xml" ).getFile() );
        mojo.setBaseDir( file.getParent() );
        mojo.setVerificationFile( file );
        mojo.setFailOnError( true );
        mojo.setVerificationResultPrinter( new VerificationResultPrinter()
        {
            public void print( VerificationResult result )
            {
                assertEquals( 0, result.getExistenceFailures().size() );
                assertEquals( 0, result.getNonExistenceFailures().size() );
                assertEquals( 0, result.getContentFailures().size() );
            }
        } );

        mojo.execute();
    }

    public void testCheckForInexistentFile()
        throws Exception
    {
        VerifierMojo mojo = new VerifierMojo();
        File file = new File( getClass().getResource( "/InexistentFile.xml" ).getFile() );
        mojo.setBaseDir( "c:/some/path" );
        mojo.setVerificationFile( file );
        mojo.setVerificationResultPrinter( new VerificationResultPrinter()
        {
            public void print( VerificationResult result )
            {
                assertEquals( 0, result.getExistenceFailures().size() );
                assertEquals( 0, result.getNonExistenceFailures().size() );
                assertEquals( 0, result.getContentFailures().size() );
            }
        } );

        mojo.execute();
    }

    public void testCheckForInexistentFileThatExists()
        throws Exception
    {
        VerifierMojo mojo = new VerifierMojo();
        File file = new File( getClass().getResource( "/InexistentFileThatExists.xml" ).getFile() );
        mojo.setBaseDir( file.getParent() );
        mojo.setVerificationFile( file );
        mojo.setFailOnError( true );
        mojo.setVerificationResultPrinter( new VerificationResultPrinter()
        {
            public void print( VerificationResult result )
            {
                assertEquals( 0, result.getExistenceFailures().size() );
                assertEquals( 1, result.getNonExistenceFailures().size() );
                assertEquals( 0, result.getContentFailures().size() );
            }
        } );

        try
        {
            mojo.execute();
            fail( "Should have thrown an exception" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
    }

    public void testCheckFileForContent()
        throws Exception
    {
        VerifierMojo mojo = new VerifierMojo();
        File file = new File( getClass().getResource( "/FileExistsValidContent.xml" ).getFile() );
        mojo.setBaseDir( file.getParent() );
        mojo.setVerificationFile( file );
        mojo.setVerificationResultPrinter( new VerificationResultPrinter()
        {
            public void print( VerificationResult result )
            {
                assertEquals( 0, result.getExistenceFailures().size() );
                assertEquals( 0, result.getNonExistenceFailures().size() );
                assertEquals( 0, result.getContentFailures().size() );
            }
        } );

        mojo.execute();
    }

    public void testCheckFileForInvalidContent()
        throws Exception
    {
        VerifierMojo mojo = new VerifierMojo();
        File file = new File( getClass().getResource( "/FileExistsInvalidContent.xml" ).getFile() );
        mojo.setBaseDir( file.getParent() );
        mojo.setVerificationFile( file );
        mojo.setFailOnError( true );
        mojo.setVerificationResultPrinter( new VerificationResultPrinter()
        {
            public void print( VerificationResult result )
            {
                assertEquals( 0, result.getExistenceFailures().size() );
                assertEquals( 0, result.getNonExistenceFailures().size() );
                assertEquals( 1, result.getContentFailures().size() );
            }
        } );

        try
        {
            mojo.execute();
            fail( "Should have thrown an exception" );
        }
        catch ( MojoExecutionException expected )
        {
            assertTrue( true );
        }
    }

}
