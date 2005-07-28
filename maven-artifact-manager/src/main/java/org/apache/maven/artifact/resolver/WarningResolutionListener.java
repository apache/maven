package org.apache.maven.artifact.resolver;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.logging.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Send resolution warning events to the warning log.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class WarningResolutionListener
    implements ResolutionListener
{
    private Logger logger;

    private static Set ignoredArtifacts = new HashSet();

    public WarningResolutionListener( Logger logger )
    {
        this.logger = logger;
    }

    public void testArtifact( Artifact node )
    {
    }

    public void startProcessChildren( Artifact artifact )
    {
    }

    public void endProcessChildren( Artifact artifact )
    {
    }

    public void includeArtifact( Artifact artifact )
    {
    }

    public void omitForNearer( Artifact omitted, Artifact kept )
    {
    }

    public void omitForCycle( Artifact omitted )
    {
    }

    public void updateScopeCurrentPom( Artifact artifact, String scope )
    {
        // TODO: better way than static? this might hide messages in a reactor
        if ( !ignoredArtifacts.contains( artifact ) )
        {
            logger.warn( "\n\tArtifact " + artifact.getId() + " has scope '" + artifact.getScope() +
                "' replaced with '" + scope + "'\n" +
                "\tas a dependency has given a broader scope. If this is not intended, use -X to locate the dependency,\n" +
                "\tor force the desired scope using dependencyManagement.\n" );
            ignoredArtifacts.add( artifact );
        }
    }

    public void updateScope( Artifact artifact, String scope )
    {
    }

    public void manageArtifact( Artifact artifact, Artifact replacement )
    {
    }
}
