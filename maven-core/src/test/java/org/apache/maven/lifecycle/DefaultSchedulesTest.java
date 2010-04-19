package org.apache.maven.lifecycle;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.List;

public class DefaultSchedulesTest
    extends PlexusTestCase
    
{
    @Requirement
    DefaultSchedules defaultSchedules;

    public DefaultSchedulesTest()
    {
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        defaultSchedules = ( lookup( DefaultSchedules.class ) );
    }

    public void testScheduling()
        throws Exception
    {
        final List<Scheduling> schedulings = defaultSchedules.getSchedules();
        DefaultLifecyclesTest.assertNotNull( schedulings );
        DefaultLifecyclesTest.assertTrue( schedulings.size() > 0 );
        Scheduling first = schedulings.get( 0 );
        DefaultLifecyclesTest.assertNotNull( first.getLifecycle() );
        final List<Schedule> schedules = first.getSchedules();
        DefaultLifecyclesTest.assertNotNull( schedules );
        // Ok so if we ever change the first schedule this test will have to change
        Schedule firstSchedule = schedules.get( 0 );
        DefaultLifecyclesTest.assertEquals( "test", firstSchedule.getPhase() );
        DefaultLifecyclesTest.assertTrue( "Should be parllel", firstSchedule.isParallel() );

    }
}