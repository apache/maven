package org.apache.maven.lifecycle.internal;

import junit.framework.TestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.stub.LifecycleTaskSegmentCalculatorStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;

import java.util.List;

/**
 * @author <a href="mailto:kristian@zenior.no">Kristian Rosenvold</a>
 */
public class LifecycleTaskSegmentCalculatorImplTest
    extends TestCase
{
    public void testCalculateProjectBuilds()
        throws Exception
    {
        LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator = getTaskSegmentCalculator();
        BuildListCalculator buildListCalculator = new BuildListCalculator();
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();
        List<TaskSegment> taskSegments = lifecycleTaskSegmentCalculator.calculateTaskSegments( session );

        final ProjectBuildList buildList = buildListCalculator.calculateProjectBuilds( session, taskSegments );
        final ProjectBuildList segments = buildList.getByTaskSegment( taskSegments.get( 0 ) );
        assertEquals( "Stub data contains 3 segments", 3, taskSegments.size() );
        assertEquals( "Stub data contains 6 items", 6, segments.size() );
        final ProjectSegment build = segments.get( 0 );
        assertNotNull( build );
    }

    private static LifecycleTaskSegmentCalculator getTaskSegmentCalculator()
    {
        return new LifecycleTaskSegmentCalculatorStub();
    }


}
