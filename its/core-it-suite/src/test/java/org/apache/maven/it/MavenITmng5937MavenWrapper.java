package org.apache.maven.it;

import java.io.BufferedWriter;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5937">MNG-5937</a>.
 *
 */
public class MavenITmng5937MavenWrapper
    extends AbstractMavenIntegrationTestCase
{
    private Path wrapperDistro;

    private final Map<String,String> envVars;

    private final Path baseDir = Paths.get( "target/test-classes/mng-5937 wrapper" );

    private ZipUnArchiver zipUnArchiver = new ZipUnArchiver();

    public MavenITmng5937MavenWrapper()
        throws Exception
    {
        super( "[4.0.0-alpha-1,)" );

        String localRepo = System.getProperty("maven.repo.local");

        envVars = new HashMap<>( 4 );
        envVars.put( "MVNW_REPOURL", Paths.get( localRepo ).toUri().toURL().toString() );
        envVars.put( "MVNW_VERBOSE", "true" );
        String javaHome = System.getenv( "JAVA_HOME" );
        if ( javaHome != null )
        {
            // source needs to call the javac executable.
            // if JAVA_HOME is not set, ForkedLauncher sets it to java.home, which is the JRE home
            envVars.put( "JAVA_HOME", javaHome );
        }
    }

    public void setUp()
        throws Exception
    {
        String mavenDist = System.getProperty( "maven.distro" );
        if ( StringUtils.isEmpty( mavenDist ) )
        {
            throw new IllegalStateException( "Missing maven.distro=${mavenDistro} parameter to test maven-wrapper: see run ITs instructions" );
        }

        Verifier distInstaller = newVerifier( baseDir.toAbsolutePath().toFile().toString() );
        distInstaller.setSystemProperty( "file", mavenDist );
        distInstaller.setSystemProperty( "groupId", "org.apache.maven" );
        distInstaller.setSystemProperty( "artifactId", "apache-maven" );
        distInstaller.setSystemProperty( "version", getMavenVersion().toString() );
        distInstaller.setSystemProperty( "classifier", "bin" );
        distInstaller.setSystemProperty( "packaging", "zip" );
        distInstaller.executeGoal( "org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file" );

        String distroValue = System.getProperty( "maven.wrapper.distrodir" );
        if ( StringUtils.isEmpty( distroValue ) )
        {
            throw new IllegalStateException( "Missing maven.wrapper.distrodir=${wrapperDistroDir} parameter to test maven-wrapper: see run ITs instructions" );
        }
        wrapperDistro = Paths.get( distroValue );
    }

    public void testitMNG5937Bin()
        throws Exception
    {
        final File testDir = baseDir.resolve( "bin" ).toFile();

        unpack( testDir.toPath(), "bin" );

        envVars.put( "MAVEN_BASEDIR", testDir.getAbsolutePath() );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setDebug( true );
        verifier.executeGoal( "validate", envVars );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG5937Script()
                    throws Exception
    {
        final File testDir = baseDir.resolve( "script" ).toFile();

        unpack( testDir.toPath(), "script" );

        envVars.put( "MAVEN_BASEDIR", testDir.getAbsolutePath() );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setDebug( true );
        verifier.executeGoal( "validate", envVars );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG5937Source()
                    throws Exception
    {
        final File testDir = baseDir.resolve( "source" ).toFile();

        unpack( testDir.toPath(), "source" );

        envVars.put( "MAVEN_BASEDIR", testDir.getAbsolutePath() );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setDebug( true );
        verifier.executeGoal( "validate", envVars );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testitMNG5937WrapperProperties()
                    throws Exception
    {
        final File testDir = baseDir.resolve( "properties" ).toFile();

        unpack( testDir.toPath(), "bin" );

        Path p = baseDir.resolve( "properties/.mvn/wrapper/maven-wrapper.properties" );
        try ( BufferedWriter out = Files.newBufferedWriter( p, StandardOpenOption.TRUNCATE_EXISTING ) )
        {
            String localRepo = System.getProperty("maven.repo.local");
            out.append( "distributionUrl = " + Paths.get( localRepo ).toUri().toURL().toString() )
               .append( "org/apache/maven/apache-maven/")
               .append( getMavenVersion().toString() )
               .append( "/apache-maven-")
               .append( getMavenVersion().toString() )
               .append( "-bin.zip" );
        }

        envVars.remove( "MVNW_REPOURL" );
        envVars.put( "MAVEN_BASEDIR", testDir.getAbsolutePath() );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setDebug( true );
        verifier.executeGoal( "validate", envVars );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    private void unpack( Path target, String classifier ) throws IOException
    {
        Path distro = wrapperDistro.resolve( "apache-maven-wrapper-" + getMavenVersion() + '-' + classifier + ".zip" );

        zipUnArchiver.setSourceFile( distro.toFile() );
        zipUnArchiver.setDestDirectory( target.toFile() );
        zipUnArchiver.enableLogging( new ConsoleLogger() );

        zipUnArchiver.extract();
    }
}
