package org.apache.maven.settings;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import junit.framework.TestCase;

public class SettingsUtilsTest
    extends TestCase
{

    public void testShouldAppendRecessivePluginGroupIds()
    {
        Settings dominant = new Settings();
        dominant.addPluginGroup( "org.apache.maven.plugins" );
        dominant.addPluginGroup( "org.codehaus.modello" );
        
        dominant.setRuntimeInfo(new RuntimeInfo(dominant));

        Settings recessive = new Settings();
        recessive.addPluginGroup( "org.codehaus.plexus" );

        recessive.setRuntimeInfo(new RuntimeInfo(recessive));

        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );

        List pluginGroups = dominant.getPluginGroups();

        assertNotNull( pluginGroups );
        assertEquals( 3, pluginGroups.size() );
        assertEquals( "org.apache.maven.plugins", pluginGroups.get( 0 ) );
        assertEquals( "org.codehaus.modello", pluginGroups.get( 1 ) );
        assertEquals( "org.codehaus.plexus", pluginGroups.get( 2 ) );
    }

}
