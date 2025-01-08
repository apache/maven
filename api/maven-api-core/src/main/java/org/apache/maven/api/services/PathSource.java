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
package org.apache.maven.api.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

class PathSource implements ModelSource {

    private final Path path;
    private final String location;

    PathSource(Path path) {
        this(path, null);
    }

    PathSource(Path path, String location) {
        this.path = path.normalize();
        this.location = location != null ? location : this.path.toString();
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public Source resolve(String relative) {
        return new PathSource(path.resolve(relative));
    }

    @Override
    public ModelSource resolve(ModelLocator locator, String relative) {
        String norm = relative.replace('\\', File.separatorChar).replace('/', File.separatorChar);
        Path path = getPath().getParent().resolve(norm);
        Path relatedPom = locator.locateExistingPom(path);
        if (relatedPom != null) {
            return new PathSource(relatedPom);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        return this == o || o.getClass() == getClass() && Objects.equals(path, ((PathSource) o).path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return "PathSource[" + "location='" + location + '\'' + ", " + "path=" + path + ']';
    }
}
