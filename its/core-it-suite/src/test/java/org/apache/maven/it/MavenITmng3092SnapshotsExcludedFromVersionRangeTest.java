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
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3092">MNG-3092</a>.
 *
 * @author Benjamin Bentmann
 */
@Disabled( "not fixed yet" )
public class MavenITmng3092SnapshotsExcludedFromVersionRangeTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3092SnapshotsExcludedFromVersionRangeTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that snapshots are not included in version ranges unless explicitly declared as the lower/upper bound
     * of the range.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3092" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng3092" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines( "target/classpath.txt", "UTF-8" );
        assertTrue( classpath.toString(), classpath.contains( "a-1.1.jar" ) );
        assertTrue( classpath.toString(), classpath.contains( "b-1.0-SNAPSHOT.jar" ) );
        assertTrue( classpath.toString(), classpath.contains( "c-1.1-SNAPSHOT.jar" ) );
    }

}
