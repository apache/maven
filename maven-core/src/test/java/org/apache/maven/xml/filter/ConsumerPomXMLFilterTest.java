package org.apache.maven.xml.filter;

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

import org.junit.Before;
import org.junit.Test;

public class ConsumerPomXMLFilterTest extends AbstractXMLFilterTests
{
    private ConsumerPomXMLFilter filter;
    
    @Before
    public void setup() throws Exception {
        filter = new ConsumerPomXMLFilter();
    }
    
    @Test
    public void testAllFilters() throws Exception {
        String input = "<project>\n"
                     + "  <parent>\n"
                     + "    <groupId>GROUPID</groupId>\n"
                     + "    <artifactId>PARENT</artifactId>\n"
                     + "    <version>VERSION</version>\n"
                     + "    <relativePath>../pom.xml</relativePath>\n"
                     + "  </parent>\n"
                     + "  <artifactId>PROJECT</artifactId>\n"
                     + "  <modules>\n"
                     + "    <module>ab</module>\n"
                     + "    <module>../cd</module>\n"
                     + "  </modules>\n"
                     + "</project>";
        String expected = "<project>\n"
                        + "  <parent>\n"
                        + "    <groupId>GROUPID</groupId>\n"
                        + "    <artifactId>PARENT</artifactId>\n"
                        + "    <version>VERSION</version>\n"
                        + "    <relativePath/>\n"
                        + "  </parent>\n"
                        + "  <artifactId>PROJECT</artifactId>\n"
                        + "</project>";
        String actual = transform( input, filter );
        assertThat( actual ).and( expected ).ignoreWhitespace().areIdentical();
    }

}
