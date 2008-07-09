package org.apache.maven.project;

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
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TestArtifactResolver
    extends DefaultArtifactResolver
    implements Contextualizable
{
    public static final String ROLE = TestArtifactResolver.class.getName();

    private ArtifactRepositoryFactory repositoryFactory;

    private PlexusContainer container;

    static class Source
        implements ArtifactMetadataSource
    {
        private ArtifactFactory artifactFactory;

        private final ArtifactRepositoryFactory repositoryFactory;

        private final PlexusContainer container;

        public Source( ArtifactFactory artifactFactory, ArtifactRepositoryFactory repositoryFactory,
                       PlexusContainer container )
        {
            this.artifactFactory = artifactFactory;
            this.repositoryFactory = repositoryFactory;
            this.container = container;
        }

        public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                         List remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            Model model = null;
            InputStreamReader r = null;
            try
            {
                String scope = artifact.getArtifactId().substring( "scope-".length() );
                if ( "maven-test".equals( artifact.getGroupId() ) )
                {
                    String name = "/projects/scope/transitive-" + scope + "-dep.xml";
                    r = new InputStreamReader( getClass().getResourceAsStream( name ) );
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    model = reader.read( r );
                }
                else
                {
                    model = new Model();
                }
                model.setGroupId( artifact.getGroupId() );
                model.setArtifactId( artifact.getArtifactId() );
            }
            catch ( IOException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }
            finally
            {
                IOUtil.close( r );
            }

            Set artifacts;
            try
            {
                artifacts = createArtifacts( model.getDependencies(), artifact.getScope() );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }

            List artifactRepositories;
            try
            {
                artifactRepositories =
                    ProjectUtils.buildArtifactRepositories( model.getRepositories(), repositoryFactory, container );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }

            return new ResolutionGroup( artifact, artifacts, artifactRepositories );
        }

        public List retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                               List remoteRepositories )
        {
            throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
        }

        protected Set createArtifacts( List dependencies, String inheritedScope )
            throws InvalidVersionSpecificationException
        {
            Set projectArtifacts = new HashSet();

            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                String scope = d.getScope();

                if ( StringUtils.isEmpty( scope ) )
                {
                    scope = Artifact.SCOPE_COMPILE;

                    d.setScope( scope );
                }

                VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                              versionRange, d.getType(),
                                                                              d.getClassifier(), scope,
                                                                              inheritedScope );
                if ( artifact != null )
                {
                    projectArtifacts.add( artifact );
                }
            }

            return projectArtifacts;
        }

        public Artifact retrieveRelocatedArtifact( Artifact artifact,
                                                   ArtifactRepository localRepository,
                                                   List remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            return artifact;
        }
    }

    public Source source()
    {
        return new Source( artifactFactory, repositoryFactory, container );
    }

    /**
     * @noinspection RefusedBequest
     */
    public void resolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        artifact.setFile( new File( "dummy" ) );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         ArtifactRepository localRepository, List remoteRepositories,
                                                         ArtifactMetadataSource source, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return super.resolveTransitively( artifacts, originatingArtifact, localRepository, remoteRepositories,
                                          new Source( artifactFactory, repositoryFactory, container ), filter );
    }

    public ArtifactResolutionResult resolveTransitively( Set artifacts, Artifact originatingArtifact,
                                                         List remoteRepositories, ArtifactRepository localRepository,
                                                         ArtifactMetadataSource source )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return super.resolveTransitively( artifacts, originatingArtifact, remoteRepositories, localRepository,
                                          new Source( artifactFactory, repositoryFactory, container ) );
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}