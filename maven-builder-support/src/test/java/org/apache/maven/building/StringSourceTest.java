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
package org.apache.maven.building;

import java.io.InputStream;
import java.util.Scanner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringSourceTest {
    @Test
    public void testGetInputStream() throws Exception {
        StringSource source = new StringSource("Hello World!");

        try (InputStream is = source.getInputStream();
                Scanner scanner = new Scanner(is)) {
            assertEquals("Hello World!", scanner.nextLine());
        }
    }

    @Test
    public void testGetLocation() {
        StringSource source = new StringSource("Hello World!");
        assertEquals("(memory)", source.getLocation());

        source = new StringSource("Hello World!", "LOCATION");
        assertEquals("LOCATION", source.getLocation());
    }

    @Test
    public void testGetContent() {
        StringSource source = new StringSource(null);
        assertEquals("", source.getContent());

        source = new StringSource("Hello World!", "LOCATION");
        assertEquals("Hello World!", source.getContent());
    }
}
