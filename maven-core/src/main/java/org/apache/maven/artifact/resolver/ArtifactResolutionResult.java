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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;

/**
 * Specific problems during resolution that we want to account for:
 * <ul>
 *   <li>missing metadata</li>
 *   <li>version range violations</li>
 *   <li>version circular dependencies</li>
 *   <li>missing artifacts</li>
 *   <li>network/transfer errors</li>
 *   <li>file system errors: permissions</li>
 * </ul>
 *
 * @author Jason van Zyl
 * TODO carlos: all these possible has*Exceptions and get*Exceptions methods make the clients too
 *       complex requiring a long list of checks, need to create a parent/interface/encapsulation
 *       for the types of exceptions
 */
public class ArtifactResolutionResult
{
    private static final String LS = System.lineSeparator();

    private Artifact originatingArtifact;

    private List<Artifact> missingArtifacts;

    // Exceptions

    private List<Exception> exceptions;

    private List<Exception> versionRangeViolations;

    private List<ArtifactResolutionException> metadataResolutionExceptions;

    private List<CyclicDependencyException> circularDependencyExceptions;

    private List<ArtifactResolutionException> errorArtifactExceptions;

    // file system errors

    private List<ArtifactRepository> repositories;

    private Set<Artifact> artifacts;

    private Set<ResolutionNode> resolutionNodes;

    public Artifact getOriginatingArtifact()
    {
        return originatingArtifact;
    }

    public ArtifactResolutionResult setOriginatingArtifact( final Artifact originatingArtifact )
    {
        this.originatingArtifact = originatingArtifact;

        return this;
    }

    public void addArtifact( Artifact artifact )
    {
        if ( artifacts == null )
        {
            artifacts = new LinkedHashSet<>();
        }

        artifacts.add( artifact );
    }

    public Set<Artifact> getArtifacts()
    {
        if ( artifacts == null )
        {
            artifacts = new LinkedHashSet<>();
        }

        return artifacts;
    }

    public void setArtifacts( Set<Artifact> artifacts )
    {
        this.artifacts = artifacts;
    }

    public Set<ResolutionNode> getArtifactResolutionNodes()
    {
        if ( resolutionNodes == null )
        {
            resolutionNodes = new LinkedHashSet<>();
        }

        return resolutionNodes;
    }

    public void setArtifactResolutionNodes( Set<ResolutionNode> resolutionNodes )
    {
        this.resolutionNodes = resolutionNodes;
    }

    public boolean hasMissingArtifacts()
    {
        return missingArtifacts != null && !missingArtifacts.isEmpty();
    }

    public List<Artifact> getMissingArtifacts()
    {
        return missingArtifacts == null
                   ? Collections.<Artifact>emptyList()
                   : Collections.unmodifiableList( missingArtifacts );

    }

    public ArtifactResolutionResult addMissingArtifact( Artifact artifact )
    {
        missingArtifacts = initList( missingArtifacts );

        missingArtifacts.add( artifact );

        return this;
    }

    public ArtifactResolutionResult setUnresolvedArtifacts( final List<Artifact> unresolvedArtifacts )
    {
        this.missingArtifacts = unresolvedArtifacts;

        return this;
    }

    public boolean isSuccess()
    {
        return !( hasMissingArtifacts() || hasExceptions() );
    }

    // ------------------------------------------------------------------------
    // Exceptions
    // ------------------------------------------------------------------------

    public boolean hasExceptions()
    {
        return exceptions != null && !exceptions.isEmpty();
    }

    public List<Exception> getExceptions()
    {
        return exceptions == null
                   ? Collections.<Exception>emptyList()
                   : Collections.unmodifiableList( exceptions );

    }

    // ------------------------------------------------------------------------
    // Version Range Violations
    // ------------------------------------------------------------------------

    public boolean hasVersionRangeViolations()
    {
        return versionRangeViolations != null;
    }

    /**
     * TODO this needs to accept a {@link OverConstrainedVersionException} as returned by
     *       {@link #getVersionRangeViolation(int)} but it's not used like that in
     *       DefaultLegacyArtifactCollector
     */
    public ArtifactResolutionResult addVersionRangeViolation( Exception e )
    {
        versionRangeViolations = initList( versionRangeViolations );

        versionRangeViolations.add( e );

        exceptions = initList( exceptions );

        exceptions.add( e );

        return this;
    }

    public OverConstrainedVersionException getVersionRangeViolation( int i )
    {
        return (OverConstrainedVersionException) versionRangeViolations.get( i );
    }

    public List<Exception> getVersionRangeViolations()
    {
        return versionRangeViolations == null
                   ? Collections.<Exception>emptyList()
                   : Collections.unmodifiableList( versionRangeViolations );

    }

    // ------------------------------------------------------------------------
    // Metadata Resolution Exceptions: ArtifactResolutionExceptions
    // ------------------------------------------------------------------------

    public boolean hasMetadataResolutionExceptions()
    {
        return metadataResolutionExceptions != null;
    }

    public ArtifactResolutionResult addMetadataResolutionException( ArtifactResolutionException e )
    {
        metadataResolutionExceptions = initList( metadataResolutionExceptions );

        metadataResolutionExceptions.add( e );

        exceptions = initList( exceptions );

        exceptions.add( e );

        return this;
    }

    public ArtifactResolutionException getMetadataResolutionException( int i )
    {
        return metadataResolutionExceptions.get( i );
    }

    public List<ArtifactResolutionException> getMetadataResolutionExceptions()
    {
        return metadataResolutionExceptions == null
                   ? Collections.<ArtifactResolutionException>emptyList()
                   : Collections.unmodifiableList( metadataResolutionExceptions );

    }

    // ------------------------------------------------------------------------
    // ErrorArtifactExceptions: ArtifactResolutionExceptions
    // ------------------------------------------------------------------------

    public boolean hasErrorArtifactExceptions()
    {
        return errorArtifactExceptions != null;
    }

    public ArtifactResolutionResult addErrorArtifactException( ArtifactResolutionException e )
    {
        errorArtifactExceptions = initList( errorArtifactExceptions );

        errorArtifactExceptions.add( e );

        exceptions = initList( exceptions );

        exceptions.add( e );

        return this;
    }

    public List<ArtifactResolutionException> getErrorArtifactExceptions()
    {
        if ( errorArtifactExceptions == null )
        {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList( errorArtifactExceptions );
    }

    // ------------------------------------------------------------------------
    // Circular Dependency Exceptions
    // ------------------------------------------------------------------------

    public boolean hasCircularDependencyExceptions()
    {
        return circularDependencyExceptions != null;
    }

    public ArtifactResolutionResult addCircularDependencyException( CyclicDependencyException e )
    {
        circularDependencyExceptions = initList( circularDependencyExceptions );

        circularDependencyExceptions.add( e );

        exceptions = initList( exceptions );

        exceptions.add( e );

        return this;
    }

    public CyclicDependencyException getCircularDependencyException( int i )
    {
        return circularDependencyExceptions.get( i );
    }

    public List<CyclicDependencyException> getCircularDependencyExceptions()
    {
        if ( circularDependencyExceptions == null )
        {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList( circularDependencyExceptions );
    }

    // ------------------------------------------------------------------------
    // Repositories
    // ------------------------------------------------------------------------

    public List<ArtifactRepository> getRepositories()
    {
        if ( repositories == null )
        {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList( repositories );
    }

    public ArtifactResolutionResult setRepositories( final List<ArtifactRepository> repositories )
    {
        this.repositories = repositories;

        return this;
    }

    //
    // Internal
    //

    private <T> List<T> initList( final List<T> l )
    {
        if ( l == null )
        {
            return new ArrayList<>();
        }
        return l;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( artifacts != null )
        {
            int i = 1;
            sb.append( "---------" ).append( LS );
            sb.append( artifacts.size() ).append( LS );
            for ( Artifact a : artifacts )
            {
                sb.append( i ).append( ' ' ).append( a ).append( LS );
                i++;
            }
            sb.append( "---------" );
        }

        return sb.toString();
    }
}
