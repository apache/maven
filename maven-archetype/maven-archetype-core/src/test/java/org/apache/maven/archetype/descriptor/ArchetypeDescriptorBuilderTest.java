package org.apache.maven.archetype.descriptor;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            "    <resource>resource0</resource>" +
            "    <resource>resource1</resource>" +
            "  </resources>" +
            "  <testSources>" +
            "    <source>testSource0</source>" +
            "    <source>testSource1</source>" +
            "  </testSources>" +
            "  <testResources>" +
            "    <resource>testResource0</resource>" +
            "    <resource>testResource1</resource>" +
            "  </testResources>" +
            "</archetype>";

        ArchetypeDescriptorBuilder builder = new ArchetypeDescriptorBuilder();

        ArchetypeDescriptor descriptor = (ArchetypeDescriptor) builder.build( new StringReader( xml ) );

        assertEquals( "standard", descriptor.getId() );

        assertEquals( 2, descriptor.getSources().size() );

        assertEquals( "source0", descriptor.getSources().get( 0 ) );

        assertEquals( "source1", descriptor.getSources().get( 1 ) );

        assertEquals( 2, descriptor.getResources().size() );

        assertEquals( "resource0", descriptor.getResources().get( 0 ) );

        assertEquals( "resource1", descriptor.getResources().get( 1 ) );

        assertEquals( 2, descriptor.getTestSources().size() );

        assertEquals( "testSource0", descriptor.getTestSources().get( 0 ) );

        assertEquals( "testSource1", descriptor.getTestSources().get( 1 ) );

        assertEquals( 2, descriptor.getTestResources().size() );

        assertEquals( "testResource0", descriptor.getTestResources().get( 0 ) );

        assertEquals( "testResource1", descriptor.getTestResources().get( 1 ) );
    }
}
