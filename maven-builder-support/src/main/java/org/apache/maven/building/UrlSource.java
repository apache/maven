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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * Wraps an ordinary {@link URL} as a source.
 *
 */
public class UrlSource implements Source {

    private final URL url;

    private final int hashCode;

    /**
     * Creates a new source backed by the specified URL.
     *
     * @param url The file, must not be {@code null}.
     */
    public UrlSource(URL url) {
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.hashCode = Objects.hashCode(url);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    @Override
    public String getLocation() {
        return url.toString();
    }

    /**
     * Gets the URL of this source.
     *
     * @return The underlying URL, never {@code null}.
     */
    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return getLocation();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!UrlSource.class.equals(obj.getClass())) {
            return false;
        }

        UrlSource other = (UrlSource) obj;
        return Objects.equals(url.toExternalForm(), other.url.toExternalForm());
    }
}
