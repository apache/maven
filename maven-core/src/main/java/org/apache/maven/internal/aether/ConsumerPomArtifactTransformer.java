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
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.TransformerContext;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.transform.ArtifactTransformer;

@Named( ConsumerPomArtifactTransformer.NAME )
@Singleton
public class ConsumerPomArtifactTransformer implements ArtifactTransformer
{
    public static final String NAME = "consumer-pom";

    private final Provider<MavenSession> mavenSessionProvider;

    @Inject
    public ConsumerPomArtifactTransformer( Provider<MavenSession> mavenSessionProvider )
    {
        this.mavenSessionProvider = mavenSessionProvider;
    }

    @Override
    public Collection<Artifact> transformArtifact( Artifact artifact )
    {
        MavenSession mavenSession = mavenSessionProvider.get();
        TransformerContext context = (TransformerContext) mavenSession
                .getRepositorySession().getData().get( TransformerContext.KEY );
        if ( "pom".equals( artifact.getExtension() ) && context != null )
        {
            // TODO: fix this, make it target rather
            Path buildOutputDirectory = Paths.get(
                    mavenSession.getCurrentProject().getBuild().getTestOutputDirectory() );
            Path transformedPom = buildOutputDirectory.resolve( NAME + ".xml" );
            try
            {
                Files.createDirectories( transformedPom.getParent() );
                try ( InputStream inputStream = new ConsumerModelSourceTransformer()
                        .transform( artifact.getFile().toPath(), context ) )
                {
                    // TODO: detect changes, do nothing if all ok
                    Files.copy( inputStream, transformedPom, StandardCopyOption.REPLACE_EXISTING );
                }
                return Collections.singleton( artifact.setFile( transformedPom.toFile() ) );
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
            return Collections.singleton( artifact );
        }
    }
}
