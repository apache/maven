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
package org.apache.maven.artifact.repository.metadata.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Handles deserialization of metadata from some kind of textual format like XML.
 *
 * @author Benjamin Bentmann
 */
@Component(role = MetadataReader.class)
public class DefaultMetadataReader implements MetadataReader {

    public Metadata read(File input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        Metadata metadata = read(ReaderFactory.newXmlReader(input), options);

        return metadata;
    }

    public Metadata read(Reader input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (Reader in = input) {
            Metadata metadata = new MetadataXpp3Reader().read(in, isStrict(options));
            validateMetadata(metadata);
            return metadata;
        } catch (XmlPullParserException e) {
            throw new MetadataParseException(e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e);
        }
    }

    public Metadata read(InputStream input, Map<String, ?> options) throws IOException {
        Objects.requireNonNull(input, "input cannot be null");

        try (InputStream in = input) {
            Metadata metadata = new MetadataXpp3Reader().read(in, isStrict(options));
            validateMetadata(metadata);
            return metadata;
        } catch (XmlPullParserException e) {
            throw new MetadataParseException(e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e);
        }
    }

    private boolean isStrict(Map<String, ?> options) {
        Object value = (options != null) ? options.get(IS_STRICT) : null;
        return value == null || Boolean.parseBoolean(value.toString());
    }

    /**
     * Coordinate-shaped tokens read from this metadata (versions, plugin artifactIds and prefixes) get carried
     * forward by callers as if they were already-validated path and coordinate components. Reject anything that
     * would not itself be a valid coordinate component here, before it leaves this reader.
     */
    private static void validateMetadata(Metadata metadata) throws IOException {
        if (metadata == null) {
            return;
        }

        Versioning versioning = metadata.getVersioning();
        if (versioning != null) {
            validateToken("version", versioning.getRelease());
            validateToken("version", versioning.getLatest());
            for (String version : versioning.getVersions()) {
                validateToken("version", version);
            }
            for (SnapshotVersion snapshotVersion : versioning.getSnapshotVersions()) {
                validateToken("version", snapshotVersion.getVersion());
            }
            Snapshot snapshot = versioning.getSnapshot();
            if (snapshot != null) {
                validateToken("snapshot timestamp", snapshot.getTimestamp());
            }
        }

        if (metadata.getPlugins() != null) {
            for (Plugin plugin : metadata.getPlugins()) {
                validateToken("plugin artifactId", plugin.getArtifactId());
                validateToken("plugin prefix", plugin.getPrefix());
            }
        }
    }

    private static void validateToken(String field, String value) throws IOException {
        if (value == null || value.isEmpty()) {
            return;
        }
        boolean valid = !"..".equals(value);
        if (valid) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '/' || c == '\\' || c == ':' || Character.isISOControl(c)) {
                    valid = false;
                    break;
                }
            }
        }
        if (!valid) {
            throw new IOException("Metadata contains an invalid " + field + ": '" + value + "'");
        }
    }
}
