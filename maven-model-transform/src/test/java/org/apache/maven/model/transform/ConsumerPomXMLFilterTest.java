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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class ConsumerPomXMLFilterTest extends AbstractXMLFilterTests
{
    @Override
    protected String omitXmlDeclaration()
    {
        return "no";
    }

    @Override
    protected AbstractSAXFilter getFilter( Consumer<LexicalHandler> lexicalHandlerConsumer )
        throws SAXException, ParserConfigurationException, TransformerConfigurationException
    {
        final BuildToRawPomXMLFilterFactory buildPomXMLFilterFactory = new BuildToRawPomXMLFilterFactory( lexicalHandlerConsumer, true )
        {
            @Override
            protected Function<Path, Optional<RelativeProject>> getRelativePathMapper()
            {
                return null;
            }

            @Override
            protected BiFunction<String, String, String> getDependencyKeyToVersionMapper()
            {
                return null;
            }

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

        RawToConsumerPomXMLFilter filter =
            new RawToConsumerPomXMLFilterFactory( buildPomXMLFilterFactory ).get( Paths.get( "pom.xml" ) );
        filter.setFeature( "http://xml.org/sax/features/namespaces", true );
        return filter;
    }

    @Test
    public void aggregatorWithParent()
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
                        + "  </parent>\n"
                        + "  <artifactId>PROJECT</artifactId>\n"
                        + "</project>";
        String actual = transform( input );
        assertThat( actual ).and( expected ).ignoreWhitespace().areIdentical();
    }

    @Test
    public void aggregatorWithCliFriendlyVersion()
        throws Exception
    {
        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "       xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n" +
            "                           http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>org.sonatype.mavenbook.multispring</groupId>\n" +
            "  <artifactId>parent</artifactId>\n" +
            "  <version>0.9-${changelist}-SNAPSHOT</version>\n" +
            "  <packaging>pom</packaging>\n" +
            "  <name>Multi-Spring Chapter Parent Project</name>\n" +
            "  <modules>\n" +
            "    <module>simple-parent</module>\n" +
            "  </modules>\n" +
            "  \n" +
            "  <pluginRepositories>\n" +
            "    <pluginRepository>\n" +
            "      <id>apache.snapshots</id>\n" +
            "      <url>http://repository.apache.org/snapshots/</url>\n" +
            "    </pluginRepository>\n" +
            "  </pluginRepositories>\n" +
            "</project>";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "       xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n" +
            "                           http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>org.sonatype.mavenbook.multispring</groupId>\n" +
            "  <artifactId>parent</artifactId>\n" +
            "  <version>0.9-CL-SNAPSHOT</version>\n" +
            "  <packaging>pom</packaging>\n" +
            "  <name>Multi-Spring Chapter Parent Project</name>\n" +
            "  \n" +
            "  <pluginRepositories>\n" +
            "    <pluginRepository>\n" +
            "      <id>apache.snapshots</id>\n" +
            "      <url>http://repository.apache.org/snapshots/</url>\n" +
            "    </pluginRepository>\n" +
            "  </pluginRepositories>\n" +
            "</project>";
        String actual = transform( input );
        assertThat( actual ).and( expected ).ignoreWhitespace().areIdentical();
    }

    @Test
    public void licenseHeader()
        throws Exception
    {
        String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<!--\n" +
            "Licensed to the Apache Software Foundation (ASF) under one\n" +
            "or more contributor license agreements.  See the NOTICE file\n" +
            "distributed with this work for additional information\n" +
            "regarding copyright ownership.  The ASF licenses this file\n" +
            "to you under the Apache License, Version 2.0 (the\n" +
            "\"License\"); you may not use this file except in compliance\n" +
            "with the License.  You may obtain a copy of the License at\n" +
            "\n" +
            "    http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "Unless required by applicable law or agreed to in writing,\n" +
            "software distributed under the License is distributed on an\n" +
            "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
            "KIND, either express or implied.  See the License for the\n" +
            "specific language governing permissions and limitations\n" +
            "under the License.\n" +
            "-->\n" +
            "\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <parent>\n" +
            "    <groupId>org.apache.maven</groupId>\n" +
            "    <artifactId>maven</artifactId>\n" +
            "    <version>4.0.0-SNAPSHOT</version>\n" +
            "  </parent>\n" +
            "  <artifactId>maven-xml</artifactId>\n" +
            "  <name>Maven XML</name>\n" +
            "  \n" +
            "  <properties>\n" +
            "    <maven.compiler.source>1.8</maven.compiler.source>\n" +
            "    <maven.compiler.target>1.8</maven.compiler.target>\n" +
            "  </properties>\n" +
            "\n" +
            "  <build>\n" +
            "    <plugins>\n" +
            "      <plugin>\n" +
            "        <groupId>org.codehaus.mojo</groupId>\n" +
            "        <artifactId>animal-sniffer-maven-plugin</artifactId>\n" +
            "        <configuration>\n" +
            "          <signature>\n" +
            "            <groupId>org.codehaus.mojo.signature</groupId>\n" +
            "            <artifactId>java18</artifactId>\n" +
            "            <version>1.0</version>\n" +
            "          </signature>\n" +
            "        </configuration>\n" +
            "      </plugin>\n" +
            "    </plugins>\n" +
            "  </build>\n" +
            "  \n" +
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>javax.inject</groupId>\n" +
            "      <artifactId>javax.inject</artifactId>\n" +
            "      <optional>true</optional>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>org.xmlunit</groupId>\n" +
            "      <artifactId>xmlunit-assertj</artifactId>\n" +
            "      <scope>test</scope>\n" +
            "    </dependency>\n" +
            "  </dependencies>\n" +
            "</project>";
        String expected = input;

        String actual = transform( input );
        assertThat( actual ).and( expected ).areIdentical();
    }

    @Test
    public void lexicalHandler()
        throws Exception
    {
        String input = "<project><!--before--><modules>"
                        + "<!--pre-in-->"
                        + "<module><!--in-->ab</module>"
                        + "<module>../cd</module>"
                        + "<!--post-in-->"
                        + "</modules>"
                        + "<!--after--></project>";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<project><!--before--><!--after--></project>";
        String actual = transform( input );
        assertThat( actual ).and( expected ).areIdentical();
    }

}
