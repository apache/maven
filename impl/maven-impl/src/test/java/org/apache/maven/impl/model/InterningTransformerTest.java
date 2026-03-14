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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link DefaultModelBuilder.InterningTransformer}.
 * Verifies that the transformer correctly interns commonly used string values
 * to reduce memory usage during Maven POM parsing.
 */
class InterningTransformerTest {

    @Test
    void testTransformerInternsCorrectContexts() {
        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer();

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
        // Use new String() to avoid automatic interning by JVM
        String value1 = new String("some-value");
        String value2 = new String("some-value");
        String nonInterned1 = transformer.transform(value1, "nonInterned");
        String nonInterned2 = transformer.transform(value2, "nonInterned");
        assertSame(value1, nonInterned1, "non-interned context should return same instance");
        assertSame(value2, nonInterned2, "non-interned context should return same instance");
        assertNotSame(nonInterned1, nonInterned2, "different input instances should remain different");
        assertEquals(nonInterned1, nonInterned2, "but values should still be equal");
    }

    @Test
    void testTransformerContainsExpectedContexts() {
        // Verify that the DEFAULT_CONTEXTS set contains all the expected fields
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("groupId"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "groupId");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("artifactId"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain "
                        + "artifactId");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("version"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "version");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("packaging"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "packaging");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("scope"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "scope");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("type"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "type");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("classifier"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain "
                        + "classifier");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("goal"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "goal");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("execution"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "execution");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("phase"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "phase");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("modelVersion"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain "
                        + "modelVersion");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("name"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "name");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("url"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "url");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("system"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "system");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("distribution"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain "
                        + "distribution");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("status"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "status");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("connection"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain "
                        + "connection");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("developerConnection"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain "
                        + "developerConnection");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("tag"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "tag");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("id"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "id");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("inherited"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "inherited");
        assertTrue(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("optional"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to contain " + "optional");

        // Verify that non-interned contexts are not in the set
        assertFalse(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("nonInterned"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to not contain "
                        + "nonInterned");
        assertFalse(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("description"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to not contain "
                        + "description");
        assertFalse(
                DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS.contains("randomField"),
                "Expected " + DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS + " to not contain "
                        + "randomField");
    }

    @Test
    void testTransformerWithNullAndEmptyValues() {
        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer();

        // Test with null value
        String result1 = transformer.transform(null, "groupId");
        String result2 = transformer.transform(null, "groupId");
        assertNull(result1);
        assertNull(result2);

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

        String pomXml = """
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
    void testInterningTransformerWithRealPomParsing() throws Exception {
        String pomXml = """
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
        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer();

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

    @Test
    void testTransformerWithSessionPropertyUserProperties() {
        // Test with custom contexts from user properties
        Map<String, String> userProperties = new HashMap<>();
        userProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "groupId,artifactId,customField");

        Session session = Mockito.mock(Session.class);
        when(session.getUserProperties()).thenReturn(userProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Test that custom contexts are used
        assertTrue(transformer.getContexts().contains("groupId"));
        assertTrue(transformer.getContexts().contains("artifactId"));
        assertTrue(transformer.getContexts().contains("customField"));

        // Test that default contexts not in the custom list are not used
        assertFalse(transformer.getContexts().contains("version"));
        assertFalse(transformer.getContexts().contains("scope"));

        // Test interning behavior
        String groupId1 = transformer.transform("org.apache.maven", "groupId");
        String groupId2 = transformer.transform("org.apache.maven", "groupId");
        assertSame(groupId1, groupId2, "groupId should be interned");

        String custom1 = transformer.transform("test-value", "customField");
        String custom2 = transformer.transform("test-value", "customField");
        assertSame(custom1, custom2, "customField should be interned");

        // Test that non-custom contexts are not interned
        String version1 = new String("1.0.0");
        String version2 = new String("1.0.0");
        String nonInterned1 = transformer.transform(version1, "version");
        String nonInterned2 = transformer.transform(version2, "version");
        assertSame(version1, nonInterned1, "version should not be interned");
        assertSame(version2, nonInterned2, "version should not be interned");
        assertNotSame(nonInterned1, nonInterned2, "different input instances should remain different");
    }

    @Test
    void testTransformerWithSessionPropertySystemProperties() {
        // Test with custom contexts from system properties
        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "scope,type");

        Session session = Mockito.mock(Session.class);
        when(session.getSystemProperties()).thenReturn(systemProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Test that custom contexts are used
        assertTrue(transformer.getContexts().contains("scope"));
        assertTrue(transformer.getContexts().contains("type"));
        assertEquals(2, transformer.getContexts().size());

        // Test interning behavior
        String scope1 = transformer.transform("compile", "scope");
        String scope2 = transformer.transform("compile", "scope");
        assertSame(scope1, scope2, "scope should be interned");
    }

    @Test
    void testTransformerUserPropertiesOverrideSystemProperties() {
        // Test that user properties take precedence over system properties
        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "scope,type");

        Map<String, String> userProperties = new HashMap<>();
        userProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "groupId,artifactId");

        Session session = Mockito.mock(Session.class);
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getSystemProperties()).thenReturn(systemProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Test that user properties are used, not system properties
        assertTrue(transformer.getContexts().contains("groupId"));
        assertTrue(transformer.getContexts().contains("artifactId"));
        assertFalse(transformer.getContexts().contains("scope"));
        assertFalse(transformer.getContexts().contains("type"));
        assertEquals(2, transformer.getContexts().size());
    }

    @Test
    void testTransformerWithEmptySessionProperty() {
        // Test with empty property value - should use defaults
        Map<String, String> userProperties = new HashMap<>();
        userProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "");

        Session session = Mockito.mock(Session.class);
        when(session.getUserProperties()).thenReturn(userProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Should use default contexts
        assertEquals(DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS, transformer.getContexts());
    }

    @Test
    void testTransformerWithWhitespaceOnlySessionProperty() {
        // Test with whitespace-only property value - should use defaults
        Map<String, String> userProperties = new HashMap<>();
        userProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "   ");

        Session session = Mockito.mock(Session.class);
        when(session.getUserProperties()).thenReturn(userProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Should use default contexts
        assertEquals(DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS, transformer.getContexts());
    }

    @Test
    void testTransformerWithNoSessionProperty() {
        // Test with no property set - should use defaults
        Session session = Mockito.mock(Session.class);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Should use default contexts
        assertEquals(DefaultModelBuilder.InterningTransformer.DEFAULT_CONTEXTS, transformer.getContexts());
    }

    @Test
    void testTransformerWithCommaSeparatedValues() {
        // Test parsing of comma-separated values with various whitespace
        Map<String, String> userProperties = new HashMap<>();
        userProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "groupId, artifactId , version,  scope  ,type");

        Session session = Mockito.mock(Session.class);
        when(session.getUserProperties()).thenReturn(userProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Test that all values are parsed correctly (whitespace trimmed)
        assertTrue(transformer.getContexts().contains("groupId"));
        assertTrue(transformer.getContexts().contains("artifactId"));
        assertTrue(transformer.getContexts().contains("version"));
        assertTrue(transformer.getContexts().contains("scope"));
        assertTrue(transformer.getContexts().contains("type"));
        assertEquals(5, transformer.getContexts().size());
    }

    @Test
    void testTransformerWithEmptyCommaSeparatedValues() {
        // Test parsing with empty values in comma-separated list
        Map<String, String> userProperties = new HashMap<>();
        userProperties.put(Constants.MAVEN_MODEL_BUILDER_INTERNS, "groupId,,artifactId, ,version");

        Session session = Mockito.mock(Session.class);
        when(session.getUserProperties()).thenReturn(userProperties);

        DefaultModelBuilder.InterningTransformer transformer = new DefaultModelBuilder.InterningTransformer(session);

        // Test that empty values are filtered out
        assertTrue(transformer.getContexts().contains("groupId"));
        assertTrue(transformer.getContexts().contains("artifactId"));
        assertTrue(transformer.getContexts().contains("version"));
        assertEquals(3, transformer.getContexts().size());
    }
}
