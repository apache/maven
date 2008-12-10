package org.apache.maven.mercury;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessorException;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.project.builder.ArtifactModelContainerFactory;
import org.apache.maven.project.builder.IdModelContainerFactory;
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
public final class MavenDependencyProcessor
    implements DependencyProcessor
{
    public List<ArtifactBasicMetadata> getDependencies( ArtifactBasicMetadata bmd, MetadataReader mdReader, Map system,
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

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        interpolatorProperties.add( new InterpolatorProperty( "${mavenVersion}", "3.0-SNAPSHOT",
                                                              PomInterpolatorTag.SYSTEM_PROPERTIES.name() ) );

        if ( system != null )
        {
            interpolatorProperties.addAll(
                InterpolatorProperty.toInterpolatorProperties( system, PomInterpolatorTag.SYSTEM_PROPERTIES.name() ) );
        }
        if ( user != null )
        {
            interpolatorProperties.addAll(
                InterpolatorProperty.toInterpolatorProperties( user, PomInterpolatorTag.USER_PROPERTIES.name() ) );
        }

        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        try
        {
            // MavenDomainModel superPom =
            //     new MavenDomainModel(MavenDependencyProcessor.class.getResourceAsStream( "pom-4.0.0.xml" ));
            // domainModels.add(superPom);

            byte[] superBytes = mdReader.readMetadata( bmd );

            if ( superBytes == null || superBytes.length < 1 )
                throw new DependencyProcessorException( "cannot read metadata for " + bmd.getGAV() );

            MavenDomainModel domainModel = new MavenDomainModel( superBytes );
            domainModels.add( domainModel );

            Collection<ModelContainer> activeProfiles = domainModel.getActiveProfileContainers( interpolatorProperties );

            for ( ModelContainer mc : activeProfiles )
            {
                domainModels.add( new MavenDomainModel( transformProfiles( mc.getProperties() ) ) );
            }

            domainModels.addAll( getParentsOfDomainModel( domainModel, mdReader ) );
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Failed to create domain model. Message = " + e.getMessage() );
        }

        PomTransformer transformer = new PomTransformer( new MavenDomainModelFactory() );
        ModelTransformerContext ctx =
            new ModelTransformerContext( Arrays.asList( new ArtifactModelContainerFactory(),
                                                        new IdModelContainerFactory() ) );

        try
        {
            MavenDomainModel model =
                ( (MavenDomainModel) ctx.transform( domainModels, transformer, transformer, null,
                                                    interpolatorProperties, null ) );
            return model.getDependencyMetadata();
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Unable to transform model" );
        }
    }

    private static List<DomainModel> getParentsOfDomainModel( MavenDomainModel domainModel, MetadataReader mdReader )
        throws IOException, MetadataReaderException
    {
        List<DomainModel> domainModels = new ArrayList<DomainModel>();
        if ( domainModel.hasParent() )
        {
            MavenDomainModel parentDomainModel =
                new MavenDomainModel( mdReader.readMetadata( domainModel.getParentMetadata() ) );
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
