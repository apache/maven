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
