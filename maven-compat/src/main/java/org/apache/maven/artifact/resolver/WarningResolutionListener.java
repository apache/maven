package org.apache.maven.artifact.resolver;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.logging.Logger;

/**
 * Send resolution warning events to the warning log.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class WarningResolutionListener
    implements ResolutionListener
{
    private Logger logger;

    public WarningResolutionListener( final Logger logger )
    {
        this.logger = logger;
    }

    public void testArtifact( final Artifact node )
    {
    }

    public void startProcessChildren( final Artifact artifact )
    {
    }

    public void endProcessChildren( final Artifact artifact )
    {
    }

    public void includeArtifact( final Artifact artifact )
    {
    }

    public void omitForNearer( final Artifact omitted,
                               final Artifact kept )
    {
    }

    public void omitForCycle( final Artifact omitted )
    {
    }

    public void updateScopeCurrentPom( final Artifact artifact,
                                       final String scope )
    {
    }

    public void updateScope( final Artifact artifact,
                             final String scope )
    {
    }

    public void manageArtifact( final Artifact artifact,
                                final Artifact replacement )
    {
    }

    public void selectVersionFromRange( final Artifact artifact )
    {
    }

    public void restrictRange( final Artifact artifact,
                               final Artifact replacement,
                               final VersionRange newRange )
    {
    }
}
