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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-1412">MNG-1412</a>:
 * it tests that dependencies order in classpath matches <code>pom.xml</code>.
 *
 * @author <a href="mailto:hboutemy@apache.org">Hervé Boutemy</a>
 *
 */
public class MavenITmng1412DependenciesOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng1412DependenciesOrderTest()
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    @Test
    public void testitMNG1412()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1412" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng1412" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> compileArtifacts = verifier.loadLines( "target/compile-artifacts.txt", "UTF-8" );
        assertArtifactOrder( compileArtifacts );

        List<String> compileClassPath = verifier.loadLines( "target/compile-classpath.txt", "UTF-8" );
        assertClassPathOrder( compileClassPath.subList( 1, compileClassPath.size() ) );

        List<String> runtimeArtifacts = verifier.loadLines( "target/runtime-artifacts.txt", "UTF-8" );
        assertArtifactOrder( runtimeArtifacts );

        List<String> runtimeClassPath = verifier.loadLines( "target/runtime-classpath.txt", "UTF-8" );
        assertClassPathOrder( runtimeClassPath.subList( 1, runtimeClassPath.size() ) );

        List<String> testArtifacts = verifier.loadLines( "target/test-artifacts.txt", "UTF-8" );
        assertArtifactOrder( testArtifacts );

        List<String> testClassPath = verifier.loadLines( "target/test-classpath.txt", "UTF-8" );
        assertClassPathOrder( testClassPath.subList( 2, testClassPath.size() ) );
    }

    private void assertArtifactOrder( List<String> artifacts )
    {
        assertEquals( 4, artifacts.size() );
        assertEquals( "org.apache.maven.its.mng1412:a:jar:0.1", artifacts.get( 0 ) );
        assertEquals( "org.apache.maven.its.mng1412:c:jar:0.1", artifacts.get( 1 ) );
        assertEquals( "org.apache.maven.its.mng1412:b:jar:0.1", artifacts.get( 2 ) );
        assertEquals( "org.apache.maven.its.mng1412:d:jar:0.1", artifacts.get( 3 ) );
    }

    private void assertClassPathOrder( List<String> classpath )
    {
        assertEquals( 4, classpath.size() );
        assertEquals( "a-0.1.jar", classpath.get( 0 ) );
        assertEquals( "c-0.1.jar", classpath.get( 1 ) );
        assertEquals( "b-0.1.jar", classpath.get( 2 ) );
        assertEquals( "d-0.1.jar", classpath.get( 3 ) );
    }

}
