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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UrlSourceTest {

    @Test
    public void testUrlSource() {
        try {
            new UrlSource(null);
            fail("Should fail, since you must specify a url");
        } catch (NullPointerException e) {
            assertEquals("url cannot be null", e.getMessage());
        }
    }

    @Test
    public void testGetInputStream() throws Exception {
        URL txtFile = new File("target/test-classes/source.txt").toURI().toURL();
        UrlSource source = new UrlSource(txtFile);
        try (InputStream is = source.getInputStream();
                Scanner scanner = new Scanner(is)) {
            assertEquals("Hello World!", scanner.nextLine());
        }
    }

    @Test
    public void testGetLocation() throws Exception {
        URL txtFile = new File("target/test-classes/source.txt").toURI().toURL();
        UrlSource source = new UrlSource(txtFile);
        assertEquals(txtFile.toExternalForm(), source.getLocation());
    }
}
