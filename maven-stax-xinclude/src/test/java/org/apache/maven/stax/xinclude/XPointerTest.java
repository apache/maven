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
package org.apache.maven.stax.xinclude;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class XPointerTest {

    @Test
    void testXPointerString() {
        XPointer xpointer;
        // Good Tests.
        String[] goodXPointers = new String[] {
            // Shorthand
            "justaShorthandPointer",
            // element() scheme.
            "element(AnNCName)",
            "element(AnNCName/1)",
            "element(AnNCName/1/2/34)",
            "element(/1)",
            "element(/1/4/43)"
        };

        // Test XPointers
        for (String goodXPointer : goodXPointers) {
            try {
                xpointer = new XPointer(goodXPointer);
                String result = xpointer.toString();
                assertEquals(
                        goodXPointer,
                        result,
                        "The serialisation of XPointer: " + goodXPointer + ", produced a different result: " + result);
            } catch (InvalidXPointerException e) {
                fail("XPointer: " + goodXPointer + ", is reported as being invalid when it actually is valid.");
            } catch (Exception e) {
                fail("Failed with unexpected exception: " + e);
            }
        }

        // Bad Tests.
        String[] badXPointers = new String[] {
            // Shorthand
            "justaShorthand##Pointer",
            // element() scheme.
            "",
            "element(/)",
            "element(//)",
            "element(/1/2/3//)",
            "element(/1/b/3)",
            "element(Not!AnNCNa-me)",
            "element(/ncname)",
            "element(AnNCName/)",
            "element(AnNCName//)",
            "element(AnNCName/1/b)",
            "element(AnNCName/1/2//)"
        };

        // Test XPointers
        for (String badXPointer : badXPointers) {
            try {
                xpointer = new XPointer(badXPointer);
                fail("XPointer parser failed to thrown an exception for invalid XPointer: " + badXPointer);
            } catch (Exception e) { // See if exception is anything other than InvalidXPointerException which we want.
                if (!(e instanceof InvalidXPointerException)) {
                    fail("Parsing the XPointer threw an unexpected exception: " + e + ", On XPointer: " + badXPointer);
                }
            }
        }
    }
}
