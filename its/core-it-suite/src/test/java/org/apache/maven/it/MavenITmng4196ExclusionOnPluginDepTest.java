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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4196">MNG-4196</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng4196ExclusionOnPluginDepTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4196ExclusionOnPluginDepTest()
    {
        super( "[2.0.9,)" );
    }

    /**
     * Verify that exclusions on a project-level plugin dependency are effective.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4196" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties pclProps = verifier.loadProperties( "target/pcl.properties" );
        assertNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassA" ) );
        assertNotNull( pclProps.getProperty( "org.apache.maven.plugin.coreit.ClassB" ) );
        assertNull( pclProps.getProperty( "org/apache/maven/plugin/coreit/a.properties" ) );
        assertNotNull( pclProps.getProperty( "org/apache/maven/plugin/coreit/b.properties" ) );

        Properties tcclProps = verifier.loadProperties( "target/tccl.properties" );
        assertEquals( pclProps, tcclProps );
    }

}
