package org.apache.maven.project.aspect;

import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.ProjectSorter;

import java.util.List;

public aspect ProjectCollisionReporterAspect
    extends AbstractProjectErrorReporterAspect
{

    /**
     * <b>Call Stack:</b>
     * <br/>
     * <pre>
     * MavenEmbedder.execute(MavenExecutionRequest)
     * MavenEmbedder.readProjectWithDependencies(MavenExecutionRequest)
     * --&gt; DefaultMaven.execute(MavenExecutionRequest)
     *        --&gt; DefaultMaven.createReactorManager(MavenExecutionRequest, MavenExecutionResult)
     *               --&gt; new ReactorManager(List, String)
     *                      --&gt; new ProjectSorter(List)
     * &lt;----------------------- DuplicateProjectException
     * </pre>
     */
    after( List allProjectInstances ) throwing( DuplicateProjectException err ):
        execution( ProjectSorter.new( List ) )
        && args( allProjectInstances )
    {
        getReporter().reportProjectCollision( allProjectInstances, err );
    }

}
