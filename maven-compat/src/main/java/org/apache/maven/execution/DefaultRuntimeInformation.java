package org.apache.maven.execution;

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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Describes runtime information about the application.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Deprecated
@Component( role = RuntimeInformation.class )
public class DefaultRuntimeInformation
    implements RuntimeInformation, Initializable
{

    @Requirement
    private org.apache.maven.rtinfo.RuntimeInformation rtInfo;

    private ArtifactVersion applicationVersion;

    public ArtifactVersion getApplicationVersion()
    {
        return applicationVersion;
    }

    public void initialize()
        throws InitializationException
    {
        String mavenVersion = rtInfo.getMavenVersion();

        if ( StringUtils.isEmpty( mavenVersion ) )
        {
            throw new InitializationException( "Unable to read Maven version from maven-core" );
        }

        applicationVersion = new DefaultArtifactVersion( mavenVersion );
    }

}
