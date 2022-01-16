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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3924">MNG-3924</a> and
 * <a href="https://issues.apache.org/jira/browse/MNG-3662">MNG-3662</a>
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3924XmlMarkupInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3924XmlMarkupInterpolationTest()
    {
        super( "[2.1.0-M1,)" );
    }

    /**
     * Test that interpolation of properties that resolve to XML markup doesn't crash the project builder.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG3924()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3924" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/xml.properties" );
        assertEquals( "<?xml version='1.0'?>Tom&Jerry", props.getProperty( "project.properties.xmlMarkup" ) );
        assertEquals( "<?xml version='1.0'?>Tom&Jerry", props.getProperty( "project.properties.xmlTest" ) );
    }

}
