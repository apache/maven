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
package org.apache.maven.toolchain.merge;

import java.io.InputStream;

import org.apache.maven.api.toolchain.PersistedToolchains;
import org.apache.maven.api.toolchain.TrackableBase;
import org.apache.maven.api.xml.Dom;
import org.apache.maven.toolchain.v4.MavenToolchainsXpp3Reader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenToolchainMergerTest
{
    private MavenToolchainMerger merger = new MavenToolchainMerger();

    private MavenToolchainsXpp3Reader reader = new MavenToolchainsXpp3Reader();

    @Test
    public void testMergeNulls()
    {
        merger.merge( null, null, null );

        PersistedToolchains pt = PersistedToolchains.newInstance();
        merger.merge( pt, null, null );
        merger.merge( null, pt, null );
    }

    @Test
    public void testMergeJdk()
        throws Exception
    {
        try ( InputStream isDominant = getClass().getResourceAsStream( "toolchains-jdks.xml" );
                        InputStream isRecessive = getClass().getResourceAsStream( "toolchains-jdks.xml" ) )
        {
            PersistedToolchains dominant = reader.read( isDominant );
            PersistedToolchains recessive = reader.read( isRecessive );
            assertEquals( 2, dominant.getToolchains().size() );

            PersistedToolchains merged = merger.merge( dominant, recessive, TrackableBase.USER_LEVEL );
            assertEquals( 2, merged.getToolchains().size() );
        }
    }

    @Test
    public void testMergeJdkExtra()
        throws Exception
    {
        try ( InputStream jdksIS = getClass().getResourceAsStream( "toolchains-jdks.xml" );
                        InputStream jdksExtraIS = getClass().getResourceAsStream( "toolchains-jdks-extra.xml" ) )
        {
            PersistedToolchains jdks = reader.read( jdksIS );
            PersistedToolchains jdksExtra = reader.read( jdksExtraIS );
            assertEquals( 2, jdks.getToolchains().size() );
            assertEquals( 2, jdksExtra.getToolchains().size() );

            PersistedToolchains merged = merger.merge( jdks, jdksExtra, TrackableBase.USER_LEVEL );
            assertEquals( 4, merged.getToolchains().size() );
            assertEquals( 2, jdks.getToolchains().size() );
            assertEquals( 2, jdksExtra.getToolchains().size() );
        }
        try ( InputStream jdksIS = getClass().getResourceAsStream( "toolchains-jdks.xml" );
                        InputStream jdksExtraIS = getClass().getResourceAsStream( "toolchains-jdks-extra.xml" ) )
        {
            PersistedToolchains jdks = reader.read( jdksIS );
            PersistedToolchains jdksExtra = reader.read( jdksExtraIS );
            assertEquals( 2, jdks.getToolchains().size() );
            assertEquals( 2, jdksExtra.getToolchains().size() );

            // switch dominant with recessive
            PersistedToolchains merged = merger.merge( jdksExtra, jdks, TrackableBase.USER_LEVEL );
            assertEquals( 4, merged.getToolchains().size() );
            assertEquals( 2, jdks.getToolchains().size() );
            assertEquals( 2, jdksExtra.getToolchains().size() );
        }
    }

    @Test
    public void testMergeJdkExtend()
        throws Exception
    {
        try ( InputStream jdksIS = getClass().getResourceAsStream( "toolchains-jdks.xml" );
                        InputStream jdksExtendIS = getClass().getResourceAsStream( "toolchains-jdks-extend.xml" ) )
        {
            PersistedToolchains jdks = reader.read( jdksIS );
            PersistedToolchains jdksExtend = reader.read( jdksExtendIS );
            assertEquals( 2, jdks.getToolchains().size() );

            PersistedToolchains merged = merger.merge( jdks, jdksExtend, TrackableBase.USER_LEVEL );
            assertEquals( 2, merged.getToolchains().size() );
            Dom config0 = merged.getToolchains().get( 0 ).getConfiguration();
            assertEquals( "lib/tools.jar", config0.getChild( "toolsJar" ).getValue() );
            assertEquals( 2, config0.getChildren().size() );
            Dom config1 = merged.getToolchains().get( 1 ).getConfiguration();
            assertEquals( 2, config1.getChildren().size() );
            assertEquals( "lib/classes.jar", config1.getChild( "toolsJar" ).getValue() );
            assertEquals( 2, jdks.getToolchains().size() );
            assertEquals( 2, jdksExtend.getToolchains().size() );
        }
        try ( InputStream jdksIS = getClass().getResourceAsStream( "toolchains-jdks.xml" );
                        InputStream jdksExtendIS = getClass().getResourceAsStream( "toolchains-jdks-extend.xml" ) )
        {
            PersistedToolchains jdks = reader.read( jdksIS );
            PersistedToolchains jdksExtend = reader.read( jdksExtendIS );
            assertEquals( 2, jdks.getToolchains().size() );

            // switch dominant with recessive
            PersistedToolchains merged = merger.merge( jdksExtend, jdks, TrackableBase.USER_LEVEL );
            assertEquals( 2, merged.getToolchains().size() );
            Dom config0 = merged.getToolchains().get( 0 ).getConfiguration();
            assertEquals( "lib/tools.jar", config0.getChild( "toolsJar" ).getValue() );
            assertEquals( 2, config0.getChildren().size() );
            Dom config1 = merged.getToolchains().get( 1 ).getConfiguration();
            assertEquals( 2, config1.getChildren().size() );
            assertEquals( "lib/classes.jar", config1.getChild( "toolsJar" ).getValue() );
            assertEquals( 2, jdks.getToolchains().size() );
            assertEquals( 2, jdksExtend.getToolchains().size() );
        }
    }

}
