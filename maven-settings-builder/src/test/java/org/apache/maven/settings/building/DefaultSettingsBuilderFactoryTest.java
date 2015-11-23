package org.apache.maven.settings.building;

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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuilderFactoryTest
{

    private SettingsBuilder builder;
    private DefaultSettingsBuildingRequest request;

    @Before
    public void setUp() throws Exception
    {
        builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull( builder );

        request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties( System.getProperties() );
    }

    @Test
    public void completeWiring()
        throws Exception
    {

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties( System.getProperties() );
        request.setUserSettingsFile( getSettings( "simple" ) );

        SettingsBuildingResult result = builder.build( request );
        assertNotNull( result );
        assertNotNull( result.getEffectiveSettings() );
    }

    @Test
    public void globalMirrorDeclarationCanBeBuild()
        throws Exception
    {
        request.setUserSettingsFile( getSettings( "mirrors-global" ) );

        SettingsBuildingResult result = builder.build( request );
        assertNotNull( result );
        assertNotNull( result.getEffectiveSettings() );
        assertThat( result.getEffectiveSettings().getMirrors().size(), is( 1 ) );
        assertThat( result.getEffectiveSettings().getMirrors().get( 0 ).getId(), is( "global-mirror-uk" ) );
    }

    @Test
    public void mirrorDeclarationWithinProfileCanBeBuild()
        throws Exception
    {
        request.setUserSettingsFile( getSettings( "mirrors-profiled" ) );

        SettingsBuildingResult result = builder.build( request );
        assertNotNull( result );
        assertNotNull( result.getEffectiveSettings() );
        assertThat( result.getEffectiveSettings().getMirrors().size(), is( 0 ) );
        assertThat( result.getEffectiveSettings().getProfiles().size(), is( 1 ) );
        assertThat( result.getEffectiveSettings().getProfiles().get( 0 ).getMirrors().size(), is( 1 ) );
        assertThat( result.getEffectiveSettings().getProfiles().get( 0 ).getMirrors().get( 0 ).getId(), is( "profiled-mirror-uk" ) );
    }

    private File getSettings( String name )
    {
        return new File( "src/test/resources/settings/factory/" + name + ".xml" ).getAbsoluteFile();
    }

}
