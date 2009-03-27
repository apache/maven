package org.apache.maven.mercury;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessorException;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.project.builder.PomInterpolatorTag;
import org.apache.maven.project.builder.PomTransformer;
import org.apache.maven.project.builder.ProjectUri;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.apache.maven.shared.model.ModelContainer;
import org.apache.maven.shared.model.ModelProperty;
import org.apache.maven.shared.model.ModelTransformerContext;
import org.codehaus.plexus.component.annotations.Component;

/**
 *
 * Maven supplied plexus component that implements POM dependency processing for Mercury
 *
 * @author Shane Isbell
 * @version $Id$
 *
 */
@Component( role=DependencyProcessor.class, hint="maven" )
public class MavenDependencyProcessor
    implements DependencyProcessor
{
	/**
	 * Over-ride this method to change how dependencies are obtained
	 */
    public List<ArtifactMetadata> getDependencies( ArtifactMetadata bmd, MetadataReader mdReader, Map system,
                                                        Map user )
        throws MetadataReaderException, DependencyProcessorException
    {
        if ( bmd == null )
        {
            throw new IllegalArgumentException( "bmd: null" );
        }

        if ( mdReader == null )
        {
            throw new IllegalArgumentException( "mdReader: null" );
        }

        List<InterpolatorProperty> interpolatorProperties = createInterpolatorProperties(system, user);

        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        try
        {
            // MavenDomainModel superPom =
            //     new MavenDomainModel(MavenDependencyProcessor.class.getResourceAsStream( "pom-4.0.0.xml" ));
            // domainModels.add(superPom);

            byte[] superBytes = mdReader.readMetadata( bmd );

            if ( superBytes == null || superBytes.length < 1 )
            {
                throw new DependencyProcessorException( "cannot read metadata for " + bmd.getGAV() );
            }

            MavenDomainModel domainModel = new MavenDomainModel( superBytes );
            domainModel.setMostSpecialized(true);
            domainModels.add( domainModel );

            Collection<ModelContainer> activeProfiles = domainModel.getActiveProfileContainers( interpolatorProperties );

            for ( ModelContainer mc : activeProfiles )
            {
                domainModels.add( new MavenDomainModel( transformProfiles( mc.getProperties() ) ) );
            }

            List<DomainModel> parentModels = getParentsOfDomainModel( domainModel, mdReader );

            if ( parentModels == null )
            {
                throw new DependencyProcessorException( "cannot read parent for " + bmd.getGAV() );
            }

            domainModels.addAll( parentModels );
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Failed to create domain model. Message = " + e.getMessage(), e );
        }

        PomTransformer transformer = new PomTransformer( new MavenDomainModelFactory() );
        ModelTransformerContext ctx =
            new ModelTransformerContext( PomTransformer.MODEL_CONTAINER_INFOS );

        try
        {
            MavenDomainModel model =
                ( (MavenDomainModel) ctx.transform( domainModels, transformer, transformer, null,
                                                    interpolatorProperties, null ) );
            return model.getDependencyMetadata();
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Unable to transform model", e );
        }
    }
    
    protected final List<InterpolatorProperty> createInterpolatorProperties(Map system, Map user)
    {
        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.add( new InterpolatorProperty( "${mavenVersion}", "3.0-SNAPSHOT",
                                                              PomInterpolatorTag.EXECUTION_PROPERTIES.name() ) );

        if ( system != null )
        {
            interpolatorProperties.addAll(
                InterpolatorProperty.toInterpolatorProperties( system, PomInterpolatorTag.EXECUTION_PROPERTIES.name() ) );
        }
        if ( user != null )
        {
            interpolatorProperties.addAll(
                InterpolatorProperty.toInterpolatorProperties( user, PomInterpolatorTag.USER_PROPERTIES.name() ) );
        }
        return interpolatorProperties;
    }

    protected final List<DomainModel> getParentsOfDomainModel( MavenDomainModel domainModel, MetadataReader mdReader )
        throws IOException, MetadataReaderException, DependencyProcessorException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        if ( domainModel.hasParent() )
        {
            byte[] b = mdReader.readMetadata( domainModel.getParentMetadata() );

            if ( b == null || b.length < 1 )
            {
                throw new DependencyProcessorException( "cannot read metadata for " + domainModel.getParentMetadata() );
            }

            MavenDomainModel parentDomainModel = new MavenDomainModel( b );
            domainModels.add( parentDomainModel );
            domainModels.addAll( getParentsOfDomainModel( parentDomainModel, mdReader ) );
        }
        return domainModels;
    }

    private static List<ModelProperty> transformProfiles( List<ModelProperty> modelProperties )
    {
        List<ModelProperty> properties = new ArrayList<ModelProperty>();
        for ( ModelProperty mp : modelProperties )
        {
            if ( mp.getUri().startsWith( ProjectUri.Profiles.Profile.xUri )
                && !mp.getUri().equals( ProjectUri.Profiles.Profile.id )
                && !mp.getUri().startsWith( ProjectUri.Profiles.Profile.Activation.xUri ) )
            {
                properties.add( new ModelProperty( mp.getUri().replace( ProjectUri.Profiles.Profile.xUri,
                                                                        ProjectUri.xUri ), mp.getResolvedValue() ) );
            }
        }
        return properties;
    }
}
