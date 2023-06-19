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
package org.apache.maven.model.transform;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelVersionXMLFilterTest extends AbstractXMLFilterTests {
    @Override
    protected XmlPullParser getFilter(XmlPullParser parser) {
        return new ModelVersionXMLFilter(parser);
    }

    @Test
    void modelVersionWithDefaultPrefix() throws Exception {
        String input = "<project xmlns='http://maven.apache.org/POM/4.0.0'>"
                + "  <groupId>GROUPID</groupId>"
                + "  <artifactId>ARTIFACTID</artifactId>"
                + "  <version>VERSION</version>"
                + "</project>";
        String expected = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">"
                + "  <modelVersion>4.0.0</modelVersion>"
                + "  <groupId>GROUPID</groupId>"
                + "  <artifactId>ARTIFACTID</artifactId>"
                + "  <version>VERSION</version>"
                + "</project>";

        // Check that the modelVersion is added
        assertEquals(expected, transform(input));
        // Check that the transformed POM is stable (modelVersion not added twice)
        assertEquals(expected, transform(expected));
    }

    @Test
    void modelVersionWithPrefix() throws Exception {
        String input = "<maven:project xmlns:maven='http://maven.apache.org/POM/4.0.0'>"
                + "  <maven:groupId>GROUPID</maven:groupId>"
                + "  <maven:artifactId>ARTIFACTID</maven:artifactId>"
                + "  <maven:version>VERSION</maven:version>"
                + "</maven:project>";
        String expected = "<maven:project xmlns:maven=\"http://maven.apache.org/POM/4.0.0\">"
                + "  <maven:modelVersion>4.0.0</maven:modelVersion>"
                + "  <maven:groupId>GROUPID</maven:groupId>"
                + "  <maven:artifactId>ARTIFACTID</maven:artifactId>"
                + "  <maven:version>VERSION</maven:version>"
                + "</maven:project>";

        // Check that the modelVersion is added
        assertEquals(expected, transform(input));
        // Check that the transformed POM is stable (modelVersion not added twice)
        assertEquals(expected, transform(expected));
    }
}
