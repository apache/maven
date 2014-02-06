package org.apache.maven.lifecycle.internal.builder;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;

/**
 * This is provisional API and is very likely to change in the near future. If you implement a builder expect it to
 * change. 
 * 
 * @author jvanzyl
 *
 */
public interface Builder
{
    //
    // Be nice to whittle this down to Session, maybe add task segments to the session. The session really is the 
    // the place to store reactor related information.
    //
    public void build( MavenSession session, ReactorContext reactorContext, ProjectBuildList projectBuilds,
                       List<TaskSegment> taskSegments, ReactorBuildStatus reactorBuildStatus )
        throws ExecutionException, InterruptedException;
} 
