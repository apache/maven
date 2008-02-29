package org.apache.maven.embedder.cache;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.embedder.MavenEmbedder;

public privileged aspect CacheCleanerAspect
{

    private pointcut requestAsLastParam( MavenExecutionRequest request, MavenEmbedder embedder ):
        execution( * MavenEmbedder.*( .., MavenExecutionRequest ) )
        && args( .., request )
        && this( embedder );

    private pointcut requestAsOnlyParam( MavenExecutionRequest request, MavenEmbedder embedder ):
        execution( * MavenEmbedder.*( MavenExecutionRequest ) )
        && args( request )
        && this( embedder );

    after( MavenExecutionRequest request, MavenEmbedder embedder ): requestAsLastParam( request, embedder )
    {
        cleanup( request, embedder );
    }

    after( MavenExecutionRequest request, MavenEmbedder embedder ): requestAsOnlyParam( request, embedder )
    {
        cleanup( request, embedder );
    }

    private void cleanup( MavenExecutionRequest request, MavenEmbedder embedder )
    {
        // TODO: Add this to the eventing-control mechanism that the workspace uses now,
        // once we can accommodate context in the event method calls.
        request.clearAccumulatedBuildState();
    }

}
