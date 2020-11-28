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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * This is a test case for a new check introduced with <a href="https://issues.apache.org/jira/browse/MNG-4660">MNG-4660</a>.
 * That check verifies if a packaged artifact within the Reactor is up-to-date with the outputDirectory of the same project.
 *
 * @author Maarten Mulders
 * @author Martin Kanters
 */
public class MavenITmng4660OutdatedPackagedArtifact extends AbstractMavenIntegrationTestCase {
    public MavenITmng4660OutdatedPackagedArtifact()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    /**
     * Test that Maven logs a warning when a packaged artifact is found that is older than the outputDirectory of the
     * same artifact.
     */
    public void testShouldWarnWhenPackagedArtifactIsOutdated() throws Exception
    {
        final File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4660-outdated-packaged-artifact" );

        // 1. Package the whole project
        final Verifier verifier1 = newVerifier( testDir.getAbsolutePath() );
        verifier1.deleteDirectory( "target" );
        verifier1.deleteArtifacts( "org.apache.maven.its.mng4660" );

        verifier1.executeGoal( "package" );

        Path module1Jar = testDir.toPath().resolve( "module-a/target/module-a-1.0.jar" ).toAbsolutePath();
        verifier1.verifyErrorFreeLog();
        verifier1.assertFilePresent( module1Jar.toString() );
        verifier1.resetStreams();

        if ( System.getProperty( "java.version", "" ).startsWith( "1." ) )
        {
            // Simulating the delay between two invocations. It also makes sure we're not hit by tests that run so fast,
            // that the difference in file modification time (see below) is too small to observe. Java 8 on Linux and
            // macOS returns that value with "just" second precision, which is not detailed enough.
            Thread.sleep( 1_000 );
        }

        // 2. Create a properties file with some content and compile only that module (module A).
        final Verifier verifier2 = newVerifier( testDir.getAbsolutePath() );
        final Path resourcesDirectory = Files.createDirectories( Paths.get( testDir.toString(), "module-a", "src", "main", "resources" ) );
        final Path fileToWrite = resourcesDirectory.resolve( "example.properties" );
        FileUtils.fileWrite( fileToWrite.toString(), "x=42" );

        verifier2.setAutoclean( false );
        verifier2.addCliOption( "--projects" );
        verifier2.addCliOption( ":module-a" );
        verifier2.executeGoal( "compile" );

        Path module1PropertiesFile = testDir.toPath().resolve( "module-a/target/classes/example.properties" )
                .toAbsolutePath();

        verifier2.assertFilePresent( module1PropertiesFile.toString() );
        assertThat( Files.getLastModifiedTime( module1PropertiesFile ),
                greaterThan ( Files.getLastModifiedTime( module1Jar ) ) );

        Path module1Class = testDir.toPath().resolve( "module-a/target/classes/org/apache/maven/it/Example.class" )
                        .toAbsolutePath();
        verifier2.verifyErrorFreeLog();
        verifier2.assertFilePresent( module1Class.toString() );
        verifier2.resetStreams();

        // 3. Resume project build from module B, that depends on module A we just touched. Its packaged artifact
        // is no longer in sync with its compiled artifacts.
        final Verifier verifier3 = newVerifier( testDir.getAbsolutePath() );
        verifier3.setAutoclean( false );
        verifier3.addCliOption( "--resume-from" );
        verifier3.addCliOption( ":module-b" );
        verifier3.executeGoal( "compile" );

        verifier3.verifyErrorFreeLog();
        try
        {
            verifier3.verifyTextInLog( "Packaged artifact is not up-to-date" );
        }
        catch ( VerificationException e )
        {
            String message = e.getMessage() + System.lineSeparator();
            message += "  " + module1Jar.getFileName() + " -> " + Files.getLastModifiedTime( module1Jar )
                            + System.lineSeparator();
            message += "  " + module1PropertiesFile.getFileName() + " -> " + Files.getLastModifiedTime( module1PropertiesFile )
                            + System.lineSeparator();
            throw new VerificationException( message, e.getCause() );
        }
        verifier3.resetStreams();
    }
}
