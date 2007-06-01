package org.apache.maven.profiles.activation;

import junit.framework.TestCase;


/** Test case for the {@link JdkPrefixProfileActivator}.
 */
public class JdkPrefixProfileActivatorTest extends TestCase
{
	/** Test for the basic match "equals".
	 */
	public void testBasicMatch()
	{
		JdkPrefixProfileActivator activator = new JdkPrefixProfileActivator();
		assertTrue( activator.isActive( "1.5", "1.5" ) );
		assertFalse( activator.isActive( "1.4", "1.5" ) );
		assertFalse( activator.isActive( "1.6", "1.5" ) );
		assertTrue( activator.isActive( "1.5.0_06", "1.5" ) );
		assertFalse( activator.isActive( "1.5.0_06", "1.5.1" ) );
		assertFalse( activator.isActive( "1.5", "!1.5" ) );
		assertTrue( activator.isActive( "1.4", "!1.5" ) );
		assertTrue( activator.isActive( "1.6", "!1.5" ) );
		assertFalse( activator.isActive( "1.5.0_06", "!1.5" ) );
		assertTrue( activator.isActive( "1.5.0_06", "!1.5.1" ) );
	}

	/** Test for the match "greather than or equal".
	 */
	public void testGreatherThanOrEqualMatch()
	{
		JdkPrefixProfileActivator activator = new JdkPrefixProfileActivator();
		assertTrue( activator.isActive( "1.5.0_06", "1.5+" ) );
		assertFalse( activator.isActive( "1.5.0_06", "!1.5+" ) );
		assertTrue( activator.isActive( "1.5.0_06", "1.4+" ) );
		assertFalse( activator.isActive( "1.5.0_06", "!1.4+" ) );
		assertFalse( activator.isActive( "1.5.0_06", "1.6+" ) );
		assertTrue( activator.isActive( "1.5.0_06", "!1.6+" ) );
		assertTrue( activator.isActive( "1.5.0_06", "1.5.0.0+" ) );
		assertFalse( activator.isActive( "1.5.0_06", "!1.5.0.0+" ) );
		assertTrue( activator.isActive( "1.5.0_06", "1.5.0.6+" ) );
		assertFalse( activator.isActive( "1.5.0_06", "!1.5.0.6+" ) );
		assertFalse( activator.isActive( "1.5.0_06", "1.5.0.7+" ) );
		assertTrue( activator.isActive( "1.5.0_06", "!1.5.0.7+" ) );
	}
}
