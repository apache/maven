package tests;

import junit.framework.TestCase;

public class DependencyManagementTest 
    extends TestCase
{
	
	public void testWrongTransitiveArtifactIsAvoided()
	{
		assertNull( Thread.currentThread().getContextClassLoader().getResource( "tests/TransitiveComponent1.class" ) );
	}
	
	public void testOtherCArtifactIsAvoided()
	{
		assertNull( Thread.currentThread().getContextClassLoader().getResource( "tests/OtherComponentC.class" ) );
	}
}