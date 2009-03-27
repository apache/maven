package org.apache.maven.mercury;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessorException;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.project.processor.ProcessorContext;
import org.apache.maven.shared.model.DomainModel;
import org.apache.maven.shared.model.InterpolatorProperty;
import org.codehaus.plexus.component.annotations.Component;

@Component( role=DependencyProcessor.class, hint="maven2" )
public class MavenDependencyProcessor2
	extends MavenDependencyProcessor
{
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
            byte[] superBytes = mdReader.readMetadata( bmd );

            if ( superBytes == null || superBytes.length < 1 )
            {
                throw new DependencyProcessorException( "cannot read metadata for " + bmd.getGAV() );
            }

            MavenDomainModel domainModel = new MavenDomainModel( superBytes );
            domainModel.setMostSpecialized(true);
            domainModels.add( domainModel );

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

        try
        {
        	MavenDomainModel model = new MavenDomainModel(ProcessorContext.build(domainModels, interpolatorProperties));
            return model.getDependencyMetadata();
        }
        catch ( IOException e )
        {
            throw new MetadataReaderException( "Unable to transform model", e );
        }
    }
}
