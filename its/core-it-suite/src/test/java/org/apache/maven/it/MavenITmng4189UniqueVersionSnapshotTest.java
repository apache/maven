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

import java.io.File;
import java.util.Properties;

import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4189">MNG-4189</a>.
 * 
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class MavenITmng4189UniqueVersionSnapshotTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4189UniqueVersionSnapshotTest()
    {
        super( "[2.2.1,),[3.0-alpha-3,)" );
    }

    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4189" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4189" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );

        // depend on org.apache.maven.its.mng4189:dep:1.0-20090608.090416-1:jar 
        verifier.setLogFileName( "log-1.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        Properties checksums = verifier.loadProperties( "target/checksum.properties" );
        assertEquals( "da2e54f69a9ba120f9211c476029f049967d840c", checksums.getProperty( "dep-1.0-SNAPSHOT.jar" ) );

        // depend on org.apache.maven.its.mng4189:dep:1.0-20090608.090416-2:jar 
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-f" );
        verifier.addCliOption( "dependent-on-newer-timestamp-pom.xml" );
        verifier.setLogFileName( "log-2.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();

        checksums = verifier.loadProperties( "target/checksum.properties" );
        assertEquals( "835979c28041014c5fd55daa15302d92976924a7", checksums.getProperty( "dep-1.0-SNAPSHOT.jar" ) );

        // revert back to org.apache.maven.its.mng4189:dep:1.0-20090608.090416-1:jar 
        verifier.deleteDirectory( "target" );
        verifier.getCliOptions().remove( "-f" );
        verifier.getCliOptions().remove( "dependent-on-newer-timestamp-pom.xml" );
        verifier.setLogFileName( "log-3.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        checksums = verifier.loadProperties( "target/checksum.properties" );
        assertEquals( "da2e54f69a9ba120f9211c476029f049967d840c", checksums.getProperty( "dep-1.0-SNAPSHOT.jar" ) );
    }

}
