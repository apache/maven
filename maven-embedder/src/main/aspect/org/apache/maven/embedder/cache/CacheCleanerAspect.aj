package org.apache.maven.embedder.cache;

import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public privileged aspect CacheCleanerAspect
{

    private ModelLineageBuilder MavenEmbedder.modelLineageBuilder;

    private pointcut embedderStarted( MavenEmbedder embedder ):
        execution( * MavenEmbedder.start( .. ) )
        && this( embedder );

    void around( MavenEmbedder embedder )
        throws MavenEmbedderException:
            embedderStarted( embedder )
    {
        proceed( embedder );

        try
        {
            embedder.modelLineageBuilder = (ModelLineageBuilder) embedder.container.lookup( ModelLineageBuilder.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenEmbedderException( "Cannot lookup required component.", e );
        }
    }

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
        request.clearAccumulatedBuildState();

        MavenProjectBuilder projectBuilder = embedder.mavenProjectBuilder;
        ModelLineageBuilder lineageBuilder = embedder.modelLineageBuilder;

        if ( projectBuilder instanceof DefaultMavenProjectBuilder )
        {
            ((DefaultMavenProjectBuilder) projectBuilder).clearProjectCache();
        }

        if ( lineageBuilder instanceof DefaultModelLineageBuilder )
        {
            ((DefaultModelLineageBuilder) lineageBuilder).clearModelAndFileCache();
        }
    }

}
