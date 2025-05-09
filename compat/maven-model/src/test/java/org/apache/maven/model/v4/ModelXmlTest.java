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
package org.apache.maven.model.v4;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.xml.XmlNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelXmlTest {

    @Test
    void xmlRoundtripWithProperties() throws Exception {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("javax.version", "3.1.0");
        props.put("mockito.version", "1.10.19");
        props.put("hamcret.version", "2.1");
        props.put("lombok.version", "1.18.6");
        props.put("junit.version", "4.12");
        Model model = Model.newBuilder(true).properties(props).build();
        String xml = toXml(model);

        for (int i = 0; i < 10; i++) {
            String newStr = toXml(fromXml(xml));
            assertThat(xml).isEqualTo(newStr);
        }
    }

    @Test
    void namespaceInXmlNode() throws XMLStreamException {
        String xml = "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "         <m:configuration xmlns:m=\"http://maven.apache.org/POM/4.0.0\" xmlns=\"http://fabric8.io/fabric8-maven-plugin\">\n"
                + "             <myConfig>foo</myConfig>\n"
                + "         </m:configuration>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        Plugin plugin = model.getBuild().getPlugins().get(0);
        XmlNode node = plugin.getConfiguration();
        assertThat(node).isNotNull();
        assertThat(node.namespaceUri()).isEqualTo("http://maven.apache.org/POM/4.0.0");
        assertThat(node.prefix()).isEqualTo("m");
        assertThat(node.name()).isEqualTo("configuration");
        assertThat(node.children().size()).isEqualTo(1);
        XmlNode myConfig = node.children().get(0);
        assertThat(myConfig.namespaceUri()).isEqualTo("http://fabric8.io/fabric8-maven-plugin");
        assertThat(myConfig.prefix()).isEqualTo("");
        assertThat(myConfig.name()).isEqualTo("myConfig");
        String config = node.toString();
        assertThat(config.isEmpty()).isFalse();
    }

    String toXml(Model model) throws IOException, XMLStreamException {
        StringWriter sw = new StringWriter();
        MavenStaxWriter writer = new MavenStaxWriter();
        writer.setAddLocationInformation(false);
        writer.write(sw, model);
        return sw.toString();
    }

    Model fromXml(String xml) throws XMLStreamException {
        return new MavenStaxReader().read(new StringReader(xml));
    }
}
