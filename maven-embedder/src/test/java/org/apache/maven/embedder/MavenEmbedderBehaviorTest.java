package org.apache.maven.embedder;

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

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/** @author Jason van Zyl */
public class MavenEmbedderBehaviorTest
    extends AbstractEmbedderTestCase
{
    public void testThatTheLocalRepositoryIsTakenFromGlobalSettingsWhenUserSettingsAreNull()
        throws Exception
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Configuration configuration = new DefaultConfiguration()
            .setClassLoader( classLoader )
            .setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() )
            .setUserSettingsFile( null )
            .setGlobalSettingsFile( new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" ) );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

         assertTrue( result.isValid() );

        MavenEmbedder maven = new MavenEmbedder( configuration );

        String expectedPath = ( new File( "/global/maven/local-repository" ) ).getCanonicalPath();
        String actualPath = maven.getLocalRepository().getBasedir();
        assertEquals( expectedPath, actualPath );

        maven.stop();
    }
}
