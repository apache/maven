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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.MavenRepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Component(role=ArtifactResolver.class, hint="test")
public class TestArtifactResolver
    extends DefaultArtifactResolver
{
    @Requirement
    private PlexusContainer container;

    @Requirement
    private MavenRepositorySystem repositorySystem;
        
    static class Source
        implements ArtifactMetadataSource
    {
        private final PlexusContainer container;

        private MavenRepositorySystem repositorySystem;
        
        public Source( MavenRepositorySystem repositorySystem, PlexusContainer container )
        {
            this.repositorySystem = repositorySystem;
            this.container = container;
        }

        public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                         List<ArtifactRepository> remoteRepositories )
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
                artifactRepositories = repositorySystem.buildArtifactRepositories( model.getRepositories() );
            }
            catch ( InvalidRepositoryException e )
            {
                throw new ArtifactMetadataRetrievalException( e );
            }

            return new ResolutionGroup( artifact, artifacts, artifactRepositories );
        }

        public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                                                List<ArtifactRepository> remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
        }

        public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
                                                                                        Artifact artifact,
                                                                                        ArtifactRepository localRepository,
                                                                                        ArtifactRepository remoteRepository )
            throws ArtifactMetadataRetrievalException
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
                
                Artifact artifact = repositorySystem.createDependencyArtifact( d.getGroupId(), 
                                                                               d.getArtifactId(),
                                                                               d.getVersion(), 
                                                                               d.getType(),
                                                                               d.getClassifier(), 
                                                                               scope,
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
                                                   List<ArtifactRepository> remoteRepositories )
            throws ArtifactMetadataRetrievalException
        {
            return artifact;
        }
    }

    public Source source()
    {
        return new Source( repositorySystem, container );
    }
}