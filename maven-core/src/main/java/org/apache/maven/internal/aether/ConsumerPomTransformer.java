package org.apache.maven.internal.aether;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.feature.Features;
import org.apache.maven.model.building.TransformerContext;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.transform.DeployRequestTransformer;
import org.eclipse.aether.transform.InstallRequestTransformer;

/**
 * Consumer POM install and deploy transformer.
 */
@Named( ConsumerPomTransformer.NAME )
@Singleton
public class ConsumerPomTransformer implements InstallRequestTransformer, DeployRequestTransformer
{
    public static final String NAME = "consumer-pom";

    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public ConsumerPomTransformer( Provider<MavenSession> mavenSessionProvider )
    {
        this.mavenSessionProvider = mavenSessionProvider;
    }

    @Override
    public DeployRequest transformDeployRequest( RepositorySystemSession session,
                                                 DeployRequest deployRequest )
    {
        MavenSession mavenSession = mavenSessionProvider.get();
        if ( !Features.buildConsumer( mavenSession.getUserProperties() ).isActive() )
        {
            return deployRequest;
        }
        return deployRequest.setArtifacts( transformArtifacts( mavenSession, deployRequest.getArtifacts() ) );
    }

    @Override
    public InstallRequest transformInstallRequest( RepositorySystemSession session,
                                                   InstallRequest installRequest )
    {
        MavenSession mavenSession = mavenSessionProvider.get();
        if ( !Features.buildConsumer( mavenSession.getUserProperties() ).isActive() )
        {
            return installRequest;
        }
        return installRequest.setArtifacts( transformArtifacts( mavenSession, installRequest.getArtifacts() ) );
    }

    private Collection<Artifact> transformArtifacts( MavenSession mavenSession, Collection<Artifact> artifacts )
    {
        TransformerContext context = (TransformerContext) mavenSession
                .getRepositorySession().getData().get( TransformerContext.KEY );
        if ( context == null )
        {
            return artifacts;
        }
        ArrayList<Artifact> result = new ArrayList<>( artifacts.size() );
        for ( Artifact artifact : artifacts )
        {
            if ( "pom".equals( artifact.getExtension() ) )
            {
                Path buildOutputDirectory = Paths.get(
                        mavenSession.getCurrentProject().getBuild().getDirectory() );
                Path originalPom = artifact.getFile().toPath();
                Path transformedPom = buildOutputDirectory.resolve( NAME + ".xml" );
                try
                {
                    FileTime originalPomTs = Files.getLastModifiedTime( originalPom );
                    FileTime transformedPomTs = Files.isRegularFile( transformedPom )
                            ? Files.getLastModifiedTime( transformedPom ) : null;

                    if ( !originalPomTs.equals( transformedPomTs ) )
                    {
                        // save it: either does not exist or TS differ
                        Files.createDirectories( transformedPom.getParent() );
                        try ( InputStream inputStream = new ConsumerModelSourceTransformer()
                                .transform( originalPom, context ) )
                        {
                            Files.copy( inputStream, transformedPom );
                            Files.setLastModifiedTime( transformedPom, originalPomTs );
                        }
                    }

                    result.add( artifact.setFile( transformedPom.toFile() ) );
                }
                catch ( XmlPullParserException e )
                {
                    throw new IllegalStateException( e );
                }
                catch ( IOException e )
                {
                    throw new UncheckedIOException( e );
                }
            }
            else
            {
                result.add( artifact );
            }
        }
        return result;
    }
}
