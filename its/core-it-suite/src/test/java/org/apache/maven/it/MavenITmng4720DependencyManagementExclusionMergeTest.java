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

import java.io.File;
import java.util.List;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4720">MNG-4720</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4720DependencyManagementExclusionMergeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4720DependencyManagementExclusionMergeTest()
    {
        super( "[2.0.6,)" );
    }

    /**
     * Verify the effective exclusions applied during transitive dependency resolution when both the regular
     * dependency section and dependency management declare exclusions for a particular dependency.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4720" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4720" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List<String> classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );

        assertTrue( classpath.toString(), classpath.contains( "a-0.1.jar" ) );
        assertTrue( classpath.toString(), classpath.contains( "c-0.1.jar" ) );

        assertFalse( classpath.toString(), classpath.contains( "b-0.1.jar" ) );

        // should better have been excluded as well, now it's a matter of backward-compat
        assertTrue( classpath.toString(), classpath.contains( "d-0.1.jar" ) );
    }

}
