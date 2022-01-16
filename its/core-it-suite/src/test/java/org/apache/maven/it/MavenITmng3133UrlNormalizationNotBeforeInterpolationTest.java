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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3133">MNG-3133</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng3133UrlNormalizationNotBeforeInterpolationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3133UrlNormalizationNotBeforeInterpolationTest()
    {
        super( "[3.0-beta-1,)" );
    }

    /**
     * Verify that URL normalization does not happen before interpolation which would result in invalid
     * inherited URLs for project layouts where the parent resides in a sibling directory of the child
     * and expressions are used for the parent URLs ("${expression}/../foo" -&gt; "foo").
     *
     * @throws Exception in case of failure
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3133" );

        Verifier verifier = newVerifier( new File( testDir, "child" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/url.properties" );
        String url;

        url = props.getProperty( "project.url" );
        assertEquals( "http://server.org/child", url );
        url = props.getProperty( "project.scm.url" );
        assertEquals( "scm:svn:https://svn.org/child", url );
        url = props.getProperty( "project.scm.connection" );
        assertEquals( "scm:svn:https://svn.org/child", url );
        url = props.getProperty( "project.scm.developerConnection" );
        assertEquals( "scm:svn:https://svn.org/child", url );
        url = props.getProperty( "project.distributionManagement.site.url" );
        assertEquals( "dav://server.org/child", url );

        url = props.getProperty( "project.properties.projectUrl" );
        assertEquals( "http://server.org/child/it", url );
        url = props.getProperty( "project.properties.projectScmUrl" );
        assertEquals( "scm:svn:https://svn.org/child/it", url );
        url = props.getProperty( "project.properties.projectScmConn" );
        assertEquals( "scm:svn:https://svn.org/child/it", url );
        url = props.getProperty( "project.properties.projectScmDevConn" );
        assertEquals( "scm:svn:https://svn.org/child/it", url );
        url = props.getProperty( "project.properties.projectDistSiteUrl" );
        assertEquals( "dav://server.org/child/it", url );
    }

}
