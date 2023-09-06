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
package org.apache.maven.model.io;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalXmlResolver implements XMLResolver {

    private final Path rootDirectory;

    public LocalXmlResolver(Path rootDirectory) {
        this.rootDirectory = rootDirectory != null ? rootDirectory.normalize() : null;
    }

    @Override
    public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
            throws XMLStreamException {
        if (rootDirectory == null) {
            return null;
        }
        if (systemID == null) {
            throw new XMLStreamException("systemID is null");
        }
        if (baseURI == null) {
            throw new XMLStreamException("baseURI is null");
        }
        URI baseUri;
        try {
            baseUri = new URI(baseURI).normalize();
        } catch (URISyntaxException e) {
            throw new XMLStreamException("Invalid syntax for baseURI URI: " + baseURI, e);
        }
        URI sysUri;
        try {
            sysUri = new URI(systemID).normalize();
        } catch (URISyntaxException e) {
            throw new XMLStreamException("Invalid syntax for systemID URI: " + systemID, e);
        }
        if (sysUri.getScheme() != null) {
            throw new XMLStreamException("systemID must be a relative URI: " + systemID);
        }
        Path base = Paths.get(baseUri).normalize();
        if (!base.startsWith(rootDirectory)) {
            return null;
        }
        Path sys = Paths.get(sysUri.getSchemeSpecificPart()).normalize();
        if (sys.isAbsolute()) {
            throw new XMLStreamException("systemID must be a relative path: " + systemID);
        }
        Path res = base.resolveSibling(sys).normalize();
        if (!res.startsWith(rootDirectory)) {
            throw new XMLStreamException("systemID cannot refer to outside rootDirectory: " + systemID);
        }
        try {
            return new StreamSource(Files.newInputStream(res), res.toUri().toASCIIString());
        } catch (IOException e) {
            throw new XMLStreamException("Unable to create Source for " + systemID + ": " + e.getMessage(), e);
        }
    }
}
