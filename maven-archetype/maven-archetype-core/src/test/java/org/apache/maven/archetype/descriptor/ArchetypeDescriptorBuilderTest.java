package org.apache.maven.archetype.descriptor;

import junit.framework.TestCase;

import java.io.StringReader;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ArchetypeDescriptorBuilderTest
    extends TestCase
{
    public void testBuilder()
        throws Exception
    {
        String xml =
            "<archetype>" +
            "  <id>standard</id>" +
            "  <sources>" +
            "    <source>source0</source>" +
            "    <source>source1</source>" +
            "  </sources>" +
            "  <resources>" +
            "    <source>source0</source>" +
            "    <source>source1</source>" +
            "  </resources>" +
            "  <testSources>" +
            "    <source>source0</source>" +
            "    <source>source1</source>" +
            "  </testSources>" +
            "  <testResources>" +
            "    <source>source0</source>" +
            "    <source>source1</source>" +
            "  </testResources>" +
            "</archetype>";

        ArchetypeDescriptorBuilder builder = new ArchetypeDescriptorBuilder();

        ArchetypeDescriptor descriptor = (ArchetypeDescriptor) builder.build( new StringReader( xml ) );

        assertEquals( "standard", descriptor.getId() );

        assertEquals( 2, descriptor.getSources().size() );

        assertEquals( 2, descriptor.getResources().size() );

        assertEquals( 2, descriptor.getTestSources().size() );

        assertEquals( 2, descriptor.getTestResources().size() );
    }
}
