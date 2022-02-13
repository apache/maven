package org.apache.maven.api;

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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerException;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactFactoryException;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactInstallerException;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.api.services.DependencyCollector;
import org.apache.maven.api.services.DependencyCollectorException;
import org.apache.maven.api.services.DependencyCollectorResult;
import org.apache.maven.api.services.DependencyFactory;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.Service;

/**
 * The session to install / deploy / resolve artifacts and dependencies.
 */
@ThreadSafe
public interface Session
{

    @Nonnull
    LocalRepository getLocalRepository();

    @Nonnull
    List<RemoteRepository> getRemoteRepositories();

    /**
     * Retrieves the service for the interface
     *
     * @throws NoSuchElementException if the service could not be found
     */
    @Nonnull
    <T extends Service> T getService( Class<T> clazz ) throws NoSuchElementException;

    /**
     * Creates a derived session using the given local repository.
     *
     * @param localRepository the new local repository
     * @return the derived session
     */
    @Nonnull
    Session withLocalRepository( @Nonnull LocalRepository localRepository );

    /**
     * Creates a derived session using the given remote repositories.
     *
     * @param repositories the new list of remote repositories
     * @return the derived session
     */
    @Nonnull
    Session withRemoteRepositories( @Nonnull List<RemoteRepository> repositories );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     */
    default Artifact createArtifact( String groupId, String artifactId, String version, String type )
            throws ArtifactFactoryException, IllegalArgumentException
    {
        return getService( ArtifactFactory.class )
                .create( this, groupId, artifactId, version, type );
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     */
    default ArtifactResolverResult resolveArtifact( Artifact artifact )
            throws ArtifactResolverException, IllegalArgumentException
    {
        return getService( ArtifactResolver.class )
                .resolve( this, artifact );
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).install(...)</code>
     */
    default void installArtifact( Artifact... artifacts )
        throws ArtifactInstallerException, IllegalArgumentException
    {
        getService( ArtifactInstaller.class )
                .install( this, Arrays.asList( artifacts ) );
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).deploy(...)</code>
     */
    default void deployArtifact( RemoteRepository repository, Artifact... artifacts )
        throws ArtifactDeployerException, IllegalArgumentException
    {
        getService( ArtifactDeployer.class )
                .deploy( this, repository, Arrays.asList( artifacts ) );
    }

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     */
    default Dependency createDependency( Artifact artifact )
    {
        return getService( DependencyFactory.class )
                .create( this, artifact );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     */
    default DependencyCollectorResult collectDependencies( Artifact artifact )
            throws DependencyCollectorException, IllegalArgumentException
    {
        return getService( DependencyCollector.class )
                .collect( this, artifact );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     */
    default DependencyCollectorResult collectDependencies( Project project )
            throws DependencyCollectorException, IllegalArgumentException
    {
        return getService( DependencyCollector.class )
                .collect( this, project );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     */
    default DependencyCollectorResult collectDependencies( Dependency dependency )
            throws DependencyCollectorException, IllegalArgumentException
    {
        return getService( DependencyCollector.class )
                .collect( this, dependency );
    }

    /**
     * Shortcut for <code>getService(DependencyResolver.class).resolve(...)</code>
     */
    default DependencyResolverResult resolveDependencies( Dependency dependency )
            throws DependencyResolverException, IllegalArgumentException
    {
        return getService( DependencyResolver.class )
                .resolve( this, dependency, null );
    }

}
