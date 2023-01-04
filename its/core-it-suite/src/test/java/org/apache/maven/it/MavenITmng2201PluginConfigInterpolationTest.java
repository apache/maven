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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2201">MNG-2201</a>.
 *
 *
 */
public class MavenITmng2201PluginConfigInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2201PluginConfigInterpolationTest()
    {
        super( "(2.0.8,)" );
    }

    /**
     * Verify that plugin configurations are correctly interpolated
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2201()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2201" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/config.properties" );
        assertCanonicalFileEquals( new File( testDir, "target" ), new File( props.getProperty( "stringParam" ) ) );
        assertCanonicalFileEquals( new File( testDir, "target" ), new File( props.getProperty( "propertiesParam.buildDir" ) ) );
        assertCanonicalFileEquals( new File( testDir, "target" ), new File( props.getProperty( "mapParam.buildDir" ) ) );
        assertEquals( "4.0.0", props.getProperty( "domParam.children.modelVersion.0.value" ) );
        assertEquals( "org.apache.maven.its.it0104", props.getProperty( "domParam.children.groupId.0.value" ) );
        assertEquals( "1.0-SNAPSHOT", props.getProperty( "domParam.children.version.0.value" ) );
        assertEquals( "jar", props.getProperty( "domParam.children.packaging.0.value" ) );
        assertEquals( "http://maven.apache.org", props.getProperty( "domParam.children.url.0.value" ) );
        assertEquals( "Apache", props.getProperty( "domParam.children.organization.0.children.name.0.value" ) );
        assertCanonicalFileEquals( new File( testDir, "target" ), new File( props.getProperty( "domParam.children.build.0.children.directory.0.value" ) ) );
        assertCanonicalFileEquals( new File( testDir, "target/classes" ), new File( props.getProperty( "domParam.children.build.0.children.outputDirectory.0.value" ) ) );
    }

}
