package org.apache.maven.artifact.factory;

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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.VersionRange;

public class DefaultArtifactFactory
    implements ArtifactFactory
{
    // TODO: remove, it doesn't know the ones from the plugins
    private ArtifactHandlerManager artifactHandlerManager;

    public DefaultArtifactFactory()
    {
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return createArtifact( groupId, artifactId, version, scope, type, null, null );
    }

    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                                  String classifier )
    {
        return createArtifact( groupId, artifactId, version, null, type, classifier, null );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                              String classifier, String scope )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, null );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                              String classifier, String scope, boolean optional )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, null, optional );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                              String classifier, String scope, String inheritedScope )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
    }

    public Artifact createDependencyArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                              String classifier, String scope, String inheritedScope, boolean optional )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope, optional );
    }

    public Artifact createBuildArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return createArtifact( groupId, artifactId, version, null, packaging, null, null );
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        return createProjectArtifact( groupId, artifactId, version, null );
    }

    public Artifact createParentArtifact( String groupId, String artifactId, String version )
    {
        return createProjectArtifact( groupId, artifactId, version );
    }

    public Artifact createPluginArtifact( String groupId, String artifactId, VersionRange versionRange )
    {
        return createArtifact( groupId, artifactId, versionRange, "maven-plugin", null, Artifact.SCOPE_RUNTIME, null );
    }

    public Artifact createProjectArtifact( String groupId, String artifactId, String version, String scope )
    {
        return createArtifact( groupId, artifactId, version, scope, "pom" );
    }

    public Artifact createExtensionArtifact( String groupId, String artifactId, VersionRange versionRange )
    {
        return createArtifact( groupId, artifactId, versionRange, "jar", null, Artifact.SCOPE_RUNTIME, null );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type,
                                     String classifier, String inheritedScope )
    {
        VersionRange versionRange = null;
        if ( version != null )
        {
            versionRange = VersionRange.createFromVersion( version );
        }
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope );
    }

    private Artifact createArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                     String classifier, String scope, String inheritedScope )
    {
        return createArtifact( groupId, artifactId, versionRange, type, classifier, scope, inheritedScope, false );
    }

    private Artifact createArtifact( String groupId, String artifactId, VersionRange versionRange, String type,
                                     String classifier, String scope, String inheritedScope, boolean optional )
    {
        // TODO: can refactor - inherited scope calculation belongs in the collector, use scope handler

        String desiredScope = Artifact.SCOPE_RUNTIME;
        if ( inheritedScope == null )
        {
            desiredScope = scope;
        }
        else if ( Artifact.SCOPE_TEST.equals( scope ) || Artifact.SCOPE_PROVIDED.equals( scope ) )
        {
            return null;
        }
        else if ( Artifact.SCOPE_COMPILE.equals( scope ) && Artifact.SCOPE_COMPILE.equals( inheritedScope ) )
        {
            // added to retain compile scope. Remove if you want compile inherited as runtime
            desiredScope = Artifact.SCOPE_COMPILE;
        }

        if ( Artifact.SCOPE_TEST.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_TEST;
        }

        if ( Artifact.SCOPE_PROVIDED.equals( inheritedScope ) )
        {
            desiredScope = Artifact.SCOPE_PROVIDED;
        }

        if ( Artifact.SCOPE_SYSTEM.equals( scope ) )
        {
            // system scopes come through unchanged...
            desiredScope = Artifact.SCOPE_SYSTEM;
        }
        
        ArtifactHandler handler = artifactHandlerManager.getArtifactHandler( type );

        return new DefaultArtifact( groupId, artifactId, versionRange, desiredScope, type, classifier, handler,
                                    optional );
    }

    protected ArtifactHandlerManager getArtifactHandlerManager()
    {
        return artifactHandlerManager;
    }
}
