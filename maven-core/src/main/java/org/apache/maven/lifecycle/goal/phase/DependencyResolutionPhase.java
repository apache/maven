package org.apache.maven.lifecycle.goal.phase;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.lifecycle.goal.AbstractMavenGoalPhase;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.lifecycle.goal.MavenGoalExecutionContext;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;

import java.util.Iterator;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DependencyResolutionPhase
    extends AbstractMavenGoalPhase
{
    public void execute( MavenGoalExecutionContext context )
        throws GoalExecutionException
    {
        boolean requiresDependencies = false;

        for ( Iterator iterator = context.getResolvedGoals().iterator(); iterator.hasNext(); )
        {
            String goalName = (String) iterator.next();

            MojoDescriptor mojoDescriptor = context.getMojoDescriptor( goalName );

            if ( mojoDescriptor.requiresDependencyResolution() )
            {
                requiresDependencies = true;

                try
                {
                    resolveTransitiveDependencies( context );
                }
                catch ( Exception e )
                {
                    throw new GoalExecutionException( "Can't resolve transitive dependencies: ", e );
                }

                break;
            }
        }

        context.requiresDependencies( requiresDependencies );
    }

    private void resolveTransitiveDependencies( MavenGoalExecutionContext context )
        throws Exception
    {
        ArtifactResolver artifactResolver = null;

        try
        {
            MavenProject project = context.getProject();

            artifactResolver = (ArtifactResolver) context.lookup( ArtifactResolver.ROLE );

            MavenMetadataSource sourceReader = new MavenMetadataSource( context.getRemoteRepositories(),
                                                                        context.getLocalRepository(),
                                                                        artifactResolver );

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                    context.getRemoteRepositories(),
                                                                                    context.getLocalRepository(),
                                                                                    sourceReader );

            project.getArtifacts().addAll( result.getArtifacts().values() );
        }
        finally
        {
            context.release( artifactResolver );
        }
    }
}
