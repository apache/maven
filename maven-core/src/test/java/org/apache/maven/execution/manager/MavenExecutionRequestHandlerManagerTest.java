package org.apache.maven.execution.manager;

import org.apache.maven.MavenTestCase;
import org.apache.maven.Maven;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenExecutionRequestHandlerManagerTest
    extends MavenTestCase
{
    public void testMaven()
        throws Exception
    {
        MavenExecutionRequestHandlerManager manager = 
            (MavenExecutionRequestHandlerManager) lookup( MavenExecutionRequestHandlerManager.ROLE );

        assertNotNull( manager );

        assertEquals( 3, manager.managedCount() );
    }
}
