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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * With the build-consumer the POM will be adjusted during the process.
 * <ul>
 *   <li>CI-friendly versions will be resolved</li>
 *   <li>parents can omit the version if the relative path points to the correct parent</li>
 *   <li>dependencies can omit the version if it is part of the reactor</li>
 * </ul>
 *
 * During install the POM will be cleaned up
 * <ul>
 *   <li>the modules will be removed</li>
 *   <li>the relativePath will be removed</li>
 * </ul>
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-6656">MNG-6656</a>.
 *
 */
public class MavenITmng6957BuildConsumer
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6957BuildConsumer()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    /**
     * Verifies:
     * <ul>
     *   <li>preserve license</li>
     *   <li>consistent line separators</li>
     *   <li>resolved project versions (at least 2 levels deep) in parent and dependencies</li>
     *   <li>removal of modules in aggregators</li>
     * </ul>
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testPublishedPoms()
                    throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6957-buildconsumer" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setAutoclean( false );
        verifier.addCliArgument( "-Dchangelist=MNG6957" );

        verifier.addCliArgument( "install" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertTextEquals( new File( testDir, "expected/parent.pom"),
                new File( verifier.getArtifactPath( "org.sonatype.mavenbook.multi", "parent", "0.9-MNG6957-SNAPSHOT", "pom" ) ) );

        assertTextEquals( new File( testDir, "expected/simple-parent.pom"),
                new File( verifier.getArtifactPath( "org.sonatype.mavenbook.multi", "simple-parent", "0.9-MNG6957-SNAPSHOT", "pom" ) ) );

        assertTextEquals( new File( testDir, "expected/simple-weather.pom"),
                new File( verifier.getArtifactPath( "org.sonatype.mavenbook.multi", "simple-weather", "0.9-MNG6957-SNAPSHOT", "pom" ) ) );

        assertTextEquals( new File( testDir, "expected/simple-webapp.pom"),
                new File( verifier.getArtifactPath( "org.sonatype.mavenbook.multi", "simple-webapp", "0.9-MNG6957-SNAPSHOT", "pom" ) ) );

        assertTextEquals( new File( testDir, "expected/simple-testutils.pom"),
                new File( verifier.getArtifactPath( "org.sonatype.mavenbook.multi", "simple-testutils", "0.9-MNG6957-SNAPSHOT", "pom" ) ) );

        assertTextEquals( new File( testDir, "expected/utils-parent.pom"),
                new File( verifier.getArtifactPath( "org.sonatype.mavenbook.multi", "utils-parent", "0.9-MNG6957-SNAPSHOT", "pom" ) ) );
    }

    static void assertTextEquals( File file1, File file2 )
            throws IOException
    {
        assertEquals( FileUtils.loadFile( file1 ), FileUtils.loadFile( file2 ) );
    }

}
