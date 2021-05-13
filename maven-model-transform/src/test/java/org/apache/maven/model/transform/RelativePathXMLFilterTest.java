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

import org.junit.jupiter.api.Test;

public class RelativePathXMLFilterTest
    extends AbstractXMLFilterTests
{
    @Override
    protected RelativePathXMLFilter getFilter()
    {
        return new RelativePathXMLFilter();
    }

    @Test
    public void testRelativePath()
        throws Exception
    {
        String input = "<project>\n"
                        + "  <parent>\n"
                        + "    <groupId>GROUPID</groupId>\n"
                        + "    <artifactId>PARENT</artifactId>\n"
                        + "    <version>VERSION</version>\n"
                        + "    <relativePath>../pom.xml</relativePath>\n"
                        + "  </parent>\n"
                        + "  <artifactId>PROJECT</artifactId>\n"
                        + "</project>";
           String expected = "<project>\n"
                           + "  <parent>\n"
                           + "    <groupId>GROUPID</groupId>\n"
                           + "    <artifactId>PARENT</artifactId>\n"
                           + "    <version>VERSION</version>\n"
                           + "  </parent>\n"
                           + "  <artifactId>PROJECT</artifactId>\n"
                           + "</project>";
           String actual = transform( input );
           assertThat( actual ).and( expected ).areIdentical();
    }

    @Test
    public void testRelativePathNS()
        throws Exception
    {
        String input = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                        + "  <parent>\n"
                        + "    <groupId>GROUPID</groupId>\n"
                        + "    <artifactId>PARENT</artifactId>\n"
                        + "    <version>VERSION</version>\n"
                        + "    <relativePath>../pom.xml</relativePath>\n"
                        + "  </parent>\n"
                        + "  <artifactId>PROJECT</artifactId>\n"
                        + "</project>";
           String expected = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
               "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                           + "  <parent>\n"
                           + "    <groupId>GROUPID</groupId>\n"
                           + "    <artifactId>PARENT</artifactId>\n"
                           + "    <version>VERSION</version>\n"
                           + "  </parent>\n"
                           + "  <artifactId>PROJECT</artifactId>\n"
                           + "</project>";
           String actual = transform( input );
           assertThat( actual ).and( expected ).areIdentical();
    }

    @Test
    public void testRelativePathPasNS()
        throws Exception
    {
        String input = "<p:project xmlns:p=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                        + "  <p:parent>\n"
                        + "    <p:groupId>GROUPID</p:groupId>\n"
                        + "    <p:artifactId>PARENT</p:artifactId>\n"
                        + "    <p:version>VERSION</p:version>\n"
                        + "    <p:relativePath>../pom.xml</p:relativePath>\n"
                        + "  </p:parent>\n"
                        + "  <p:artifactId>PROJECT</p:artifactId>\n"
                        + "</p:project>";
           String expected = "<p:project xmlns:p=\"http://maven.apache.org/POM/4.0.0\"\n" +
               "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                           + "  <p:parent>\n"
                           + "    <p:groupId>GROUPID</p:groupId>\n"
                           + "    <p:artifactId>PARENT</p:artifactId>\n"
                           + "    <p:version>VERSION</p:version>\n"
                           + "  </p:parent>\n"
                           + "  <p:artifactId>PROJECT</p:artifactId>\n"
                           + "</p:project>";
           String actual = transform( input );
           assertThat( actual ).and( expected ).areIdentical();
    }

}
