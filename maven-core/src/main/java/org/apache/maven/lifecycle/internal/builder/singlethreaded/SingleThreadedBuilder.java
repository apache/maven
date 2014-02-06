package org.apache.maven.lifecycle.internal.builder.singlethreaded;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.builder.Builder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component( role = Builder.class, hint = "singlethreaded" )
public class SingleThreadedBuilder
    implements Builder
{
    @Requirement
    private LifecycleModuleBuilder lifecycleModuleBuilder;

    public void build( MavenSession session, ReactorContext reactorContext, ProjectBuildList projectBuilds,
                       List<TaskSegment> taskSegments, ReactorBuildStatus reactorBuildStatus )
    {
        for ( TaskSegment taskSegment : taskSegments )
        {
            for ( ProjectSegment projectBuild : projectBuilds.getByTaskSegment( taskSegment ) )
            {
                try
                {
                    lifecycleModuleBuilder.buildProject( session, reactorContext, projectBuild.getProject(),
                                                         taskSegment );
                    if ( reactorBuildStatus.isHalted() )
                    {
                        break;
                    }
                }
                catch ( Exception e )
                {
                    break; // Why are we just ignoring this exception? Are exceptions are being used for flow control
                }
            }
        }
    }
}
