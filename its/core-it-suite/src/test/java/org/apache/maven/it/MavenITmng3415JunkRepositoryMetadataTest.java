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

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3415">MNG-3415</a>.
 *
 * @version $Id$
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
     * <p/>
     * Steps executed to verify this test:
     * <p/>
     * 0. Find the local repository directory:
     * a. build the maven-find-local-repo-plugin, then run it, to spit out the path of the
     * local repository in use by default. Read the output file to get this path.
     * (Yes, it's heavy, but it's reliable.)
     * 1. Setup the test:
     * a. Make sure the metadata for the test-repo is NOT in the local repository.
     * b. Make sure the dependency POM IS in the local repository, so we're not
     * distracted by failed builds that are unrelated.
     * c. Create the settings file for use in this test, which contains the invalid
     * remote repository entry.
     * 2. Build the test project the first time
     * a. Verify that a TransferFailedException is in the build output for the test-repo
     * b. Verify that the metadata for the dependency POM is NOT in the local
     * repository afterwards.
     * 3. Build the test project the second time
     * a. See (2.a) and (2.b) above; the same criteria applies here.
     */
    public void testitTransferFailed()
        throws Exception
    {
        String methodName = getMethodName();

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_BASE );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3415" );

        setupDummyDependency( verifier, testDir, true );

        Properties filterProps = verifier.newDefaultFilterProperties();
        filterProps.put( "@protocol@", "invalid" );
        filterProps.put( "@port@", "0" );
        File settings = verifier.filterFile( "settings-template.xml", "settings-a.xml", "UTF-8", filterProps );

        verifier.addCliOption( "-X" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( settings.getName() );

        verifier.setLogFileName( "log-" + methodName + "-firstBuild.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();

        assertMetadataMissing( verifier );

        setupDummyDependency( verifier, testDir, true );

        verifier.setLogFileName( "log-" + methodName + "-secondBuild.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        assertMetadataMissing( verifier );
    }

    private String getMethodName()
    {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    /**
     * This test simply verifies that when metadata doesn't exist on the remote
     * repository, a basic metadata file is written to the local repository.
     * <p/>
     * Steps executed to verify this test:
     * <p/>
     * 0. Find the local repository directory:
     * a. build the maven-find-local-repo-plugin, then run it, to spit out the path of the
     * local repository in use by default. Read the output file to get this path.
     * (Yes, it's heavy, but it's reliable.)
     * 1. Setup the test:
     * a. Make sure the metadata for the test-repo is NOT in the local repository.
     * b. Make sure the dependency POM IS in the local repository, so we're not
     * distracted by failed builds that are unrelated.
     * c. Create the settings file for use in this test, which contains the VALID
     * remote repository entry.
     * 2. Build the test project the first time
     * a. Verify that the remote repository is checked for the metadata file
     * 3. Build the test project the second time
     * a. Verify that the remote repository is NOT checked for the metadata file again
     * b. Verify that the file used for updateInterval calculations was NOT changed from
     * the first build.
     */
    public void testShouldNotRepeatedlyUpdateOnResourceNotFoundException()
        throws Exception
    {
        String methodName = getMethodName();

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_BASE );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3415" );

        final Deque<String> requestUris = new ConcurrentLinkedDeque<>();

        Handler repoHandler = new AbstractHandler()
        {
            @Override
            public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            {
                System.out.println( "Handling " + request.getMethod() + " " + request.getRequestURL() );

                requestUris.add( request.getRequestURI() );

                response.setStatus( HttpServletResponse.SC_NOT_FOUND );

                ( (Request) request ).setHandled( true );
            }
        };

        Server server = new Server( 0 );
        server.setHandler( repoHandler );

        try
        {
            server.start();
            if ( server.isFailed() )
            {
                fail( "Couldn't bind the server socket to a free port!" );
            }

            int port = ( (NetworkConnector) server.getConnectors()[0] ).getLocalPort();
            System.out.println( "Bound server socket to the port " + port );

            Properties filterProps = verifier.newDefaultFilterProperties();
            filterProps.put( "@protocol@", "http" );
            filterProps.put( "@port@", Integer.toString( port ) );
            File settings = verifier.filterFile( "settings-template.xml", "settings-b.xml", "UTF-8", filterProps );

            verifier.addCliOption( "-X" );
            verifier.addCliOption( "-s" );
            verifier.addCliOption( settings.getName() );

            setupDummyDependency( verifier, testDir, true );

            verifier.setLogFileName( "log-" + methodName + "-firstBuild.txt" );
            verifier.executeGoal( "validate" );

            verifier.verifyErrorFreeLog();

            assertTrue( requestUris.toString(), requestUris.contains(
                "/org/apache/maven/its/mng3415/missing/1.0-SNAPSHOT/maven-metadata.xml" ) );

            requestUris.clear();

            File updateCheckFile = getUpdateCheckFile( verifier );
            long firstLastMod = updateCheckFile.lastModified();

            setupDummyDependency( verifier, testDir, false );

            verifier.setLogFileName( "log-" + methodName + "-secondBuild.txt" );
            verifier.executeGoal( "validate" );

            verifier.verifyErrorFreeLog();
            verifier.resetStreams();

            assertFalse( requestUris.toString(), requestUris.contains(
                "/org/apache/maven/its/mng3415/missing/1.0-SNAPSHOT/maven-metadata.xml" ) );

            assertEquals(
                "Last-modified time should be unchanged from first build through second build for the file we use for"
                    + " updateInterval checks.",
                firstLastMod, updateCheckFile.lastModified() );
        }
        finally
        {
            server.stop();
            server.join();
        }
    }

    private void assertMetadataMissing( Verifier verifier )
    {
        File metadata = getMetadataFile( verifier );

        assertFalse( "Metadata file should NOT be present in local repository: " + metadata.getAbsolutePath(),
                     metadata.exists() );
    }

    private void setupDummyDependency( Verifier verifier, File testDir, boolean resetUpdateInterval )
        throws IOException
    {
        String gid = "org.apache.maven.its.mng3415";
        String aid = "missing";
        String version = "1.0-SNAPSHOT";

        if ( resetUpdateInterval )
        {
            verifier.deleteArtifacts( gid );
        }

        File pom = new File( verifier.getArtifactPath( gid, aid, version, "pom" ) );

        File pomSrc = new File( testDir, "dependency-pom.xml" );

        System.out.println( "Copying dependency POM\nfrom: " + pomSrc + "\nto: " + pom );
        FileUtils.copyFile( pomSrc, pom );
    }

    private File getMetadataFile( Verifier verifier )
    {
        String gid = "org.apache.maven.its.mng3415";
        String aid = "missing";
        String version = "1.0-SNAPSHOT";
        String name = "maven-metadata-testing-repo.xml";

        return new File( verifier.getArtifactMetadataPath( gid, aid, version, name ) );
    }

    /**
     * If the current maven version is < 3.0, we'll use the metadata file itself (old maven-artifact code)...
     * otherwise, use the new resolver-status.properties file (new artifact code).
     */
    private File getUpdateCheckFile( Verifier verifier )
    {
        String gid = "org.apache.maven.its.mng3415";
        String aid = "missing";
        String version = "1.0-SNAPSHOT";
        String name;

        // < 3.0 (including snapshots)
        if ( matchesVersionRange( "(2.0.8,3.0-alpha-1)" ) )
        {
            name = "maven-metadata-testing-repo.xml";
        }
        else
        {
            name = "resolver-status.properties";
        }

        return new File( verifier.getArtifactMetadataPath( gid, aid, version, name ) );
    }
}
