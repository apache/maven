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

import java.util.Optional;

import javax.inject.Provider;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;

public class ConsumerPomXMLFilterTest extends AbstractXMLFilterTests
{
    @Override
    protected XMLFilter getFilter() throws SAXException, ParserConfigurationException
    {
        final BuildPomXMLFilterFactory buildPomXMLFilterFactory = new BuildPomXMLFilterFactory()
        {
            @Override
            protected Optional<String> getSha1()
            {
                return Optional.empty();
            }
            
            @Override
            protected Optional<String> getRevision()
            {
                return Optional.empty();
            }
            
            @Override
            protected Optional<String> getChangelist()
            {
                return Optional.of( "CL" );
            }
        };
        
        Provider<BuildPomXMLFilterFactory> provider = new Provider<BuildPomXMLFilterFactory>()
        {

            @Override
            public BuildPomXMLFilterFactory get()
            {
                return buildPomXMLFilterFactory;
            }
        };
        
        XMLFilter filter = new ConsumerPomXMLFilterFactory( provider )
        {
        }.get( "G", "A" );
        filter.setFeature( "http://xml.org/sax/features/namespaces", true );
        return filter;
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
        String actual = transform( input );
        assertThat( actual ).and( expected ).ignoreWhitespace().areIdentical();
    }
    
    @Test
    public void testMe() throws Exception {
        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" \r\n" + 
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 \r\n" + 
            "                             http://maven.apache.org/maven-v4_0_0.xsd\">\r\n" + 
            "  <modelVersion>4.0.0</modelVersion>\r\n" + 
            "  <groupId>org.sonatype.mavenbook.multispring</groupId>\r\n" + 
            "  <artifactId>parent</artifactId>\r\n" + 
            "  <version>0.9-${changelist}-SNAPSHOT</version>\r\n" + 
            "  <packaging>pom</packaging>\r\n" + 
            "  <name>Multi-Spring Chapter Parent Project</name>\r\n" + 
            "  <modules>\r\n" + 
            "    <module>simple-parent</module>\r\n" + 
            "  </modules>\r\n" + 
            "  \r\n" + 
            "  <pluginRepositories>\r\n" + 
            "    <pluginRepository>\r\n" + 
            "      <id>apache.snapshots</id>\r\n" + 
            "      <url>http://repository.apache.org/snapshots/</url>\r\n" + 
            "    </pluginRepository>\r\n" + 
            "  </pluginRepositories>\r\n" + 
            "</project>";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" \r\n" + 
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 \r\n" + 
            "                             http://maven.apache.org/maven-v4_0_0.xsd\">\r\n" + 
            "  <modelVersion>4.0.0</modelVersion>\r\n" + 
            "  <groupId>org.sonatype.mavenbook.multispring</groupId>\r\n" + 
            "  <artifactId>parent</artifactId>\r\n" + 
            "  <version>0.9-CL-SNAPSHOT</version>\r\n" + 
            "  <packaging>pom</packaging>\r\n" + 
            "  <name>Multi-Spring Chapter Parent Project</name>\r\n" + 
            "  \r\n" + 
            "  <pluginRepositories>\r\n" + 
            "    <pluginRepository>\r\n" + 
            "      <id>apache.snapshots</id>\r\n" + 
            "      <url>http://repository.apache.org/snapshots/</url>\r\n" + 
            "    </pluginRepository>\r\n" + 
            "  </pluginRepositories>\r\n" + 
            "</project>";
        String actual = transform( input );
        assertThat( actual ).and( expected ).ignoreWhitespace().areIdentical();
    }
    
    

}
