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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2293">MNG-2293</a>.
 *
 *
 */
public class MavenITmng2293CustomPluginParamImplTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2293CustomPluginParamImplTest()
    {
        super( "(2.0.4,)" );
    }

    /**
     * Verify that default implementation of an implementation for a complex object works as expected.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG2293()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2293" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/param.properties" );
        assertEquals( "org.apache.maven.plugin.coreit.sub.AnImplementation-foobar", props.getProperty( "theParameter.string" ) );
        assertEquals( "org.apache.maven.plugin.coreit.sub.AnImplementation", props.getProperty( "theParameter.class" ) );
    }

}
