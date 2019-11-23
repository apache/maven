package org.apache.maven.feature.check;

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

import java.io.File;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.feature.api.MavenFeatures;
import org.apache.maven.feature.spi.DefaultMavenFeatures;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Helper class to work around class scanning/loading issues.
 */
class Helper
{
    static void enableFeatures( MavenSession session, String targetVersion, PlexusContainer container,
                                File topLevelProjectFile )
        throws MavenExecutionException, ClassNotFoundException, ComponentLookupException
    {
        MavenFeatures features = container.lookup( MavenFeatures.class, "default" );
        if ( !( features instanceof DefaultMavenFeatures ) )
        {
            throw new MavenExecutionException(
                "This project uses experimental features that require exactly Maven " + targetVersion
                    + ", cannot enable experimental features because feature flag component is not as expected (was: "
                    + features + ")", topLevelProjectFile );
        }
        ( (DefaultMavenFeatures) features ).enable( session );
    }
}
