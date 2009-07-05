package org.apache.maven.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.FileUtils;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources folder along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources folder. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng3415JunkRepositoryMetadataTest
    extends AbstractMavenIntegrationTestCase
{
    private static final String RESOURCE_BASE = "/mng-3415";

    public MavenITmng3415JunkRepositoryMetadataTest()
    {
        // we're going to control the test execution according to the maven version present within each test method.
        // all methods should execute as long as we're using maven 2.0.9+, but the specific tests may vary a little
        // depending on which version we're using above 2.0.8.
        super( "(2.0.8,)" ); // only test in 2.0.9+
    }

    /**
     * This test simply verifies that when a metadata transfer fails (network error, etc.)
     * no metadata file is written to the local repository.
     *
     * Steps executed to verify this test:
     *
     * 0. Find the local repository directory:
     *    a. build the maven-find-local-repo-plugin, then run it, to spit out the path of the
     *       local repository in use by default. Read the output file to get this path.
     *       (Yes, it's heavy, but it's reliable.)
     * 1. Setup the test:
     *    a. Make sure the metadata for the test-repo is NOT in the local repository.
     *    b. Make sure the dependency POM IS in the local repository, so we're not
     *       distracted by failed builds that are unrelated.
     *    c. Create the settings file for use in this test, which contains the invalid
     *       remote repository entry.
     * 2. Build the test project the first time
     *    a. Verify that a TransferFailedException is in the build output for the test-repo
     *    b. Verify that the metadata for the dependency POM is NOT in the local
     *       repository afterwards.
     * 3. Build the test project the second time
     *    a. See (2.a) and (2.b) above; the same criteria applies here.
     */
    public void testitTransferFailed()
        throws Exception
    {
        String methodName = getMethodName();

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_BASE );
        File projectDir = new File( testDir, "project" );

        Verifier verifier;

        verifier = new Verifier( projectDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3415" );

        File localRepo = new File( verifier.localRepo );

        setupDummyDependency( testDir, localRepo, true );

        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.put( "@protocol@", "invalid" );
        filterProps.put( "@port@", "0" );
        File settings = verifier.filterFile( "../settings-template.xml", "settings-a.xml", "UTF-8", filterProps );

        List cliOptions = new ArrayList();
        cliOptions.add( "-X" );
        cliOptions.add( "-s" );
        cliOptions.add( settings.getName() );

        verifier.setCliOptions( cliOptions );
        verifier.setLogFileName( "log-" + methodName + "-firstBuild.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();

        assertMetadataMissing( localRepo );

        setupDummyDependency( testDir, localRepo, true );

        verifier.setLogFileName( "log-" + methodName + "-secondBuild.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertMetadataMissing( localRepo );
    }

    private String getMethodName()
    {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    /**
     * This test simply verifies that when metadata doesn't exist on the remote
     * repository, a basic metadata file is written to the local repository.
     *
     * Steps executed to verify this test:
     *
     * 0. Find the local repository directory:
     *    a. build the maven-find-local-repo-plugin, then run it, to spit out the path of the
     *       local repository in use by default. Read the output file to get this path.
     *       (Yes, it's heavy, but it's reliable.)
     * 1. Setup the test:
     *    a. Make sure the metadata for the test-repo is NOT in the local repository.
     *    b. Make sure the dependency POM IS in the local repository, so we're not
     *       distracted by failed builds that are unrelated.
     *    c. Create the settings file for use in this test, which contains the VALID
     *       remote repository entry.
     * 2. Build the test project the first time
     *    a. Verify that the remote repository is checked for the metadata file
     * 3. Build the test project the second time
     *    a. Verify that the remote repository is NOT checked for the metadata file again
     *    b. Verify that the file used for updateInterval calculations was NOT changed from
     *       the first build.
     */
    public void testShouldNotRepeatedlyUpdateOnResourceNotFoundException()
        throws Exception
    {
        String methodName = getMethodName();

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_BASE );
        File projectDir = new File( testDir, "project" );

        Verifier verifier;

        verifier = new Verifier( projectDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3415" );

        File localRepo = new File( verifier.localRepo );

        final List requestUris = new ArrayList();

        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                requestUris.add( request.getRequestURI() );

                response.setStatus( HttpServletResponse.SC_NOT_FOUND );

                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );
        server.start();

        try
        {
            int port = server.getConnectors()[0].getLocalPort();

            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.put( "@protocol@", "http" );
            filterProps.put( "@port@", Integer.toString( port ) );
            File settings = verifier.filterFile( "../settings-template.xml", "settings-b.xml", "UTF-8", filterProps );

            List cliOptions = new ArrayList();
            cliOptions.add( "-X" );
            cliOptions.add( "-s" );
            cliOptions.add( settings.getName() );

            verifier.setCliOptions( cliOptions );

            setupDummyDependency( testDir, localRepo, true );

            verifier.setLogFileName( "log-" + methodName + "-firstBuild.txt" );
            verifier.executeGoal( "validate" );

            verifier.verifyErrorFreeLog();

            assertTrue( requestUris.toString(), 
                requestUris.contains( "/org/apache/maven/its/mng3415/missing/1.0-SNAPSHOT/maven-metadata.xml" ) );

            requestUris.clear();

            File updateCheckFile = getUpdateCheckFile( localRepo );
            long firstLastMod = updateCheckFile.lastModified();

            setupDummyDependency( testDir, localRepo, false );

            verifier.setLogFileName( "log-" + methodName + "-secondBuild.txt" );
            verifier.executeGoal( "validate" );

            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            assertFalse( requestUris.toString(), 
                requestUris.contains( "/org/apache/maven/its/mng3415/missing/1.0-SNAPSHOT/maven-metadata.xml" ) );

            assertEquals( "Last-modified time should be unchanged from first build through second build for the file we use for updateInterval checks.", firstLastMod, updateCheckFile.lastModified() );
        }
        finally
        {
            server.stop();
        }
    }

    private void assertMetadataMissing( File localRepo )
    {
        File metadata = getMetadataFile( localRepo );

        assertFalse( "Metadata file should NOT be present in local repository: "
                     + metadata.getAbsolutePath(), metadata.exists() );
    }

    private void setupDummyDependency( File testDir,
                                       File localRepo,
                                       boolean resetUpdateInterval )
        throws VerificationException, IOException
    {
        File metadata = getMetadataFile( localRepo );

        if ( resetUpdateInterval && metadata.exists() )
        {
            System.out.println( "Deleting metadata file: " + metadata );
            metadata.delete();
        }

        File resolverStatus = new File( metadata.getParentFile(), "resolver-status.properties" );

        if ( resetUpdateInterval && resolverStatus.exists() )
        {
            System.out.println( "Deleting resolver-status.properties file related to: " + metadata );
            resolverStatus.delete();
        }

        File dir = metadata.getParentFile();

        System.out.println( "Setting up dependency POM in: " + dir );

        File pom = new File( dir, "missing-1.0-SNAPSHOT.pom" );

        if ( pom.exists() )
        {
            System.out.println( "Deleting pre-existing POM: " + pom );
            pom.delete();
        }

        File pomSrc = new File( testDir, "dependency-pom.xml" );

        System.out.println( "Copying dependency POM\nfrom: " + pomSrc + "\nto: " + pom );

        FileUtils.copyFile( pomSrc, pom );
    }

    private File getMetadataFile( File localRepo )
    {
        File dir = new File( localRepo, "org/apache/maven/its/mng3415/missing/1.0-SNAPSHOT" );

        dir.mkdirs();

        return new File( dir, "maven-metadata-testing-repo.xml" );
    }

    /**
     * If the current maven version is < 3.0, we'll use the metadata file itself (old maven-artifact code)...
     * otherwise, use the new resolver-status.properties file (new artifact code).
     */
    private File getUpdateCheckFile( File localRepo )
    {
        File dir = new File( localRepo, "org/apache/maven/its/mng3415/missing/1.0-SNAPSHOT" );

        dir.mkdirs();

        // < 3.0 (including snapshots)
        if ( matchesVersionRange( "(2.0.8,3.0-alpha-1)" ) )
        {
            return new File( dir, "maven-metadata-testing-repo.xml" );
        }
        else
        {
            return new File( dir, "resolver-status.properties" );
        }
    }

}
