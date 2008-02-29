package org.apache.maven.embedder.event;

import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;

public privileged aspect EmbedderEventDispatcherAspect
{

    after( MavenEmbedder embedder ): execution( * MavenEmbedder.start( .. ) ) && this( embedder )
    {
        if ( embedder.dispatcher != null )
        {
            embedder.dispatcher.dispatchStart( MavenEvents.EMBEDDER_LIFECYCLE, "" );
        }
    }

    before( MavenEmbedder embedder ): execution( * MavenEmbedder.stop( .. ) ) && this( embedder )
    {
        if ( embedder.dispatcher != null )
        {
            embedder.dispatcher.dispatchEnd( MavenEvents.EMBEDDER_LIFECYCLE, "" );
        }
    }

    private pointcut eventedMethods( MavenEmbedder embedder ):
        (
          execution( * MavenEmbedder.*( .., MavenExecutionRequest ) )
          || execution( * MavenEmbedder.*( MavenExecutionRequest ) )
          || execution( * MavenEmbedder.*( MavenExecutionRequest, .. ) )
          || execution( * MavenEmbedder.resolve( .. ) )
          || execution( * MavenEmbedder.readProject( .. ) )
          || execution( * MavenEmbedder.verifyPlugin( .. ) )
        )
        && this( embedder );

    before( MavenEmbedder embedder ):
        eventedMethods( embedder )
        && !cflowbelow( eventedMethods( MavenEmbedder ) )
    {
        if ( embedder.dispatcher != null )
        {
            String target = thisJoinPointStaticPart.getSignature().getName();

            embedder.dispatcher.dispatchStart( MavenEvents.EMBEDDER_METHOD, target );
        }
    }

    after( MavenEmbedder embedder ):
        eventedMethods( embedder )
        && !cflowbelow( eventedMethods( MavenEmbedder ) )
    {
        if ( embedder.dispatcher != null )
        {
            String target = thisJoinPointStaticPart.getSignature().getName();

            embedder.dispatcher.dispatchEnd( MavenEvents.EMBEDDER_METHOD, target );
        }
    }

}
