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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4666">MNG-4666</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng4666CoreRealmImportTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4666CoreRealmImportTest()
    {
        super( "[2.0.11,2.0.99),[2.1.0,3.0-alpha-1),[3.0-beta-2,)" );
    }

    /**
     * Verify that API types from the Maven core realm are shared/imported into the plugin realm despite the plugin
     * declaring conflicting dependencies. For the core artifact filter, this boils down to the filter properly
     * recognizing such a conflicting dependency, i.e. knowing the relevant groupId:artifactId's.
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4666" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4666" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.getCliOptions().add( "-s" );
        verifier.getCliOptions().add( "settings.xml" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/type.properties" );
        List types = getTypes( props );
        assertFalse( types.isEmpty() );
        for ( Iterator it = types.iterator(); it.hasNext(); )
        {
            String type = it.next().toString();
            assertEquals( type, props.get( "plugin." + type ), props.get( "core." + type ) );
        }
    }

    private List getTypes( Properties props )
    {
        List types = new ArrayList();
        for ( Iterator it = props.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();
            if ( key.startsWith( "core." ) )
            {
                String type = key.substring( 5 );
                if ( props.getProperty( key, "" ).length() > 0 )
                {
                    // types not in the core realm can't be exported/shared, so ignore those
                    types.add( type );
                }
            }
        }
        return types;
    }

}
