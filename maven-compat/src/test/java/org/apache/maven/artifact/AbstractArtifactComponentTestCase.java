package org.apache.maven.artifact;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.testing.PlexusTest;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.legacy.repository.ArtifactRepositoryFactory;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.junit.jupiter.api.BeforeEach;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 */
@PlexusTest
public abstract class AbstractArtifactComponentTestCase //extends PlexusTestCase
{
    @Inject
    protected ArtifactFactory artifactFactory;

    @Inject
    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    @Inject
    LegacySupport legacySupport;

    @Inject @Named( "default" )
    ArtifactRepositoryLayout repoLayout;

    @Inject
    PlexusContainer container;

    public PlexusContainer getContainer() {
        return container;
    }

    @BeforeEach
    public void setUp()
        throws Exception
    {
        RepositorySystemSession repoSession = initRepoSession();
        MavenSession session = new MavenSession( getContainer(), repoSession, new DefaultMavenExecutionRequest(),
                                                 new DefaultMavenExecutionResult() );

        legacySupport.setSession( session );
    }

    protected abstract String component();

    /**
     * Return an existing file, not a directory - causes creation to fail.
     *
     * @throws Exception
     */
    protected ArtifactRepository badLocalRepository()
        throws Exception
    {
        String path = "target/test-repositories/" + component() + "/bad-local-repository";

        File f = new File( getBasedir(), path );

        f.createNewFile();

        return artifactRepositoryFactory.createArtifactRepository( "test", "file://" + f.getPath(), repoLayout, null,
                                                                   null );
    }

    protected String getRepositoryLayout()
    {
        return "default";
    }

    protected ArtifactRepository localRepository()
        throws Exception
    {
        String path = "target/test-repositories/" + component() + "/local-repository";

        File f = new File( getBasedir(), path );

        return artifactRepositoryFactory.createArtifactRepository( "local", "file://" + f.getPath(), repoLayout, null,
                                                                   null );
    }

    protected ArtifactRepository remoteRepository()
        throws Exception
    {
        String path = "target/test-repositories/" + component() + "/remote-repository";

        File f = new File( getBasedir(), path );

        return artifactRepositoryFactory.createArtifactRepository( "test", "file://" + f.getPath(), repoLayout,
                                                                   new ArtifactRepositoryPolicy(),
                                                                   new ArtifactRepositoryPolicy() );
    }

    protected ArtifactRepository badRemoteRepository()
        throws Exception
    {
        return artifactRepositoryFactory.createArtifactRepository( "test", "http://foo.bar/repository", repoLayout,
                                                                   null, null );
    }

    protected void assertRemoteArtifactPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoteRepo = remoteRepository();

        String path = remoteRepo.pathOf( artifact );

        File file = new File( remoteRepo.getBasedir(), path );

        assertTrue( file.exists(), "Remote artifact " + file + " should be present." );
    }

    protected void assertLocalArtifactPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository localRepo = localRepository();

        String path = localRepo.pathOf( artifact );

        File file = new File( localRepo.getBasedir(), path );

        assertTrue( file.exists(), "Local artifact " + file + " should be present." );
    }

    protected void assertRemoteArtifactNotPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository remoteRepo = remoteRepository();

        String path = remoteRepo.pathOf( artifact );

        File file = new File( remoteRepo.getBasedir(), path );

        assertFalse( file.exists(), "Remote artifact " + file + " should not be present." );
    }

    protected void assertLocalArtifactNotPresent( Artifact artifact )
        throws Exception
    {
        ArtifactRepository localRepo = localRepository();

        String path = localRepo.pathOf( artifact );

        File file = new File( localRepo.getBasedir(), path );

        assertFalse( file.exists(), "Local artifact " + file + " should not be present." );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected List<ArtifactRepository> remoteRepositories()
        throws Exception
    {
        List<ArtifactRepository> remoteRepositories = new ArrayList<>();

        remoteRepositories.add( remoteRepository() );

        return remoteRepositories;
    }

    // ----------------------------------------------------------------------
    // Test artifact generation for unit tests
    // ----------------------------------------------------------------------

    protected Artifact createLocalArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, localRepository() );

        return artifact;
    }

    protected Artifact createRemoteArtifact( String artifactId, String version )
        throws Exception
    {
        Artifact artifact = createArtifact( artifactId, version );

        createArtifact( artifact, remoteRepository() );

        return artifact;
    }

    protected void createLocalArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, localRepository() );
    }

    protected void createRemoteArtifact( Artifact artifact )
        throws Exception
    {
        createArtifact( artifact, remoteRepository() );
    }

    protected void createArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = repository.pathOf( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( !artifactFile.getParentFile().exists() )
        {
            artifactFile.getParentFile().mkdirs();
        }
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( artifactFile ), StandardCharsets.ISO_8859_1) )
        {
            writer.write( artifact.getId() );
        }

        MessageDigest md = MessageDigest.getInstance( "MD5" );
        md.update( artifact.getId().getBytes() );
        byte[] digest = md.digest();

        String md5path = repository.pathOf( artifact ) + ".md5";
        File md5artifactFile = new File( repository.getBasedir(), md5path );
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( md5artifactFile ), StandardCharsets.ISO_8859_1) )
        {
            writer.append( printHexBinary( digest ) );
        }
    }

    protected Artifact createArtifact( String artifactId, String version )
        throws Exception
    {
        return createArtifact( artifactId, version, "jar" );
    }

    protected Artifact createArtifact( String artifactId, String version, String type )
        throws Exception
    {
        return createArtifact( "org.apache.maven", artifactId, version, type );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        Artifact a = artifactFactory.createBuildArtifact( groupId, artifactId, version, type );

        return a;
    }

    protected void deleteLocalArtifact( Artifact artifact )
        throws Exception
    {
        deleteArtifact( artifact, localRepository() );
    }

    protected void deleteArtifact( Artifact artifact, ArtifactRepository repository )
        throws Exception
    {
        String path = repository.pathOf( artifact );

        File artifactFile = new File( repository.getBasedir(), path );

        if ( artifactFile.exists() )
        {
            if ( !artifactFile.delete() )
            {
                throw new IOException( "Failure while attempting to delete artifact " + artifactFile );
            }
        }
    }

    protected RepositorySystemSession initRepoSession()
        throws Exception
    {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setArtifactDescriptorPolicy( new SimpleArtifactDescriptorPolicy( true, true ) );
        DependencyTraverser depTraverser = new FatArtifactTraverser();
        session.setDependencyTraverser( depTraverser );

        DependencyManager depManager = new ClassicDependencyManager();
        session.setDependencyManager( depManager );

        DependencySelector depFilter = new AndDependencySelector( new ScopeDependencySelector( "test", "provided" ),
                                                                  new OptionalDependencySelector(),
                                                                  new ExclusionDependencySelector() );
        session.setDependencySelector( depFilter );

        DependencyGraphTransformer transformer =
            new ConflictResolver( new NearestVersionSelector(), new JavaScopeSelector(),
                                  new SimpleOptionalitySelector(), new JavaScopeDeriver() );
        transformer = new ChainedDependencyGraphTransformer( transformer, new JavaDependencyContextRefiner() );
        session.setDependencyGraphTransformer( transformer );

        LocalRepository localRepo = new LocalRepository( localRepository().getBasedir() );
        session.setLocalRepositoryManager(
            new SimpleLocalRepositoryManagerFactory().newInstance( session, localRepo ) );

        return session;
    }

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    private static final String printHexBinary( byte[] data )
    {
        StringBuilder r = new StringBuilder( data.length * 2 );
        for ( byte b : data )
        {
            r.append( hexCode[( b >> 4 ) & 0xF] );
            r.append( hexCode[( b & 0xF )] );
        }
        return r.toString();
    }

}
