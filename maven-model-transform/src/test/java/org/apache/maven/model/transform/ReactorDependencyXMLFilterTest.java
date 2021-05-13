package org.apache.maven.model.transform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.xmlunit.assertj.XmlAssert.assertThat;

import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class ReactorDependencyXMLFilterTest
    extends AbstractXMLFilterTests
{
    @Override
    protected ReactorDependencyXMLFilter getFilter( Consumer<LexicalHandler> lexicalHandlerConsumer )
        throws TransformerException, SAXException, ParserConfigurationException
    {
        ReactorDependencyXMLFilter filter = new ReactorDependencyXMLFilter( (g, a) -> "1.0.0" );
        lexicalHandlerConsumer.accept( filter );
        return filter;
    }

    @Test
    public void testDefaultDependency()
        throws Exception
    {
        String input = "<dependency>"
            + "<groupId>GROUPID</groupId>"
            + "<artifactId>ARTIFACTID</artifactId>"
            + "<version>VERSION</version>"
            + "</dependency>";
        String expected = input;

        String actual = transform( input );

        assertThat( actual ).isEqualTo( expected );
    }

    @Test
    public void testManagedDependency()
        throws Exception
    {
        ReactorDependencyXMLFilter filter = new ReactorDependencyXMLFilter( (g, a) -> null );

        String input = "<dependency>"
            + "<groupId>GROUPID</groupId>"
            + "<artifactId>ARTIFACTID</artifactId>"
            + "</dependency>";
        String expected = input;

        String actual = transform( input, filter );

        assertThat( actual ).isEqualTo( expected );
    }

    @Test
    public void testReactorDependency()
        throws Exception
    {
        String input = "<dependency>"
                        + "<groupId>GROUPID</groupId>"
                        + "<artifactId>ARTIFACTID</artifactId>"
                        + "</dependency>";
        String expected = "<dependency>"
                        + "<groupId>GROUPID</groupId>"
                        + "<artifactId>ARTIFACTID</artifactId>"
                        + "<version>1.0.0</version>"
                        + "</dependency>";

        String actual = transform( input );

        assertThat( actual ).isEqualTo( expected );
    }

    @Test
    public void testReactorDependencyLF()
        throws Exception
    {
        String input = "<dependency>\n"
                        + "  <groupId>GROUPID</groupId>\n"
                        + "  <artifactId>ARTIFACTID</artifactId>\n"
                        + "  <!-- include version here --> "
                        + "</dependency>";
        String expected = "<dependency>\n"
                        + "  <groupId>GROUPID</groupId>\n"
                        + "  <artifactId>ARTIFACTID</artifactId>\n"
                        + "  <!-- include version here -->\n"
                        + "  <version>1.0.0</version>\n"
                        + "</dependency>";

        String actual = transform( input );

        assertThat( actual ).and( expected ).ignoreWhitespace().areIdentical();
    }

    @Test
    public void multipleDependencies()
        throws Exception
    {
        String input = "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>tests.project</groupId>\n" +
            "    <artifactId>duplicate-plugin-defs-merged</artifactId>\n" +
            "    <version>1</version>\n" +
            "    <build>\n" +
            "      <plugins>\n" +
            "        <plugin>\n" +
            "          <artifactId>maven-compiler-plugin</artifactId>\n" +
            "          <dependencies>\n" +
            "            <dependency>\n" +
            "              <groupId>group</groupId>\n" +
            "              <artifactId>first</artifactId>\n" +
            "              <version>1</version>\n" +
            "            </dependency>\n" +
            "          </dependencies>\n" +
            "        </plugin>\n" +
            "        <plugin>\n" +
            "          <artifactId>maven-compiler-plugin</artifactId>\n" +
            "          <dependencies>\n" +
            "            <dependency>\n" +
            "              <groupId>group</groupId>\n" +
            "              <artifactId>second</artifactId>\n" +
            "              <version>1</version>\n" +
            "            </dependency>\n" +
            "          </dependencies>\n" +
            "        </plugin>\n" +
            "      </plugins>\n" +
            "    </build>\n" +
            "</project>";
        String expected = input;

        String actual = transform( input );

        assertThat( actual ).and( expected ).areIdentical();
    }
}
