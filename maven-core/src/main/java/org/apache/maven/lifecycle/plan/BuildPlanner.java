package org.apache.maven.lifecycle.plan;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleLoaderException;
import org.apache.maven.lifecycle.LifecycleSpecificationException;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * Responsible for creating a plan of execution for a given project and list of tasks. This build plan consists of
 * MojoBinding instances that carry all the information necessary to execute a mojo, including configuration from the
 * POM and other sources. NOTE: the build plan may be constructed of a main lifecycle binding-set, plus any number of
 * lifecycle modifiers and direct-invocation modifiers, to handle cases of forked execution.
 *
 * @author jdcasey
 *
 */
public interface BuildPlanner
{

    /**
     * Orchestrates construction of the build plan which will be used by the user of LifecycleExecutor.
     */
    BuildPlan constructBuildPlan( List tasks,
                                  MavenProject project,
                                  MavenSession session,
                                  boolean allowUnbindableMojos )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException;

    void constructInitialProjectBuildPlans( MavenSession session )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException;

    BuildPlan constructInitialProjectBuildPlan( MavenProject project,
                                                MavenSession session )
        throws LifecycleLoaderException, LifecycleSpecificationException, LifecyclePlannerException;
}
