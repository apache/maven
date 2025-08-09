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
package org.codehaus.plexus.classworlds.launcher;

import org.codehaus.plexus.classworlds.AbstractClassWorldsTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigurationParserTest extends AbstractClassWorldsTestCase {

    ConfigurationParser configurator = new ConfigurationParser(null, System.getProperties());

    @Test
    void testFilterUnterminated() {
        try {
            this.configurator.filter("${cheese");
            fail("throw ConfigurationException");
        } catch (ConfigurationException e) {
            // expected and correct
            assertTrue(e.getMessage().startsWith("Unterminated"));
        }
    }

    @Test
    void testFilterSolitary() throws Exception {
        System.setProperty("classworlds.test.prop", "test prop value");

        String result = this.configurator.filter("${classworlds.test.prop}");

        assertEquals("test prop value", result);
    }

    @Test
    void testFilterAtStart() throws Exception {
        System.setProperty("classworlds.test.prop", "test prop value");

        String result = this.configurator.filter("${classworlds.test.prop}cheese");

        assertEquals("test prop valuecheese", result);
    }

    @Test
    void testFilterAtEnd() throws Exception {
        System.setProperty("classworlds.test.prop", "test prop value");

        String result = this.configurator.filter("cheese${classworlds.test.prop}");

        assertEquals("cheesetest prop value", result);
    }

    @Test
    void testFilterMultiple() throws Exception {
        System.setProperty("classworlds.test.prop.one", "test prop value one");

        System.setProperty("classworlds.test.prop.two", "test prop value two");

        String result =
                this.configurator.filter("I like ${classworlds.test.prop.one} and ${classworlds.test.prop.two} a lot");

        assertEquals("I like test prop value one and test prop value two a lot", result);
    }

    @Test
    void testFilterNonExistent() {
        try {
            this.configurator.filter("${gollygeewillikers}");
            fail("throw ConfigurationException");
        } catch (ConfigurationException e) {
            // expected and correct
            assertTrue(e.getMessage().startsWith("No such property"));
        }
    }

    @Test
    void testFilterInMiddle() throws Exception {
        System.setProperty("classworlds.test.prop", "test prop value");

        String result = this.configurator.filter("cheese${classworlds.test.prop}toast");

        assertEquals("cheesetest prop valuetoast", result);
    }
}
