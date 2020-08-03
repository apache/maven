package com.example;

import java.lang.IllegalStateException;
import java.util.Objects;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component( role = AbstractMavenLifecycleParticipant.class )
public class BuildExtensionUsingGraphPackage extends AbstractMavenLifecycleParticipant
{

    @Requirement( hint = GraphBuilder.HINT )
    private GraphBuilder graphBuilder;

    @Override
    public void afterProjectsRead( final MavenSession session ) throws MavenExecutionException
    {
        Objects.requireNonNull( graphBuilder, "graphBuilder should be available in build extension" );

        Result<? extends ProjectDependencyGraph> graphResult = graphBuilder.build( session );
        Objects.requireNonNull( graphResult, "graphResult should have been built" );

        for ( ModelProblem problem : graphResult.getProblems() )
        {
            if ( problem.getSeverity() == ModelProblem.Severity.WARNING )
            {
                throw new IllegalStateException( "unexpected WARNING found: " + problem );
            }
            else
            {
                throw new IllegalStateException( "unexpected Problem found: " + problem );
            }
        }
    }
}
