package org.apache.maven.artifact.resolver;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;

public class MultipleArtifactsNotFoundException
    extends ArtifactResolutionException
{

    public MultipleArtifactsNotFoundException( Artifact originatingArtifact, List artifacts, List remoteRepositories )
    {
        super( constructMessage( artifacts ), originatingArtifact, remoteRepositories );
    }

    private static String constructMessage( List artifacts )
    {
        StringBuffer buffer = new StringBuffer();

        int size = artifacts.size();

        buffer.append( size ).append( " required artifact" );

        if ( size > 1 )
        {
            buffer.append( "s" );
        }

        buffer.append( " missing:\n" );

        int counter = 0;

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            String message = "\n" + ( ++counter ) + ") " + artifact.getId();

            buffer.append( constructMissingArtifactMessage( message, "  ", artifact.getGroupId(), artifact
                .getArtifactId(), artifact.getVersion(), artifact.getType(), artifact.getDownloadUrl(), artifact
                .getDependencyTrail() ) );

            buffer.append( "\n" );
        }

        buffer.append( "\nfor the artifact:" );
        
        return buffer.toString();
    }

}
