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
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4528">MNG-4528</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4528ExcludeWagonsFromMavenCoreArtifactsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4528ExcludeWagonsFromMavenCoreArtifactsTest()
    {
        super( "[2.0.5,3.0-alpha-1),[3.0-alpha-7,4.0.0-alpha-1)" );
    }

    /**
     * Test that wagon providers pulled in via transitive dependencies of Maven core artifacts get excluded from
     * plugin realms (in favor of potentially newer wagons bundled with the core). This requirement is mostly a
     * hack to compensate for the historic slip of Maven core artifacts depending on wagon providers. Those old
     * wagon providers conflict with the usually newer wagons bundled with the core distro and cause grief under
     * a class loader hierarchy where wagons are loaded from the plugin realm (if available) like in Maven 3.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4528" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), "remote" );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/wagon.properties" );
        String version = props.getProperty( "version", "" );
        assertNotEquals( "Bad wagon version used: " + version, "1.0-alpha-6", version );
    }

}
