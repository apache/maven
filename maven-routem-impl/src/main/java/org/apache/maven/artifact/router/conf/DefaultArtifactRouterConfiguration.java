/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.artifact.router.conf;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public final class DefaultArtifactRouterConfiguration
    implements ArtifactRouterConfiguration
{

    private final File routesFile;

    private final boolean disabled;

    private final String discoveryStrategy;

    private final String selectionStrategy;

    private final Set<ArtifactRouterOption> routerOptions;

    private final RouterSource groupSource;

    private final RouterSource mirrorSource;

    public DefaultArtifactRouterConfiguration( File routesFile, RouterSource groupSource, RouterSource mirrorSource,
                                               String discoveryStrategy, String selectionStrategy, boolean disabled,
                                               Set<ArtifactRouterOption> options )
    {
        this.routesFile = routesFile;
        this.groupSource = groupSource;
        this.mirrorSource = mirrorSource;
        this.discoveryStrategy = discoveryStrategy;
        this.selectionStrategy = selectionStrategy;
        this.disabled = disabled;

        if ( options == null || options.isEmpty() )
        {
            routerOptions = Collections.unmodifiableSet( Collections.singleton( ArtifactRouterOption.update ) );
        }
        else
        {
            routerOptions = Collections.unmodifiableSet( options );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getGroupSource()
     */
    public RouterSource getGroupSource()
    {
        return groupSource;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getMirrorSource()
     */
    public RouterSource getMirrorSource()
    {
        return mirrorSource;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#isDisabled()
     */
    public boolean isDisabled()
    {
        return disabled;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getDiscoveryStrategy()
     */
    public String getDiscoveryStrategy()
    {
        return discoveryStrategy;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getSelectionStrategy()
     */
    public String getSelectionStrategy()
    {
        return selectionStrategy;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getRoutesFile()
     */
    public File getRoutesFile()
    {
        return routesFile;
    }

    // public ArtifactRouterConfiguration setOffline( boolean offline )
    // {
    // return setOption( ArtifactRouterOption.offline, offline );
    // }
    //

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#isOffline()
     */
    public boolean isOffline()
    {
        return this.routerOptions.contains( ArtifactRouterOption.offline );
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#isClear()
     */
    public boolean isClear()
    {
        return routerOptions.contains( ArtifactRouterOption.clear );
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#isUpdate()
     */
    public boolean isUpdate()
    {
        return routerOptions.contains( ArtifactRouterOption.update );
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getDefaultGroupSource()
     */
    public RouterSource getDefaultGroupSource()
    {
        return CANONICAL_GROUP_SOURCE;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.artifact.router.conf.ArtifactRouterConfiguration#getDefaultMirrorSource()
     */
    public RouterSource getDefaultMirrorSource()
    {
        return CANONICAL_MIRROR_SOURCE;
    }

}
