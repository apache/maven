package org.apache.maven.acm;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 *
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 */
public class AcmTest
    extends TestCase
{
    public void testAcmSettingTheDefaultSystemId()
        throws Exception
    {
        Acm acm = new Acm( "dev", new PropertiesProvider() );

        assertEquals( "dev", acm.getParameter( "system" ) );

        assertEquals( "test", acm.getParameter( "test", "system" ) );

        assertEquals( "qa", acm.getParameter( "qa", "system" ) );

        assertEquals( "production", acm.getParameter( "production", "system" ) );
    }
}
