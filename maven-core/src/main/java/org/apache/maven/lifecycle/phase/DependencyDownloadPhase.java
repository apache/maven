package org.apache.maven.lifecycle.phase;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.lifecycle.AbstractMavenLifecyclePhase;
import org.apache.maven.lifecycle.MavenGoalExecutionContext;

import java.util.Iterator;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DependencyDownloadPhase
    extends AbstractMavenLifecyclePhase
{
    public void execute( MavenGoalExecutionContext context )
        throws Exception
    {
        ArtifactResolver artifactResolver = null;

        try
        {
            artifactResolver = (ArtifactResolver) context.lookup( ArtifactResolver.ROLE );

            for ( Iterator it = context.getProject().getArtifacts().iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                artifactResolver.resolve( artifact,
                                          context.getRemoteRepositories(),
                                          context.getLocalRepository() );
            }
        }
        finally
        {
            context.release( artifactResolver );
        }
    }
}
