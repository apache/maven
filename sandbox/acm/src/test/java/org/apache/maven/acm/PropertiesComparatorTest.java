package org.apache.maven.acm;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesComparatorTest
    extends TestCase
{
    public void testPropertiesComparator()
        throws Exception
    {
        Properties source = new Properties();

        Properties target = new Properties();

        source.setProperty( "property0", "value0" );

        source.setProperty( "property1", "value1" );

        target.setProperty( "property3", "value3" );

        PropertiesComparator pc = new PropertiesComparator();

        List diff = pc.compare( source, target );

        assertTrue( diff.contains( "property0" ) );

        assertTrue( diff.contains( "property1" ) );                
    }
}
