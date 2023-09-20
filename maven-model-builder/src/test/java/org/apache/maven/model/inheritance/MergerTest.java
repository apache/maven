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
package org.apache.maven.model.inheritance;

import java.io.InputStream;
import java.io.StringReader;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenMerger;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MergerTest {

    @Test
    void testMergerPreserveLocations() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/poms/factory/complex.xml")) {

            InputSource inputSource = new InputSource(null, "classpath:/poms/factory/complex.xml");
            Model model = new MavenStaxReader().read(is, true, inputSource);
            InputLocation propertiesLocation = model.getLocation("properties");
            assertNotNull(propertiesLocation);
            assertEquals(13, propertiesLocation.getLineNumber());
            assertEquals(5, propertiesLocation.getColumnNumber());
            InputLocation filterPropLocation = propertiesLocation.getLocation("my.filter.value");
            assertNotNull(filterPropLocation);
            assertEquals(14, filterPropLocation.getLineNumber());
            assertEquals(31, filterPropLocation.getColumnNumber());

            Model model2 = Model.newBuilder(model).build();
            propertiesLocation = model2.getLocation("properties");
            assertNotNull(propertiesLocation);
            assertEquals(13, propertiesLocation.getLineNumber());
            assertEquals(5, propertiesLocation.getColumnNumber());
            filterPropLocation = propertiesLocation.getLocation("my.filter.value");
            assertNotNull(filterPropLocation);
            assertEquals(14, filterPropLocation.getLineNumber());
            assertEquals(31, filterPropLocation.getColumnNumber());
            assertNotNull(model.getLocation("groupId"));

            Model mergeInput = new MavenStaxReader()
                    .read(
                            new StringReader("<project>\n"
                                    + "  <properties>\n"
                                    + "    <my.prop>my-value</my.prop>\n"
                                    + "  </properties>\n"
                                    + "</project>"),
                            true,
                            new InputSource(null, "merge-input"));
            propertiesLocation = mergeInput.getLocation("properties");
            assertNotNull(propertiesLocation);
            assertEquals(2, propertiesLocation.getLineNumber());
            assertEquals(3, propertiesLocation.getColumnNumber());
            filterPropLocation = propertiesLocation.getLocation("my.prop");
            assertNotNull(filterPropLocation);
            assertEquals(3, filterPropLocation.getLineNumber());
            assertEquals(22, filterPropLocation.getColumnNumber());

            Model result = new MavenMerger().merge(model, mergeInput, true, null);
            propertiesLocation = result.getLocation("properties");
            assertNotNull(propertiesLocation);
            assertEquals(-1, propertiesLocation.getLineNumber());
            assertEquals(-1, propertiesLocation.getColumnNumber());
            filterPropLocation = propertiesLocation.getLocation("my.filter.value");
            assertNotNull(filterPropLocation);
            assertEquals(14, filterPropLocation.getLineNumber());
            assertEquals(31, filterPropLocation.getColumnNumber());
            filterPropLocation = propertiesLocation.getLocation("my.prop");
            assertNotNull(filterPropLocation);
            assertEquals(3, filterPropLocation.getLineNumber());
            assertEquals(22, filterPropLocation.getColumnNumber());
            assertNotNull(result.getLocation("groupId"));

            result = new DefaultInheritanceAssembler.InheritanceModelMerger().merge(model, mergeInput, true, null);
            propertiesLocation = result.getLocation("properties");
            assertNotNull(propertiesLocation);
            assertEquals(-1, propertiesLocation.getLineNumber());
            assertEquals(-1, propertiesLocation.getColumnNumber());
            filterPropLocation = propertiesLocation.getLocation("my.filter.value");
            assertNotNull(filterPropLocation);
            assertEquals(14, filterPropLocation.getLineNumber());
            assertEquals(31, filterPropLocation.getColumnNumber());
            filterPropLocation = propertiesLocation.getLocation("my.prop");
            assertNotNull(filterPropLocation);
            assertEquals(3, filterPropLocation.getLineNumber());
            assertEquals(22, filterPropLocation.getColumnNumber());
            assertNotNull(result.getLocation("groupId"));
        }
    }
}
