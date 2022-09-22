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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4973">MNG-4973</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4973ExtensionVisibleToPluginInReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4973ExtensionVisibleToPluginInReactorTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0.3,)" );
    }

    /**
     * Verify that a given plugin within a reactor build gets run with the proper class loader that is wired to
     * the extensions of the current module. More technically speaking, the plugin class realm cache must be keyed
     * by the current project and its build extensions as well.
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4973" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "sub-b/target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4973" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "sub-b/target/artifact.properties" );
        assertNotNull( props.get( "org.apache.maven.its.mng4973:dep:it-artifact:it:0.1" ) );
    }

}
