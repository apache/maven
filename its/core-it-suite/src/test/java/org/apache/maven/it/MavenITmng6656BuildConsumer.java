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
import java.util.Arrays;

/**
 * With the build-consumer the pom.xml will be adjusted during the process.
 * <ul>
 *   <li>CLI-friendly versions will be resolved</li>
 *   <li>parents can omit the version if the relative path points to the correct parent</li>
 *   <li>dependencies can omit the version if it is part of the reactor</li>
 * </ul>
 * 
 * During install the pom will be cleaned up
 * <ul>
 *   <li>the modules will be removed</li>
 *   <li>the relativePath will be removed</li>
 * </ul>
 *
 * <a href="https://issues.apache.org/jira/browse/MNG-6656">MNG-6656</a>.
 *
 */
public class MavenITmng6656BuildConsumer
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng6656BuildConsumer()
    {
        super( "[3.7.0,)" );
    }

    /**
     * Verifies:
     * <ul>
     *   <li>preserve license</li>
     *   <li>consistent line separators</li>
     *   <li>resolved project versions (at least 2 levels deep) in parent and dependencies</li>
     *   <li>removal of modules in aggregators</li>
     * </ul>
     * @throws Exception
     */
    public void testPublishedPoms()
                    throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-6656-buildconsumer" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), false );
        verifier.setMavenDebug( false );
        verifier.setAutoclean( false );
        verifier.addCliOption( "-Dchangelist=MNG6656" );

        verifier.executeGoals( Arrays.asList( "install" ) );
        verifier.verifyErrorFreeLog();

        String content;
        content = FileUtils.fileRead( new File( testDir, "expected/parent.pom") ); 
        verifier.assertArtifactContents( "org.sonatype.mavenbook.multi", "parent", "0.9-MNG6656-SNAPSHOT", "pom", content );

        content = FileUtils.fileRead( new File( testDir, "expected/simple-parent.pom") ); 
        verifier.assertArtifactContents( "org.sonatype.mavenbook.multi", "simple-parent", "0.9-MNG6656-SNAPSHOT", "pom", content );

        content = FileUtils.fileRead( new File( testDir, "expected/simple-weather.pom") ); 
        verifier.assertArtifactContents( "org.sonatype.mavenbook.multi", "simple-weather", "0.9-MNG6656-SNAPSHOT", "pom", content );

        content = FileUtils.fileRead( new File( testDir, "expected/simple-webapp.pom") ); 
        verifier.assertArtifactContents( "org.sonatype.mavenbook.multi", "simple-webapp", "0.9-MNG6656-SNAPSHOT", "pom", content );
    }

}
