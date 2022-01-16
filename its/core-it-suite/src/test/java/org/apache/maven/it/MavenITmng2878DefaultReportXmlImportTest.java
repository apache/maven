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
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2878">MNG-2878</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2878DefaultReportXmlImportTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng2878DefaultReportXmlImportTest()
    {
        // reporting is not supported in 3.x core (see MNG-4162)
        super( "(,3.0-alpha-1)" );
    }

    /**
     * Verify that a plugin can load default-report.xml from the core.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG2878()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2878" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties pclProps = verifier.loadProperties( "target/pcl.properties" );
        assertNotNull( "target/pcl.properties does not contain 'default-report.xml' property",
                       pclProps.getProperty( "default-report.xml" ) );

        Properties tcclProps = verifier.loadProperties( "target/tccl.properties" );
        assertEquals( pclProps, tcclProps );
    }

}
