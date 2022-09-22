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
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3441">MNG-3441</a>.
 *
 *
 */
public class MavenITmng3441MetadataUpdatedFromDeploymentRepositoryTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3441MetadataUpdatedFromDeploymentRepositoryTest()
    {
        super( "(2.0.8,)" );
    }

    public void testitMNG3441()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3441" );

        File targetRepository = new File( testDir, "target-repo" );
        FileUtils.deleteDirectory( targetRepository );
        FileUtils.copyDirectoryStructure( new File( testDir, "deploy-repo" ), targetRepository );

        Verifier verifier;

        verifier = newVerifier( testDir.getAbsolutePath(), "remote" );

        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );
        verifier.executeGoal( "deploy" );

        verifier.verifyErrorFreeLog();

        Xpp3Dom dom = readDom( new File( targetRepository,
                                         "org/apache/maven/its/mng3441/test-artifact/1.0-SNAPSHOT/maven-metadata.xml"
        ) );
        assertEquals( "2", dom.getChild( "versioning" ).getChild( "snapshot" ).getChild( "buildNumber" ).getValue() );

        dom = readDom( new File( targetRepository, "org/apache/maven/its/mng3441/maven-metadata.xml" ) );
        Xpp3Dom[] plugins = dom.getChild( "plugins" ).getChildren();
        assertEquals( "other-plugin", plugins[0].getChild( "prefix" ).getValue() );
        assertEquals( "test-artifact", plugins[1].getChild( "prefix" ).getValue() );

        verifier.resetStreams();
    }

    private Xpp3Dom readDom( File file )
        throws XmlPullParserException, IOException
    {
        try ( FileReader reader = new FileReader( file ) )
        {
            return Xpp3DomBuilder.build( reader );
        }
    }
}
