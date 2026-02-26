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
package org.apache.maven.internal.xml;

import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.api.xml.XmlService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlServiceLoadingTest {

    @Test
    void testServiceLoaderFallbackWhenContextClassLoaderCannotSeeProvider() throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader contextClassLoader = new URLClassLoader(new URL[0], null)) {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            XmlNode node = XmlService.read(new StringReader("<configuration/>"));
            assertEquals("configuration", node.name());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
