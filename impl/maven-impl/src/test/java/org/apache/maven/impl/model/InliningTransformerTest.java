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
package org.apache.maven.impl.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InliningTransformerTest {

    @Test
    void testTransformerInternsCorrectContexts() {
        DefaultModelBuilder.InliningTransformer transformer = new DefaultModelBuilder.InliningTransformer();

        // Test that contexts in the CONTEXTS set are interned
        String groupId1 = transformer.transform("org.apache.maven", "groupId");
        String groupId2 = transformer.transform("org.apache.maven", "groupId");
        assertSame(groupId1, groupId2, "groupId should be interned");

        String type1 = transformer.transform("jar", "type");
        String type2 = transformer.transform("jar", "type");
        assertSame(type1, type2, "type should be interned");

        String scope1 = transformer.transform("compile", "scope");
        String scope2 = transformer.transform("compile", "scope");
        assertSame(scope1, scope2, "scope should be interned");

        String classifier1 = transformer.transform("sources", "classifier");
        String classifier2 = transformer.transform("sources", "classifier");
        assertSame(classifier1, classifier2, "classifier should be interned");

        String goal1 = transformer.transform("compile", "goal");
        String goal2 = transformer.transform("compile", "goal");
        assertSame(goal1, goal2, "goal should be interned");

        String modelVersion1 = transformer.transform("4.0.0", "modelVersion");
        String modelVersion2 = transformer.transform("4.0.0", "modelVersion");
        assertSame(modelVersion1, modelVersion2, "modelVersion should be interned");

        // Test that contexts not in the CONTEXTS set are not interned
        String nonInterned1 = transformer.transform("some-value", "nonInterned");
        String nonInterned2 = transformer.transform("some-value", "nonInterned");
        assertNotSame(nonInterned1, nonInterned2, "non-interned context should not be interned");
        assertEquals(nonInterned1, nonInterned2, "but values should still be equal");
    }

    @Test
    void testTransformerContainsExpectedContexts() {
        // Verify that the CONTEXTS set contains all the expected fields
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("groupId"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("artifactId"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("version"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("packaging"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("scope"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("type"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("classifier"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("goal"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("execution"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("phase"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("modelVersion"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("name"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("url"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("system"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("distribution"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("status"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("connection"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("developerConnection"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("tag"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("id"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("inherited"));
        assertTrue(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("optional"));

        // Verify that non-interned contexts are not in the set
        assertFalse(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("nonInterned"));
        assertFalse(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("description"));
        assertFalse(DefaultModelBuilder.InliningTransformer.CONTEXTS.contains("randomField"));
    }

    @Test
    void testTransformerWithNullAndEmptyValues() {
        DefaultModelBuilder.InliningTransformer transformer = new DefaultModelBuilder.InliningTransformer();

        // Test with null value
        String result1 = transformer.transform(null, "groupId");
        String result2 = transformer.transform(null, "groupId");
        assertSame(result1, result2, "null values should be interned");

        // Test with empty string
        String empty1 = transformer.transform("", "artifactId");
        String empty2 = transformer.transform("", "artifactId");
        assertSame(empty1, empty2, "empty strings should be interned");

        // Test with whitespace
        String whitespace1 = transformer.transform("   ", "version");
        String whitespace2 = transformer.transform("   ", "version");
        assertSame(whitespace1, whitespace2, "whitespace strings should be interned");
    }

    @Test
    void testTransformerIsUsedDuringPomParsing() throws Exception {
        // Create a test transformer that tracks what contexts are called
        List<String> calledContexts = new ArrayList<>();
        XmlReaderRequest.Transformer trackingTransformer = (value, context) -> {
            calledContexts.add(context);
            return value;
        };

        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>

                <dependencies>
                    <dependency>
                        <groupId>org.apache.maven</groupId>
                        <artifactId>maven-core</artifactId>
                        <version>3.9.0</version>
                        <scope>compile</scope>
                        <type>jar</type>
                        <classifier>sources</classifier>
                    </dependency>
                </dependencies>
            </project>
            """;

        DefaultModelXmlFactory factory = new DefaultModelXmlFactory();
        XmlReaderRequest request = XmlReaderRequest.builder()
                .reader(new StringReader(pomXml))
                .transformer(trackingTransformer)
                .build();

        Model model = factory.read(request);

        // Verify the model was parsed correctly
        assertEquals("com.example", model.getGroupId());
        assertEquals("test-project", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("jar", model.getPackaging());

        // Verify that the transformer was called for the expected contexts
        assertTrue(calledContexts.contains("groupId"), "groupId context should be called");
        assertTrue(calledContexts.contains("artifactId"), "artifactId context should be called");
        assertTrue(calledContexts.contains("version"), "version context should be called");
        assertTrue(calledContexts.contains("packaging"), "packaging context should be called");
        assertTrue(calledContexts.contains("scope"), "scope context should be called");
        assertTrue(calledContexts.contains("type"), "type context should be called");
        assertTrue(calledContexts.contains("classifier"), "classifier context should be called");

        // Verify specific paths are called correctly
        long groupIdCount = calledContexts.stream().filter("groupId"::equals).count();
        assertTrue(groupIdCount >= 2, "groupId should be called at least 2 times (project, dependency)");
    }

    @Test
    void testInliningTransformerWithRealPomParsing() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.apache.maven</groupId>
                <artifactId>maven-core</artifactId>
                <version>4.0.0</version>
                <packaging>jar</packaging>

                <dependencies>
                    <dependency>
                        <groupId>org.apache.maven</groupId>
                        <artifactId>maven-api</artifactId>
                        <version>4.0.0</version>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            </project>
            """;

        DefaultModelXmlFactory factory = new DefaultModelXmlFactory();
        DefaultModelBuilder.InliningTransformer transformer = new DefaultModelBuilder.InliningTransformer();

        XmlReaderRequest request = XmlReaderRequest.builder()
                .reader(new StringReader(pomXml))
                .transformer(transformer)
                .build();

        Model model = factory.read(request);

        // Verify the model was parsed correctly
        assertEquals("org.apache.maven", model.getGroupId());
        assertEquals("maven-core", model.getArtifactId());
        assertEquals("4.0.0", model.getVersion());
        assertEquals("jar", model.getPackaging());

        // Verify dependency was parsed
        assertEquals(1, model.getDependencies().size());
        assertEquals("org.apache.maven", model.getDependencies().get(0).getGroupId());
        assertEquals("maven-api", model.getDependencies().get(0).getArtifactId());
        assertEquals("4.0.0", model.getDependencies().get(0).getVersion());
        assertEquals("compile", model.getDependencies().get(0).getScope());
    }
}
