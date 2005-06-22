package org.apache.maven.artifact.ant;

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
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.tools.ant.Project;

/**
 * Show resolution information in Ant.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class AntResolutionListener
    implements ResolutionListener
{
    private String indent = "";

    private final Project project;

    public AntResolutionListener( Project project )
    {
        this.project = project;
    }

    public void testArtifact( Artifact node )
    {
    }

    public void startProcessChildren( Artifact artifact )
    {
        indent += "  ";
    }

    public void endProcessChildren( Artifact artifact )
    {
        indent = indent.substring( 2 );
    }

    public void includeArtifact( Artifact artifact )
    {
        project.log( indent + artifact.getId() + " (selected)" );
    }

    public void omitForNearer( Artifact omitted, Artifact kept )
    {
        project.log( indent + omitted.getId() + " (removed - nearer found: " + kept.getVersion() + ")" );
    }

    public void omitForCycle( Artifact omitted )
    {
        project.log( indent + omitted.getId() + " (removed - causes a cycle in the graph)" );
    }

    public void updateScope( Artifact artifact, String scope )
    {
        project.log( indent + artifact.getId() + " (settings scope to: " + scope + ")" );
    }

    public void manageArtifact( Artifact artifact, Artifact replacement )
    {
        String msg = indent + artifact.getId();
        msg += " (";
        if ( replacement.getVersion() != null )
        {
            msg += "applying version: " + replacement.getVersion() + ";";
        }
        if ( replacement.getScope() != null )
        {
            msg += "applying scope: " + replacement.getScope();
        }
        msg += ")";
        project.log( msg );
    }
}
