package org.apache.maven.plugin.internal;

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

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Injects plexus-utils:1.1 into a plugin's class path if it doesn't already declare a dependency on plexus-utils. This
 * is another legacy bit to provide backward-compat with Maven 2.x.
 *
 * @author Benjamin Bentmann
 */
class PlexusUtilsInjector
    implements DependencyGraphTransformer
{

    private static final String GID = "org.codehaus.plexus";

    private static final String AID = "plexus-utils";

    private static final String VER = "1.1";

    private static final String EXT = "jar";

    public DependencyNode transformGraph( DependencyNode node, DependencyGraphTransformationContext context )
        throws RepositoryException
    {
        if ( findPlexusUtils( node ) == null )
        {
            Artifact pu = new DefaultArtifact( GID, AID, null, EXT, VER );
            DefaultDependencyNode child = new DefaultDependencyNode( new Dependency( pu, JavaScopes.RUNTIME ) );
            child.setRepositories( node.getRepositories() );
            child.setRequestContext( node.getRequestContext() );
            node.getChildren().add( child );
        }

        return node;
    }

    private DependencyNode findPlexusUtils( DependencyNode node )
    {
        DependencyNode plexusUtils = null;

        find:
        {
            if ( AID.equals( node.getArtifact().getArtifactId() )
                     && GID.equals( node.getArtifact().getGroupId() )
                     && EXT.equals( node.getArtifact().getExtension() )
                     && "".equals( node.getArtifact().getClassifier() ) )
            {
                plexusUtils = node;
                break find;
            }

            for ( DependencyNode child : node.getChildren() )
            {
                DependencyNode result = findPlexusUtils( child );

                if ( result != null )
                {
                    plexusUtils = result;
                    break find;
                }
            }
        }

        return plexusUtils;
    }

}
