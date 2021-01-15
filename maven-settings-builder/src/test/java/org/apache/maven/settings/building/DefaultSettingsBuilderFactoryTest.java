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

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuilderFactoryTest
{

    private File getSettings( String name )
    {
        return new File( "src/test/resources/settings/factory/" + name + ".xml" ).getAbsoluteFile();
    }

    @Test
    public void testCompleteWiring()
        throws Exception
    {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull( builder );

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties( System.getProperties() );
        request.setUserSettingsFile( getSettings( "simple" ) );

        SettingsBuildingResult result = builder.build( request );
        assertNotNull( result );
        assertNotNull( result.getEffectiveSettings() );
    }

}
